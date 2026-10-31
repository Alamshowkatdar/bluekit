package com.alamshowkat.bluekit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alamshowkat.bluekit.hid.HidState
import com.alamshowkat.bluekit.hid.MouseButton
import com.alamshowkat.bluekit.ui.theme.Electric
import com.alamshowkat.bluekit.ui.theme.Line
import com.alamshowkat.bluekit.ui.theme.Mint
import com.alamshowkat.bluekit.ui.theme.Muted
import com.alamshowkat.bluekit.ui.theme.Night
import com.alamshowkat.bluekit.ui.theme.Panel
import com.alamshowkat.bluekit.ui.theme.Paper
import java.util.Locale

@Composable
fun TrackpadScreen(
    state: HidState,
    dragLocked: Boolean,
    onBack: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onScroll: (Int) -> Unit,
    onClick: (Int) -> Unit,
    onToggleDrag: () -> Unit
) {
    var sensitivity by rememberSaveable { mutableFloatStateOf(1.5f) }

    ModeScaffold(
        title = "Trackpad",
        subtitle = "Drag to move · tap to click · two fingers to scroll",
        state = state,
        onBack = onBack
    ) {
        DisconnectedNote(state, Modifier.padding(bottom = 8.dp))
        TouchSurface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            sensitivity = sensitivity,
            dragLocked = dragLocked,
            onMove = onMove,
            onScroll = onScroll,
            onClick = onClick
        )

        Spacer(Modifier.height(10.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Pointer speed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Paper
                )
                Spacer(Modifier.weight(1f))
                Text(
                    String.format(Locale.US, "%.1f×", sensitivity),
                    style = MaterialTheme.typography.labelMedium,
                    color = Mint
                )
            }
            Slider(
                value = sensitivity,
                onValueChange = { sensitivity = it },
                valueRange = 0.5f..3f,
                colors = SliderDefaults.colors(
                    thumbColor = Mint,
                    activeTrackColor = Mint,
                    inactiveTrackColor = Line
                )
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrackButton("Left", Modifier.weight(1f)) { onClick(MouseButton.LEFT) }
                TrackButton("Middle", Modifier.weight(1f)) { onClick(MouseButton.MIDDLE) }
                TrackButton("Right", Modifier.weight(1f)) { onClick(MouseButton.RIGHT) }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onToggleDrag,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (dragLocked) "Drag locked — tap to release" else "Drag lock (hold left button)",
                    color = if (dragLocked) Mint else Paper,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun TrackButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(label, fontSize = 13.sp)
    }
}

@Composable
private fun TouchSurface(
    modifier: Modifier,
    sensitivity: Float,
    dragLocked: Boolean,
    onMove: (Int, Int) -> Unit,
    onScroll: (Int) -> Unit,
    onClick: (Int) -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Night, Panel.copy(alpha = 0.72f), Night)
                )
            )
            .border(1.dp, Line.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
            .drawBehind {
                val step = 28.dp.toPx()
                val line = Electric.copy(alpha = 0.045f)
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
            }
            .pointerInput(sensitivity, dragLocked) {
                awaitEachGesture {
                    val first = awaitFirstDown(requireUnconsumed = false)
                    var lastCentroid = first.position
                    var pointerCount = 1
                    var maxPointers = 1
                    var totalMovement = 0f
                    var scrollAccum = 0f
                    var residualX = 0f
                    var residualY = 0f
                    var moved = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.isEmpty()) break

                        val centroid = pressed.fold(Offset.Zero) { acc, change ->
                            acc + change.position
                        } / pressed.size.toFloat()

                        if (pressed.size == pointerCount) {
                            val delta = centroid - lastCentroid
                            totalMovement += delta.getDistance()

                            if (pressed.size == 1) {
                                if (totalMovement > viewConfiguration.touchSlop) {
                                    moved = true
                                    residualX += delta.x * sensitivity
                                    residualY += delta.y * sensitivity
                                    val dx = residualX.toInt()
                                    val dy = residualY.toInt()
                                    if (dx != 0 || dy != 0) {
                                        residualX -= dx
                                        residualY -= dy
                                        onMove(dx, dy)
                                    }
                                }
                            } else if (pressed.size == 2) {
                                scrollAccum += delta.y * sensitivity
                                val steps = (scrollAccum / 26f).toInt()
                                if (steps != 0) {
                                    scrollAccum -= steps * 26f
                                    onScroll(steps)
                                }
                            }
                        }

                        pointerCount = pressed.size
                        maxPointers = maxOf(maxPointers, pressed.size)
                        lastCentroid = centroid
                        event.changes.forEach { it.consume() }
                    }

                    if (!moved && totalMovement < viewConfiguration.touchSlop) {
                        when {
                            maxPointers >= 3 -> onClick(MouseButton.MIDDLE)
                            maxPointers == 2 -> onClick(MouseButton.RIGHT)
                            !dragLocked -> onClick(MouseButton.LEFT)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.Mouse,
                contentDescription = null,
                tint = Electric.copy(alpha = 0.5f),
                modifier = Modifier.size(38.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Touch to control the pointer",
                style = MaterialTheme.typography.bodyMedium,
                color = Muted
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Tap = left · two-finger tap = right · three = middle",
                style = MaterialTheme.typography.bodySmall,
                color = Muted.copy(alpha = 0.7f)
            )
        }
    }
}
