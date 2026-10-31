package com.alamshowkat.bluekit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alamshowkat.bluekit.hid.HidState
import com.alamshowkat.bluekit.ui.theme.Amber
import com.alamshowkat.bluekit.ui.theme.Electric
import com.alamshowkat.bluekit.ui.theme.Ink
import com.alamshowkat.bluekit.ui.theme.Line
import com.alamshowkat.bluekit.ui.theme.Mint
import com.alamshowkat.bluekit.ui.theme.Muted
import com.alamshowkat.bluekit.ui.theme.Night
import com.alamshowkat.bluekit.ui.theme.Panel
import com.alamshowkat.bluekit.ui.theme.Paper
import com.alamshowkat.bluekit.ui.theme.Rose

/** Ink gradient with a faint blueprint grid. */
@Composable
fun BlueprintBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Ink, Color(0xFF081426), Ink)
                )
            )
            .drawBehind {
                val step = 34.dp.toPx()
                val line = Electric.copy(alpha = 0.035f)
                var x = 0f
                while (x < size.width) {
                    drawLine(line, Offset(x, 0f), Offset(x, size.height), 1f)
                    x += step
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(line, Offset(0f, y), Offset(size.width, y), 1f)
                    y += step
                }
            },
        content = content
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Panel.copy(alpha = 0.94f), Night.copy(alpha = 0.94f))
                )
            )
            .border(1.dp, Line.copy(alpha = 0.75f), RoundedCornerShape(22.dp))
            .padding(18.dp),
        content = content
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
        color = Muted,
        modifier = modifier
    )
}

@Composable
fun MonoText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Paper,
    size: Int = 13
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = size.sp
    )
}

@Composable
fun ConnectionPill(state: HidState, modifier: Modifier = Modifier) {
    val label = when (state) {
        HidState.Unsupported -> "No Bluetooth"
        HidState.BluetoothOff -> "Bluetooth off"
        HidState.Starting, HidState.Registering -> "Starting…"
        HidState.Ready -> "Ready to pair"
        is HidState.Connected -> "Connected"
        is HidState.Failed -> "Needs attention"
    }
    val color = when (state) {
        HidState.Unsupported, is HidState.Failed -> Rose
        HidState.BluetoothOff -> Muted
        HidState.Starting, HidState.Registering -> Amber
        HidState.Ready -> Electric
        is HidState.Connected -> Mint
    }
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.38f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(7.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
fun ModeScaffold(
    title: String,
    subtitle: String,
    state: HidState,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Paper
                )
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
            }
            ConnectionPill(state)
        }
        content()
    }
}

@Composable
fun DisconnectedNote(state: HidState, modifier: Modifier = Modifier) {
    if (state.isConnected) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Amber.copy(alpha = 0.08f))
            .border(1.dp, Amber.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Amber)
        )
        Text(
            "No host connected — reports are not being sent.",
            style = MaterialTheme.typography.bodySmall,
            color = Amber
        )
    }
}

@Composable
fun DividerLine(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Line.copy(alpha = 0.6f))
    )
}
