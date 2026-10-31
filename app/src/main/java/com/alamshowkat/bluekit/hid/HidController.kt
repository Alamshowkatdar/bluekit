package com.alamshowkat.bluekit.hid

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** What the HID profile is doing right now. */
sealed interface HidState {
    /** The phone has no Bluetooth adapter at all. */
    data object Unsupported : HidState

    /** Bluetooth is switched off, or the app is not allowed to use it. */
    data object BluetoothOff : HidState

    /** Asking Android for the HID device profile. */
    data object Starting : HidState

    /** Profile acquired; waiting for Android to confirm registration. */
    data object Registering : HidState

    /** Registered and waiting for a host to connect. */
    data object Ready : HidState

    /** A host is connected and reports can be sent. */
    data class Connected(val deviceName: String, val address: String) : HidState

    data class Failed(val message: String) : HidState

    val isConnected: Boolean get() = this is Connected
}

/**
 * Owns the Android Bluetooth HID device profile and turns small, mode-specific
 * calls into HID reports.
 *
 * Only one application can hold the HID device profile at a time, so this class
 * is deliberately a single owner: the ViewModel creates exactly one instance
 * and closes it when the app goes away.
 */
class HidController(context: Context) {

    private companion object {
        const val TAG = "BlueKit"

        /**
         * Some Bluetooth stacks (notably after a Bluetooth restart) return true
         * from registerApp but never deliver onAppStatusChanged. Retrying makes
         * the stack disable and re-enable the HID service, which clears it.
         */
        const val REGISTRATION_TIMEOUT_MS = 2_500L
        const val REGISTER_RETRY_MS = 1_500L
        const val MAX_REGISTER_ATTEMPTS = 3

        /** How often to verify the link while the UI claims it is connected. */
        const val CONNECTION_POLL_MS = 2_000L

        /** Consecutive rejected reports that mean the host is really gone. */
        const val MAX_CONSECUTIVE_REJECTS = 5

        /** ...but only if they keep failing for at least this long. */
        const val REJECT_WINDOW_MS = 1_000L
    }

    private val appContext = context.applicationContext
    private val mainExecutor = ContextCompat.getMainExecutor(appContext)

