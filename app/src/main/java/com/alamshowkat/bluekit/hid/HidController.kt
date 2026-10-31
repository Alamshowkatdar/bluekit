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
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
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

    private val appContext = context.applicationContext
    private val mainExecutor = ContextCompat.getMainExecutor(appContext)

    private val adapter: BluetoothAdapter? =
        (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private val _state = MutableStateFlow<HidState>(HidState.Starting)
    val state: StateFlow<HidState> = _state.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    private val sendExecutor: ExecutorService =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "bluekit-hid-send").apply { isDaemon = true }
        }

    private val closed = AtomicBoolean(false)
    private val lastReports = ConcurrentHashMap<Int, ByteArray>()

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
        if (hidDevice == null) {
            start()
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
        sendExecutor.shutdownNow()
    }

    // -------------------------------------------------------------- profile

    private val serviceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile != BluetoothProfile.HID_DEVICE) return
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
        val accepted = try {
            hid.registerApp(settings, null, null, mainExecutor, callback)
        } catch (error: SecurityException) {
            log("registerApp was denied: ${error.message}")
            false
        } catch (error: IllegalArgumentException) {
            log("registerApp rejected the descriptor: ${error.message}")
            false
        }

        if (!accepted) {
            _state.value = HidState.Failed(
                "Could not register HID. Another HID app may be active, or this phone blocks it."
            )
            log("registerApp returned false.")
        }
    }

    @SuppressLint("MissingPermission")
    private fun dropProfile(closeProxy: Boolean) {
        val hid = hidDevice
        if (registered) {
            ignoreNextUnregister = true
            try {
                hid?.unregisterApp()
            } catch (_: Exception) {
                // Bluetooth may already be off.
            }
        }
        registered = false
        hidDevice = null
        connectedDevice = null
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
            if (registered) {
                log("HID profile registered. Pair from the host device.")
                val device = pluggedDevice ?: connectedDevice
                if (device != null) {
                    connectedDevice = device
                    _state.value = connectedState(device)
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
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedDevice = device
                    _state.value = connectedState(device)
                    log("Host connected: ${deviceLabel(device)}")
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (connectedDevice?.address == device.address) {
                        connectedDevice = null
                    }
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
            connectedDevice = null
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
        send(HidDescriptor.MOUSE_REPORT_ID, MouseReport.encode(buttons, dx, dy, wheel))
    }

    fun sendConsumer(usage: Int) {
        send(HidDescriptor.CONSUMER_REPORT_ID, ConsumerReport.encode(usage))
    }

    fun sendGamepad(state: GamepadState) {
        send(HidDescriptor.GAMEPAD_REPORT_ID, state.encode())
    }

    private fun send(reportId: Int, report: ByteArray) {
        if (closed.get()) return
        val device = connectedDevice ?: return
        lastReports[reportId] = report
        sendExecutor.execute {
            if (closed.get()) return@execute
            val hid = hidDevice ?: return@execute
            try {
                hid.sendReport(device, reportId, report)
            } catch (_: SecurityException) {
                // Permission was revoked; the UI will show the Bluetooth state.
            } catch (_: IllegalArgumentException) {
                // Malformed report; should not happen with our fixed layouts.
            }
        }
    }

    // ------------------------------------------------------------------ logs

    private fun log(message: String) {
        val line = "${timeFormat.format(Date())}  $message"
        _logs.update { previous -> (previous + line).takeLast(60) }
    }
}
