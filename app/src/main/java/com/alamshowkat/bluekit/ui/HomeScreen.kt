package com.alamshowkat.bluekit.ui

import android.bluetooth.BluetoothDevice
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alamshowkat.bluekit.hid.HidState
import com.alamshowkat.bluekit.ui.theme.Amber
import com.alamshowkat.bluekit.ui.theme.Electric
import com.alamshowkat.bluekit.ui.theme.Ink
import com.alamshowkat.bluekit.ui.theme.Mint
import com.alamshowkat.bluekit.ui.theme.Muted
import com.alamshowkat.bluekit.ui.theme.Panel
import com.alamshowkat.bluekit.ui.theme.Paper
import com.alamshowkat.bluekit.ui.theme.Rose

@Composable
fun HomeScreen(
    state: HidState,
    logs: List<String>,
    adapterName: String?,
    bondedDevices: List<BluetoothDevice>,
    onEnableBluetooth: () -> Unit,
    onDiscoverable: () -> Unit,
    onRetry: () -> Unit,
    onRefreshDevices: () -> Unit,
    onConnect: (BluetoothDevice) -> Unit,
    onDisconnect: () -> Unit,
    onOpenMode: (Mode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(18.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "BlueKit",
                    style = MaterialTheme.typography.displaySmall,
                    color = Paper
                )
                Text(
                    "One phone. Four wireless input devices.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted
                )
            }
            ConnectionPill(state)
        }

        Spacer(Modifier.height(18.dp))
        StatusCard(
            state = state,
            adapterName = adapterName,
            onEnableBluetooth = onEnableBluetooth,
            onDiscoverable = onDiscoverable,
            onRetry = onRetry,
            onDisconnect = onDisconnect
        )

        Spacer(Modifier.height(14.dp))
        HowToCard()

        Spacer(Modifier.height(22.dp))
        SectionLabel("Modes")
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ModeTile(
                modifier = Modifier.weight(1f),
                title = Mode.KEYBOARD.title,
                subtitle = Mode.KEYBOARD.subtitle,
                icon = Icons.Filled.Keyboard,
                accent = Electric,
                onClick = { onOpenMode(Mode.KEYBOARD) }
            )
            ModeTile(
                modifier = Modifier.weight(1f),
                title = Mode.TRACKPAD.title,
                subtitle = Mode.TRACKPAD.subtitle,
                icon = Icons.Filled.Mouse,
                accent = Mint,
                onClick = { onOpenMode(Mode.TRACKPAD) }
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ModeTile(
                modifier = Modifier.weight(1f),
                title = Mode.GAMEPAD.title,
                subtitle = Mode.GAMEPAD.subtitle,
                icon = Icons.Filled.SportsEsports,
                accent = Amber,
                onClick = { onOpenMode(Mode.GAMEPAD) }
            )
            ModeTile(
                modifier = Modifier.weight(1f),
                title = Mode.REMOTE.title,
                subtitle = Mode.REMOTE.subtitle,
                icon = Icons.Filled.SettingsRemote,
                accent = Rose,
                onClick = { onOpenMode(Mode.REMOTE) }
            )
        }

        Spacer(Modifier.height(22.dp))
        PairedHostsCard(
            devices = bondedDevices,
            onRefresh = onRefreshDevices,
            onConnect = onConnect
        )

        Spacer(Modifier.height(14.dp))
        ActivityCard(logs)

        Spacer(Modifier.height(18.dp))
        Text(
            "BlueKit speaks standard Bluetooth HID, so the host needs no companion app. " +
                "It works with Windows, Linux, Android and most consoles and TVs. " +
                "Apple hosts and some vendor ROMs do not expose the HID device profile.",
            style = MaterialTheme.typography.bodySmall,
            color = Muted
        )
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun StatusCard(
    state: HidState,
    adapterName: String?,
    onEnableBluetooth: () -> Unit,
    onDiscoverable: () -> Unit,
    onRetry: () -> Unit,
    onDisconnect: () -> Unit
) {
    val accent = when (state) {
        HidState.Unsupported, is HidState.Failed -> Rose
        HidState.BluetoothOff -> Muted
        HidState.Starting, HidState.Registering -> Amber
        HidState.Ready -> Electric
        is HidState.Connected -> Mint
    }
    val title = when (state) {
        HidState.Unsupported -> "No Bluetooth adapter"
        HidState.BluetoothOff -> "Bluetooth is off"
        HidState.Starting -> "Starting the HID profile"
        HidState.Registering -> "Registering with Android"
        HidState.Ready -> "Ready to pair"
        is HidState.Connected -> state.deviceName
        is HidState.Failed -> "Something is blocked"
    }
    val body = when (state) {
        HidState.Unsupported ->
            "This device cannot act as a Bluetooth HID device."
        HidState.BluetoothOff ->
            "Turn Bluetooth on so BlueKit can register its keyboard, mouse and gamepad profile."
        HidState.Starting ->
            "Asking Android for the Bluetooth HID device profile."
        HidState.Registering ->
            "Android is validating the combined report descriptor."
        HidState.Ready ->
            "Pair with this phone from the host's Bluetooth menu, or connect a paired host below."
        is HidState.Connected ->
            "Connected over Bluetooth HID. Pick a mode below."
        is HidState.Failed -> state.message
    }
    val icon = when (state) {
        is HidState.Connected -> Icons.Filled.Computer
        HidState.BluetoothOff -> Icons.Filled.BluetoothDisabled
        else -> Icons.Filled.Bluetooth
    }

    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.copy(alpha = 0.14f))
                    .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Paper)
                Spacer(Modifier.height(3.dp))
                Text(body, style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        }

        if (state is HidState.Starting || state is HidState.Registering) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Amber,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(10.dp))
                Text("Working…", style = MaterialTheme.typography.bodySmall, color = Amber)
            }
        }

        when (state) {
            HidState.BluetoothOff -> {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onEnableBluetooth,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Electric,
                        contentColor = Ink
                    )
                ) {
                    Text("Turn on Bluetooth", fontWeight = FontWeight.SemiBold)
                }
            }

            HidState.Ready -> {
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onDiscoverable,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Electric,
                            contentColor = Ink
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.BluetoothSearching, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Discoverable", fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = onRetry,
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Retry")
                    }
                }
            }

            is HidState.Connected -> {
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDiscoverable,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Pair another")
                    }
                    OutlinedButton(
                        onClick = onDisconnect,
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Disconnect")
                    }
                }
                Spacer(Modifier.height(10.dp))
                MonoText(state.address, color = Muted, size = 11)
            }

            is HidState.Failed -> {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Rose,
                        contentColor = Ink
                    )
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Try again", fontWeight = FontWeight.SemiBold)
                }
            }

            else -> Unit
        }

        if (state is HidState.Ready || state is HidState.Connected) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Bluetooth name: ${adapterName ?: "this phone"}",
                style = MaterialTheme.typography.bodySmall,
                color = Muted
            )
        }
    }
}