    private val adapter: BluetoothAdapter? =
        (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private val _state = MutableStateFlow<HidState>(HidState.Starting)
    val state: StateFlow<HidState> = _state.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val mainHandler = Handler(Looper.getMainLooper())

    private val closed = AtomicBoolean(false)
    private val lastReports = ConcurrentHashMap<Int, ByteArray>()
    private val lastSlowLogAt = AtomicLong(0L)
    private val lastRejectedLogAt = AtomicLong(0L)
    private val consecutiveRejects = AtomicInteger(0)
    private val firstRejectAt = AtomicLong(0L)

    private var registerAttempts = 0
    private var staleConnectionPolls = 0

    private val registrationWatchdog = Runnable {
        if (closed.get() || registered) return@Runnable
        if (_state.value != HidState.Registering) return@Runnable
        val hid = hidDevice ?: return@Runnable
        if (registerAttempts >= MAX_REGISTER_ATTEMPTS) {
            _state.value = HidState.Failed(
                "Android did not finish registering the HID profile. " +
                    "Restart Bluetooth (or the phone), then tap Retry."
            )
            log("Android never confirmed HID registration.")
            Log.w(TAG, "HID registration timed out after $MAX_REGISTER_ATTEMPTS attempts")
            return@Runnable
        }
        registerAttempts++
        log(
            "Android has not confirmed HID registration yet; " +
                "retrying (${registerAttempts}/$MAX_REGISTER_ATTEMPTS)."
        )
        Log.w(TAG, "HID registration timeout, retry $registerAttempts")
        ignoreNextUnregister = true
        unregisterQuietly(hid)
        register(hid)
    }

    /**
     * Used when registerApp itself is refused, usually because the previous
     * instance of this app is still being cleaned up. Retrying the call (without
     * unregistering, which could disturb another HID app) recovers from that.
     */
    private val registerRetry = Runnable {
        if (closed.get() || registered) return@Runnable
        if (_state.value != HidState.Registering) return@Runnable
        val hid = hidDevice ?: return@Runnable
        log(
            "Android refused the HID registration; " +
                "retrying (${registerAttempts + 1}/$MAX_REGISTER_ATTEMPTS)."
        )
        registerAttempts++
        register(hid)
    }

    /**
     * Android can keep reporting the HID link as connected after the host turns
     * its Bluetooth off, which leaves the app sending reports into a dead link.
     * Two failed polls in a row are treated as a real disconnect.
     */
    private val connectionWatchdog = object : Runnable {
        override fun run() {
            if (closed.get()) return
            val device = connectedDevice ?: return
            val hid = hidDevice ?: return
            val connectionState = try {
                hid.getConnectionState(device)
            } catch (_: SecurityException) {
                BluetoothProfile.STATE_DISCONNECTED
            }
            if (connectionState == BluetoothProfile.STATE_CONNECTED) {
                staleConnectionPolls = 0
                mainHandler.postDelayed(this, CONNECTION_POLL_MS)
                return
            }
            staleConnectionPolls++
            if (staleConnectionPolls >= 2) {
                Log.i(TAG, "Connection watchdog: link is gone (state=$connectionState)")
                handleLinkLost(device)
            } else {
                mainHandler.postDelayed(this, CONNECTION_POLL_MS)
            }
        }
    }

    private fun startConnectionWatchdog() {
        mainHandler.removeCallbacks(connectionWatchdog)
        staleConnectionPolls = 0
        mainHandler.postDelayed(connectionWatchdog, CONNECTION_POLL_MS)
    }

    /** Runs on the main thread. */
    @SuppressLint("MissingPermission")
    private fun handleLinkLost(device: BluetoothDevice) {
        if (connectedDevice?.address != device.address) return
        connectedDevice = null
        consecutiveRejects.set(0)
        staleConnectionPolls = 0
        mainHandler.removeCallbacks(connectionWatchdog)
        // The next host connection starts from a neutral report, not from
        // whatever key or button happened to be down when the link died.
        lastReports.clear()
        pump.reset()
        _state.value = if (registered) {
            HidState.Ready
        } else {
            HidState.Failed("The HID profile is not registered. Tap Retry.")
        }
        log("Host disconnected: ${deviceLabel(device)}")
    }

    private val pump = ReportPump(::deliver)

    @Volatile
    private var hidDevice: BluetoothHidDevice? = null
    private var proxyRequested = false
    private var registered = false
    private var ignoreNextUnregister = false

    @Volatile
    private var connectedDevice: BluetoothDevice? = null

    // ------------------------------------------------------------- lifecycle

    /** True when the app may call the Bluetooth APIs on this Android version. */
    fun hasBluetoothPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasAdvertisePermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.BLUETOOTH_ADVERTISE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isBluetoothEnabled(): Boolean = try {
        adapter?.isEnabled == true
    } catch (_: SecurityException) {
        false
    }

    fun adapterName(): String? = try {
        adapter?.name
    } catch (_: SecurityException) {
        null
    }

    @SuppressLint("MissingPermission")
    fun bondedDevices(): List<BluetoothDevice> = try {
        adapter?.bondedDevices?.toList().orEmpty()
    } catch (_: SecurityException) {
        emptyList()
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (closed.get()) return
        val bluetooth = adapter
        if (bluetooth == null) {
            _state.value = HidState.Unsupported
            log("This device has no Bluetooth adapter.")
            return
        }
        if (!hasBluetoothPermission()) {
            _state.value = HidState.BluetoothOff
            log("Bluetooth permission is not granted.")
            return
        }
        if (!bluetooth.isEnabled) {
            _state.value = HidState.BluetoothOff
            return
        }
        if (hidDevice != null || proxyRequested) return

        _state.value = HidState.Starting
        proxyRequested = true
        val requested = try {
            bluetooth.getProfileProxy(appContext, serviceListener, BluetoothProfile.HID_DEVICE)
        } catch (error: SecurityException) {
            log("Bluetooth permission was revoked.")
            false
        }
        if (!requested) {
            proxyRequested = false
            _state.value = HidState.Failed(
                "This phone does not expose the Bluetooth HID device profile."
            )
            log("getProfileProxy(HID_DEVICE) returned false.")
        }
    }

    /** Re-check Bluetooth after the user flips the system switch. */
    fun refresh() {
        if (closed.get()) return
        val bluetooth = adapter ?: return
        val enabled = try {
            bluetooth.isEnabled
        } catch (_: SecurityException) {
            false
        }
        if (!enabled) {
            dropProfile(closeProxy = true)
            _state.value = HidState.BluetoothOff
            return
        }
        val hid = hidDevice
        if (hid == null) {
            start()
        } else if (!registered) {
            // Android refuses registerApp while the app is not in the foreground
            // (for example while the phone is locked), so try again whenever we
            // come back to the front.
            registerAttempts = 0
            register(hid)
        }
    }

    /** Unregister and register again, e.g. after a failed attempt. */
    fun restart() {
        if (closed.get()) return
        dropProfile(closeProxy = true)
        _state.value = HidState.Starting
        start()
    }

    fun close() {
        if (!closed.compareAndSet(false, true)) return
        dropProfile(closeProxy = true)
        pump.stop()
    }

    // -------------------------------------------------------------- profile

    private val serviceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            Log.i(TAG, "HID_DEVICE profile connected")
            val hid = proxy as? BluetoothHidDevice
            if (hid == null) {
                _state.value = HidState.Failed("The HID device profile is unavailable.")
                return
            }
            hidDevice = hid
            register(hid)
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            log("The HID device profile was disconnected by the system.")
            mainHandler.removeCallbacks(registrationWatchdog)
            mainHandler.removeCallbacks(registerRetry)
            mainHandler.removeCallbacks(connectionWatchdog)
            registerAttempts = 0
            staleConnectionPolls = 0
            consecutiveRejects.set(0)
            hidDevice = null
            registered = false
            proxyRequested = false
            connectedDevice = null
            _state.value = if (isBluetoothEnabled()) {
                HidState.Failed("The HID profile stopped. Tap Retry.")
            } else {
                HidState.BluetoothOff
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun register(hid: BluetoothHidDevice) {
        val settings = BluetoothHidDeviceAppSdpSettings(
            "BlueKit",
            "Keyboard, trackpad, gamepad and media remote",
            "Alam Showkat",
            (BluetoothHidDevice.SUBCLASS1_COMBO.toInt() or
                BluetoothHidDevice.SUBCLASS2_GAMEPAD.toInt()).toByte(),
            HidDescriptor.DESCRIPTOR
        )

        _state.value = HidState.Registering
        mainHandler.removeCallbacks(registrationWatchdog)
        mainHandler.removeCallbacks(registerRetry)
        val accepted = try {
            hid.registerApp(settings, null, null, mainExecutor, callback)
        } catch (error: SecurityException) {
            log("registerApp was denied: ${error.message}")
            Log.w(TAG, "registerApp denied", error)
            false
        } catch (error: IllegalArgumentException) {
            log("registerApp rejected the descriptor: ${error.message}")
            Log.w(TAG, "registerApp rejected the descriptor", error)
            false
        }

        Log.i(TAG, "registerApp returned $accepted")
        if (accepted) {
            mainHandler.postDelayed(registrationWatchdog, REGISTRATION_TIMEOUT_MS)
        } else if (registerAttempts >= MAX_REGISTER_ATTEMPTS) {
            _state.value = HidState.Failed(
                "Could not register HID. Another HID app may be active, or this phone blocks it."
            )
            log("registerApp returned false.")
        } else {
            mainHandler.postDelayed(registerRetry, REGISTER_RETRY_MS)
        }
    }

    @SuppressLint("MissingPermission")
    private fun unregisterQuietly(hid: BluetoothHidDevice?) {
        try {
            hid?.unregisterApp()
        } catch (_: Exception) {
            // Bluetooth may already be off, or nothing is registered.
        }
    }

    @SuppressLint("MissingPermission")
    private fun dropProfile(closeProxy: Boolean) {
        mainHandler.removeCallbacks(registrationWatchdog)
        mainHandler.removeCallbacks(registerRetry)
        mainHandler.removeCallbacks(connectionWatchdog)
        registerAttempts = 0
        staleConnectionPolls = 0
        consecutiveRejects.set(0)
        val hid = hidDevice
        // Always ask the stack to unregister, even when we believe nothing is
        // registered: a Bluetooth restart can leave a stale registration that
        // would otherwise make the next registerApp a silent no-op.
        ignoreNextUnregister = true
        unregisterQuietly(hid)
        registered = false
        hidDevice = null
        connectedDevice = null
        pump.reset()
        if (closeProxy && proxyRequested && hid != null) {
            try {
                adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hid)
            } catch (_: Exception) {
                // The proxy was never opened or Bluetooth is already gone.
            }
        }
        proxyRequested = false
    }

    private val callback = object : BluetoothHidDevice.Callback() {

        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            this@HidController.registered = registered
            Log.i(TAG, "onAppStatusChanged registered=$registered device=${pluggedDevice?.address}")
            if (registered) {
                registerAttempts = 0
                ignoreNextUnregister = false
                mainHandler.removeCallbacks(registrationWatchdog)
                log("HID profile registered. Pair from the host device.")
                val device = pluggedDevice ?: connectedDevice
                if (device != null) {
                    connectedDevice = device
                    _state.value = connectedState(device)
                    startConnectionWatchdog()
                } else {
                    _state.value = HidState.Ready
                }
            } else {
                log("Android unregistered the HID profile.")
                if (ignoreNextUnregister) {
                    ignoreNextUnregister = false
                } else {
                    _state.value = HidState.Failed("The HID profile was unregistered.")
                }
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            Log.i(TAG, "onConnectionStateChanged ${device.address} state=$state")
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedDevice = device
                    _state.value = connectedState(device)
                    startConnectionWatchdog()
                    log("Host connected: ${deviceLabel(device)}")
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    mainHandler.removeCallbacks(connectionWatchdog)
                    if (connectedDevice?.address == device.address) {
                        connectedDevice = null
                    }
                    consecutiveRejects.set(0)
                    staleConnectionPolls = 0
                    lastReports.clear()
                    pump.reset()
                    _state.value = if (registered) {
                        HidState.Ready
                    } else {
                        HidState.Failed("The HID profile is not registered. Tap Retry.")
                    }
                    log("Host disconnected: ${deviceLabel(device)}")
                }

                BluetoothProfile.STATE_CONNECTING -> log("Connecting to ${deviceLabel(device)}…")
                BluetoothProfile.STATE_DISCONNECTING -> log("Disconnecting ${deviceLabel(device)}…")
            }
        }

