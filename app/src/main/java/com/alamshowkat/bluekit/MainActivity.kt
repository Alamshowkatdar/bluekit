package com.alamshowkat.bluekit

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alamshowkat.bluekit.ui.BlueprintBackground
import com.alamshowkat.bluekit.ui.GamepadScreen
import com.alamshowkat.bluekit.ui.HomeScreen
import com.alamshowkat.bluekit.ui.KeyboardScreen
import com.alamshowkat.bluekit.ui.Mode
import com.alamshowkat.bluekit.ui.RemoteScreen
import com.alamshowkat.bluekit.ui.TrackpadScreen
import com.alamshowkat.bluekit.ui.theme.BlueKitTheme
import com.alamshowkat.bluekit.ui.theme.Electric
import com.alamshowkat.bluekit.ui.theme.Ink
import com.alamshowkat.bluekit.ui.theme.Muted
import com.alamshowkat.bluekit.ui.theme.Paper

class MainActivity : ComponentActivity() {

    private val viewModel: BlueKitViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            BlueKitTheme {
                BlueKitApp(viewModel)
            }
        }
    }

    override fun onStop() {
        viewModel.releaseAll()
        super.onStop()
    }
}

@SuppressLint("InlinedApi")
@Composable
private fun BlueKitApp(viewModel: BlueKitViewModel) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val keyboard by viewModel.keyboard.collectAsStateWithLifecycle()
    val gamepad by viewModel.gamepad.collectAsStateWithLifecycle()
    val dragLocked by viewModel.dragLocked.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()

    var mode by rememberSaveable { mutableStateOf<Mode?>(null) }
    var permissionsGranted by remember { mutableStateOf(hasBluetoothPermissions(context)) }
    var devices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }

    fun refreshDevices() {
        devices = viewModel.bondedDevices()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionsGranted = result.values.all { it }
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshBluetooth()
    }

    val discoverableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshBluetooth()
    }

    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted) {
            viewModel.startHid()
            refreshDevices()
        }
    }

    LaunchedEffect(mode) {
        if (mode == null) refreshDevices()
    }

    // Leaving a mode must not leave a key, button or stick held on the host.
    DisposableEffect(mode) {
        val leavingMode = mode
        onDispose {
            if (leavingMode != null) viewModel.releaseAll()
        }
    }

    // Follow the system Bluetooth switch and pairing changes while the app is open.
    DisposableEffect(permissionsGranted) {
        if (!permissionsGranted) {
            onDispose { }
        } else {
            val filter = IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            }
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(receiverContext: Context, intent: Intent) {
                    when (intent.action) {
                        BluetoothAdapter.ACTION_STATE_CHANGED -> {
                            val adapterState = intent.getIntExtra(
                                BluetoothAdapter.EXTRA_STATE,
                                BluetoothAdapter.ERROR
                            )
                            if (adapterState == BluetoothAdapter.STATE_ON) {
                                viewModel.refreshBluetooth()
                                refreshDevices()
                            } else if (adapterState == BluetoothAdapter.STATE_OFF) {
                                viewModel.refreshBluetooth()
                            }
                        }

                        BluetoothDevice.ACTION_BOND_STATE_CHANGED -> refreshDevices()
                    }
                }
            }
            val registered = try {
                ContextCompat.registerReceiver(
                    context,
                    receiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
                true
            } catch (_: SecurityException) {
                false
            }
            onDispose {
                if (registered) {
                    try {
                        context.unregisterReceiver(receiver)
                    } catch (_: IllegalArgumentException) {
                        // Already unregistered.
                    }
                }
            }
        }
    }

    BackHandler(enabled = mode != null) {
        mode = null
    }

    if (!permissionsGranted) {
        PermissionGate(
            onRequest = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_ADVERTISE
                    )
                )
            }
        )
        return
    }

    AnimatedContent(
        targetState = mode,
        transitionSpec = {
            (fadeIn(tween(180)) + slideInHorizontally(tween(220)) { it / 12 }) togetherWith
                (fadeOut(tween(140)) + slideOutHorizontally(tween(180)) { -it / 12 })
        },
        label = "mode"
    ) { current ->
        when (current) {
            null -> HomeScreen(
                state = state,
                logs = logs,
                adapterName = viewModel.adapterName(),
                bondedDevices = devices,
                onEnableBluetooth = {
                    enableBluetoothLauncher.launch(
                        Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    )
                },
                onDiscoverable = {
                    discoverableLauncher.launch(
                        Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                            .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                    )
                },
                onRetry = { viewModel.restartHid() },
                onRefreshDevices = { refreshDevices() },
                onConnect = { device -> viewModel.connect(device) },
                onDisconnect = { viewModel.disconnect() },
                onOpenMode = { mode = it }
            )

            Mode.KEYBOARD -> KeyboardScreen(
                state = state,
                keyboard = keyboard,
                onBack = { mode = null },
                onPressKey = viewModel::pressKey,
                onReleaseKey = viewModel::releaseKey,
                onTapKey = viewModel::tapKey,
                onToggleModifier = viewModel::toggleModifier,
                onReleaseAll = viewModel::releaseAllKeys
            )

            Mode.TRACKPAD -> TrackpadScreen(
                state = state,
                dragLocked = dragLocked,
                onBack = { mode = null },
                onMove = viewModel::mouseMove,
                onScroll = viewModel::mouseScroll,
                onClick = viewModel::click,
                onToggleDrag = { viewModel.toggleDragLock() }
            )

            Mode.GAMEPAD -> GamepadScreen(
                state = state,
                gamepad = gamepad,
                onBack = { mode = null },
                onButton = viewModel::setGamepadButton,
                onLeftStick = viewModel::setLeftStick,
                onRightStick = viewModel::setRightStick,
                onTrigger = viewModel::setTrigger,
                onHat = viewModel::setHat,
                onRelease = viewModel::releaseGamepad
            )

            Mode.REMOTE -> RemoteScreen(
                state = state,
                onBack = { mode = null },
                onMediaDown = viewModel::mediaDown,
                onMediaUp = viewModel::mediaUp,
                onMediaTap = viewModel::mediaTap,
                onShortcut = { usage -> viewModel.tapShortcut(usage) }
            )
        }
    }
}

@Composable
private fun PermissionGate(onRequest: () -> Unit) {
    BlueprintBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Electric.copy(alpha = 0.14f))
                    .border(1.dp, Electric.copy(alpha = 0.35f), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Bluetooth,
                    contentDescription = null,
                    tint = Electric,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Bluetooth access is required",
                style = MaterialTheme.typography.headlineMedium,
                color = Paper,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Android 12 and newer only let an app register a Bluetooth HID device and " +
                    "become discoverable after you grant Bluetooth permission.",
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(26.dp))
            Button(
                onClick = onRequest,
                modifier = Modifier.height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Electric,
                    contentColor = Ink
                )
            ) {
                Text("Grant Bluetooth access", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "BlueKit never scans for nearby devices. The permission is used only for the " +
                    "keyboard, trackpad, gamepad and media remote profiles.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun hasBluetoothPermissions(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.BLUETOOTH_CONNECT
    ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_ADVERTISE
        ) == PackageManager.PERMISSION_GRANTED
}