@Composable
private fun HowToCard() {
    GlassCard(Modifier.fillMaxWidth()) {
        SectionLabel("How to connect")
        Spacer(Modifier.height(12.dp))
        StepRow(1, "Make this phone discoverable", "Tap the button above and keep BlueKit open.")
        StepRow(2, "Pair from the host", "On the PC, console or TV, open Bluetooth settings and select this phone.")
        StepRow(3, "Pick a mode", "Open Keyboard, Trackpad, Gamepad or Media. No host software is needed.")
    }
}

@Composable
private fun StepRow(number: Int, title: String, body: String) {
    Row(modifier = Modifier.padding(vertical = 5.dp)) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Electric.copy(alpha = 0.16f))
                .border(1.dp, Electric.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = Electric,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = Paper)
            Text(body, style = MaterialTheme.typography.bodySmall, color = Muted)
        }
    }
}

@Composable
private fun ModeTile(
    modifier: Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(accent.copy(alpha = 0.14f), Panel.copy(alpha = 0.9f))
                )
            )
            .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(23.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = Paper)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Muted)
    }
}

@Composable
private fun PairedHostsCard(
    devices: List<BluetoothDevice>,
    onRefresh: () -> Unit,
    onConnect: (BluetoothDevice) -> Unit
) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Paired hosts", Modifier.weight(1f))
            IconButton(onClick = onRefresh, modifier = Modifier.size(30.dp)) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = "Refresh paired hosts",
                    tint = Muted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        if (devices.isEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                "No paired devices yet. Pair from the host's Bluetooth menu; BlueKit cannot scan for hosts.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted
            )
        } else {
            devices.take(6).forEach { device ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Computer,
                        contentDescription = null,
                        tint = Electric.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            device.safeName(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Paper
                        )
                        MonoText(device.address, color = Muted, size = 10)
                    }
                    TextButton(onClick = { onConnect(device) }) {
                        Text("Connect", color = Electric)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityCard(logs: List<String>) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    GlassCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabel("Activity log", Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Hide activity log" else "Show activity log",
                tint = Muted,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        val preview = logs.lastOrNull() ?: "Nothing yet."
        Text(
            preview,
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
            fontFamily = FontFamily.Monospace
        )
        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(top = 8.dp)) {
                logs.takeLast(12).forEach { line ->
                    Text(
                        line,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = if (line.contains("fail", true) || line.contains("denied", true)) Rose else Muted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

private fun BluetoothDevice.safeName(): String = try {
    name?.takeIf { it.isNotBlank() } ?: address
} catch (_: SecurityException) {
    address
}