        @SuppressLint("MissingPermission")
        override fun onGetReport(
            device: BluetoothDevice,
            type: Byte,
            id: Byte,
            bufferSize: Int
        ) {
            val reportId = if (id.toInt() == 0) HidDescriptor.KEYBOARD_REPORT_ID else id.toInt()
            val report = lastReports[reportId] ?: when (reportId) {
                HidDescriptor.KEYBOARD_REPORT_ID -> ByteArray(8)
                HidDescriptor.MOUSE_REPORT_ID -> ByteArray(4)
                HidDescriptor.CONSUMER_REPORT_ID -> ByteArray(2)
                HidDescriptor.GAMEPAD_REPORT_ID -> GamepadState().encode()
                else -> ByteArray(8)
            }
            try {
                hidDevice?.replyReport(device, type, id, report)
            } catch (_: Exception) {
                // The host went away between the request and the reply.
            }
        }

        @SuppressLint("MissingPermission")
        override fun onSetReport(
            device: BluetoothDevice,
            type: Byte,
            id: Byte,
            data: ByteArray
        ) {
            // Keyboard LED state arrives here. We do not render LEDs, but the
            // host still expects a successful handshake.
            try {
                hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
            } catch (_: Exception) {
                // Ignore.
            }
        }

        override fun onSetProtocol(device: BluetoothDevice, protocol: Byte) {
            log(
                if (protocol == BluetoothHidDevice.PROTOCOL_BOOT_MODE) {
                    "Host requested boot protocol; BlueKit uses report protocol."
                } else {
                    "Host selected report protocol."
                }
            )
        }

