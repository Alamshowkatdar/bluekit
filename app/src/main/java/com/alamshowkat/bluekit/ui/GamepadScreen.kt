package com.alamshowkat.bluekit.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alamshowkat.bluekit.hid.GamepadButton
import com.alamshowkat.bluekit.hid.GamepadState
import com.alamshowkat.bluekit.hid.HidState
import com.alamshowkat.bluekit.ui.theme.Amber
import com.alamshowkat.bluekit.ui.theme.Electric
import com.alamshowkat.bluekit.ui.theme.Ink
import com.alamshowkat.bluekit.ui.theme.Line
import com.alamshowkat.bluekit.ui.theme.Mint
import com.alamshowkat.bluekit.ui.theme.Muted
import com.alamshowkat.bluekit.ui.theme.Panel
import com.alamshowkat.bluekit.ui.theme.Paper
import com.alamshowkat.bluekit.ui.theme.Rose
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun GamepadScreen(
    state: HidState,
    gamepad: GamepadState,
    onBack: () -> Unit,
    onButton: (Int, Boolean) -> Unit,
    onLeftStick: (Float, Float) -> Unit,
    onRightStick: (Float, Float) -> Unit,
    onTrigger: (Boolean, Int) -> Unit,
    onHat: (Int) -> Unit,
    onRelease: () -> Unit
) {
    DisposableEffect(Unit) {
        onDispose { onRelease() }
    }

    ModeScaffold(
        title = "Gamepad",
        subtitle = "XInput-style layout · 16 buttons, two sticks, analog triggers",
        state = state,
        onBack = onBack
    ) {
        DisconnectedNote(state, Modifier.padding(bottom = 8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StickPad(
                label = "LEFT STICK",
                x = (gamepad.leftX - 128) / 127f,
                y = (gamepad.leftY - 128) / 127f,
                accent = Mint,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onChange = onLeftStick
            )
            StickPad(
                label = "RIGHT STICK",
                x = (gamepad.rightX - 128) / 127f,
                y = (gamepad.rightY - 128) / 127f,
                accent = Electric,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onChange = onRightStick
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HatPad(
                hat = gamepad.hat,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onChange = onHat
            )
            FaceButtons(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onButton = onButton
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TriggerPad(
                label = "LT",
                value = gamepad.leftTrigger,
                accent = Amber,
                modifier = Modifier.weight(1f),
                onChange = { onTrigger(true, it) }
            )
            TriggerPad(
                label = "RT",
                value = gamepad.rightTrigger,
                accent = Amber,
                modifier = Modifier.weight(1f),
                onChange = { onTrigger(false, it) }
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PadButton(
                label = "LB",
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
                onPress = { onButton(GamepadButton.LEFT_BUMPER, true) },
                onRelease = { onButton(GamepadButton.LEFT_BUMPER, false) }
            )
            PadButton(
                label = "BACK",
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
                onPress = { onButton(GamepadButton.BACK, true) },
                onRelease = { onButton(GamepadButton.BACK, false) }
            )
            PadButton(
                label = "GUIDE",
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
                accent = Mint,
                onPress = { onButton(GamepadButton.GUIDE, true) },
                onRelease = { onButton(GamepadButton.GUIDE, false) }
            )
            PadButton(
                label = "START",
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
                onPress = { onButton(GamepadButton.START, true) },
                onRelease = { onButton(GamepadButton.START, false) }
            )
            PadButton(
                label = "RB",
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
                onPress = { onButton(GamepadButton.RIGHT_BUMPER, true) },
                onRelease = { onButton(GamepadButton.RIGHT_BUMPER, false) }
            )
        }

        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun StickPad(
    label: String,
    x: Float,
    y: Float,
    accent: Color,
    modifier: Modifier = Modifier,
    onChange: (Float, Float) -> Unit
) {
    val currentOnChange by rememberUpdatedState(onChange)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Panel.copy(alpha = 0.55f))
            .border(1.dp, Line.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
            .pointerInput(Unit) {
                fun emit(position: Offset) {
                    val pad = 16.dp.toPx()
                    val radius = ((minOf(size.width, size.height) - 2f * pad) / 2f)
                        .coerceAtLeast(1f)
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    var nx = (position.x - cx) / radius
                    var ny = (position.y - cy) / radius
                    val length = hypot(nx, ny)
                    if (length > 1f) {
                        nx /= length
                        ny /= length
                    }
                    if (length < 0.14f) {
                        nx = 0f
                        ny = 0f
                    }
                    currentOnChange(nx, ny)
                }

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    emit(down.position)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        emit(change.position)
                        change.consume()
                    }
                    currentOnChange(0f, 0f)
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(color = Ink.copy(alpha = 0.55f), radius = radius, center = center)
            drawCircle(
                color = accent.copy(alpha = 0.28f),
                radius = radius,
                center = center,
                style = Stroke(width = 2f)
            )
            drawLine(
                color = Line.copy(alpha = 0.5f),
                start = Offset(center.x - radius, center.y),
                end = Offset(center.x + radius, center.y),
                strokeWidth = 1f
            )
            drawLine(
                color = Line.copy(alpha = 0.5f),
                start = Offset(center.x, center.y - radius),
                end = Offset(center.x, center.y + radius),
                strokeWidth = 1f
            )
            val knobRadius = radius * 0.34f
            val travel = radius - knobRadius
            val knob = Offset(
                x = center.x + x.coerceIn(-1f, 1f) * travel,
                y = center.y + y.coerceIn(-1f, 1f) * travel
            )
            drawCircle(color = accent.copy(alpha = 0.2f), radius = knobRadius, center = knob)
            drawCircle(
                color = accent,
                radius = knobRadius,
                center = knob,
                style = Stroke(width = 2.5f)
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
            color = Muted,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
        )
    }
}

@Composable
private fun HatPad(
    hat: Int,
    modifier: Modifier = Modifier,
    onChange: (Int) -> Unit
) {
    val currentOnChange by rememberUpdatedState(onChange)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Panel.copy(alpha = 0.55f))
            .border(1.dp, Line.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
            .pointerInput(Unit) {
                fun emit(position: Offset) {
                    val pad = 16.dp.toPx()
                    val radius = ((minOf(size.width, size.height) - 2f * pad) / 2f)
                        .coerceAtLeast(1f)
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val dx = position.x - cx
                    val dy = position.y - cy
                    val distance = hypot(dx, dy)
                    if (distance < radius * 0.16f) {
                        currentOnChange(GamepadState.HAT_NEUTRAL)
                        return
                    }
                    val angle = atan2(dx, -dy)
                    val index = ((angle / (PI / 4f)).roundToInt() + 8) % 8
                    currentOnChange(index)
                }

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    emit(down.position)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        emit(change.position)
                        change.consume()
                    }
                    currentOnChange(GamepadState.HAT_NEUTRAL)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(color = Ink.copy(alpha = 0.55f), radius = radius, center = center)
            drawCircle(
                color = Amber.copy(alpha = 0.26f),
                radius = radius,
                center = center,
                style = Stroke(width = 2f)
            )
            for (i in 0 until 8) {
                val angle = i * (PI / 4f) - PI / 2f
                val start = Offset(
                    center.x + (radius * 0.72f) * kotlin.math.cos(angle).toFloat(),
                    center.y + (radius * 0.72f) * kotlin.math.sin(angle).toFloat()
                )
                val end = Offset(
                    center.x + (radius * 0.9f) * kotlin.math.cos(angle).toFloat(),
                    center.y + (radius * 0.9f) * kotlin.math.sin(angle).toFloat()
                )
                drawLine(
                    color = if (i == hat) Amber else Line.copy(alpha = 0.7f),
                    start = start,
                    end = end,
                    strokeWidth = if (i == hat) 3f else 1.5f
                )
            }
            drawCircle(
                color = if (hat in 0..7) Amber.copy(alpha = 0.22f) else Line.copy(alpha = 0.35f),
                radius = radius * 0.34f,
                center = center
            )
        }

        Text(
            text = if (hat in 0..7) HAT_ARROWS[hat] else "•",
            fontSize = 26.sp,
            color = if (hat in 0..7) Amber else Muted,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
        Text(
            text = "D-PAD",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
            color = Muted,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
        )
    }
}

private val HAT_ARROWS = listOf("▲", "↗", "▶", "↘", "▼", "↙", "◀", "↖")

@Composable
private fun FaceButtons(
    modifier: Modifier = Modifier,
    onButton: (Int, Boolean) -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Panel.copy(alpha = 0.55f))
            .border(1.dp, Line.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
    ) {
        FaceButton(
            label = "Y",
            accent = Amber,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 22.dp),
            onPress = { onButton(GamepadButton.Y, true) },
            onRelease = { onButton(GamepadButton.Y, false) }
        )
        FaceButton(
            label = "X",
            accent = Electric,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 18.dp),
            onPress = { onButton(GamepadButton.X, true) },
            onRelease = { onButton(GamepadButton.X, false) }
        )
        FaceButton(
            label = "B",
            accent = Rose,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 18.dp),
            onPress = { onButton(GamepadButton.B, true) },
            onRelease = { onButton(GamepadButton.B, false) }
        )
        FaceButton(
            label = "A",
            accent = Mint,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 22.dp),
            onPress = { onButton(GamepadButton.A, true) },
            onRelease = { onButton(GamepadButton.A, false) }
        )
        Text(
            text = "FACE",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
            color = Muted,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 10.dp)
        )
    }
}