        override fun onInterruptData(device: BluetoothDevice, reportId: Byte, data: ByteArray) {
            // Output reports (keyboard LEDs). Nothing to display yet.
        }

        override fun onVirtualCableUnplug(device: BluetoothDevice) {
            log("Virtual cable unplugged: ${deviceLabel(device)}")
            mainHandler.removeCallbacks(connectionWatchdog)
            connectedDevice = null
            consecutiveRejects.set(0)
            staleConnectionPolls = 0
            lastReports.clear()
            pump.reset()
            _state.value = if (registered) HidState.Ready else HidState.Starting
        }
    }

    // ------------------------------------------------------------ connection

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice): Boolean {
        val hid = hidDevice
        if (hid == null) {
            log("Connect ignored: the HID profile is not registered yet.")
            return false
        }
        return try {
            val started = hid.connect(device)
            log(
                if (started) {
                    "Asked ${deviceLabel(device)} to connect."
                } else {
                    "Android refused the connection to ${deviceLabel(device)}."
                }
            )
            started
        } catch (error: SecurityException) {
            log("Bluetooth permission is required to connect.")
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect(): Boolean {
        val hid = hidDevice ?: return false
        val device = connectedDevice ?: return false
        return try {
            hid.disconnect(device)
        } catch (_: SecurityException) {
            false
        }
    }

    private fun connectedState(device: BluetoothDevice) =
        HidState.Connected(deviceLabel(device), device.address)

    @SuppressLint("MissingPermission")
    private fun deviceLabel(device: BluetoothDevice): String = try {
        device.name?.takeIf { it.isNotBlank() } ?: device.address
    } catch (_: SecurityException) {
        device.address
    }

    // --------------------------------------------------------------- reports

    fun sendKeyboard(modifiers: Int, keys: IntArray) {
        send(HidDescriptor.KEYBOARD_REPORT_ID, KeyboardReport.encode(modifiers, keys))
    }

    fun sendMouse(buttons: Int, dx: Int, dy: Int, wheel: Int) {
        if (closed.get()) return
        if (connectedDevice == null) return
        pump.postMouse(buttons, dx, dy, wheel)
    }

    fun sendConsumer(usage: Int) {
        send(HidDescriptor.CONSUMER_REPORT_ID, ConsumerReport.encode(usage))
    }

    fun sendGamepad(state: GamepadState) {
        if (closed.get()) return
        if (connectedDevice == null) return
        // Full-state report: a newer sample supersedes an older queued one.
        pump.post(HidDescriptor.GAMEPAD_REPORT_ID, state.encode(), coalesce = true)
    }

    private fun send(reportId: Int, report: ByteArray) {
        if (closed.get()) return
        if (connectedDevice == null) return
        pump.post(reportId, report)
    }

    /** Runs on the pump thread: the only place that talks to the stack. */
    @SuppressLint("MissingPermission")
    private fun deliver(reportId: Int, report: ByteArray): Boolean {
        if (closed.get()) return false
        val device = connectedDevice ?: return false
        val hid = hidDevice ?: return false
        lastReports[reportId] = report
        val started = System.nanoTime()
        val accepted = try {
            hid.sendReport(device, reportId, report)
        } catch (_: SecurityException) {
            // Permission was revoked; the UI will show the Bluetooth state.
            return false
        } catch (_: IllegalArgumentException) {
            // Malformed report; should not happen with our fixed layouts.
            return false
        }
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        val now = System.currentTimeMillis()
        if (accepted) {
            consecutiveRejects.set(0)
        } else {
            // A dead host leaves the profile state at CONNECTED on some stacks,
            // so a run of rejected reports is the reliable signal that it is gone.
            // Requiring the failures to last a while keeps a short burst of
            // backpressure from looking like a disconnect.
            if (consecutiveRejects.incrementAndGet() == 1) {
                firstRejectAt.set(now)
            }
            if (consecutiveRejects.get() >= MAX_CONSECUTIVE_REJECTS &&
                now - firstRejectAt.get() >= REJECT_WINDOW_MS
            ) {
                consecutiveRejects.set(0)
                Log.w(TAG, "sendReport rejected $MAX_CONSECUTIVE_REJECTS times; link is gone")
                mainHandler.post { handleLinkLost(device) }
            }
            if (now - lastRejectedLogAt.get() > 5_000) {
                lastRejectedLogAt.set(now)
                log("The Bluetooth stack rejected a report. Is the host still connected?")
                Log.w(TAG, "sendReport rejected id=$reportId")
            }
        }
        if (elapsedMs >= 25 && now - lastSlowLogAt.get() > 5_000) {
            lastSlowLogAt.set(now)
            log("The Bluetooth stack took ${elapsedMs} ms to accept a report.")
            Log.w(TAG, "sendReport took ${elapsedMs}ms id=$reportId")
        }
        return accepted
    }

    // ------------------------------------------------------------------ logs

    private fun log(message: String) {
        val line = "${timeFormat.format(LocalTime.now())}  $message"
        _logs.update { previous -> (previous + line).takeLast(60) }
    }
}