@Composable
private fun FaceButton(
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    PadButton(
        label = label,
        modifier = modifier.size(50.dp),
        shape = CircleShape,
        accent = accent,
        onPress = onPress,
        onRelease = onRelease
    )
}

@Composable
private fun TriggerPad(
    label: String,
    value: Int,
    accent: Color,
    modifier: Modifier = Modifier,
    onChange: (Int) -> Unit
) {
    val currentOnChange by rememberUpdatedState(onChange)
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(Panel.copy(alpha = 0.8f))
            .border(1.dp, Line.copy(alpha = 0.7f), shape)
            .pointerInput(Unit) {
                fun emit(position: Offset) {
                    val fraction = 1f - (position.y / size.height.toFloat())
                    currentOnChange((fraction * 255f).roundToInt().coerceIn(0, 255))
                }

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    emit(down.position)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        emit(change.position)
                        change.consume()
                    }
                    currentOnChange(0)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .fillMaxHeight(fraction = (value / 255f).coerceIn(0f, 1f))
                .background(accent.copy(alpha = 0.22f))
        )
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (value > 0) accent else Paper,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${value * 100 / 255}%",
                style = MaterialTheme.typography.labelSmall,
                color = Muted
            )
        }
    }
}

@Composable
private fun PadButton(
    label: String,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp),
    accent: Color = Electric,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    var held by remember { mutableStateOf(false) }
    val currentPress by rememberUpdatedState(onPress)
    val currentRelease by rememberUpdatedState(onRelease)

    Box(
        modifier = modifier
            .clip(shape)
            .background(if (held) accent else Panel.copy(alpha = 0.9f))
            .border(1.dp, if (held) accent else Line.copy(alpha = 0.75f), shape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        held = true
                        currentPress()
                        try {
                            tryAwaitRelease()
                        } finally {
                            held = false
                            currentRelease()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (held) Ink else Paper,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = if (label.length > 4) 10.sp else 13.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
