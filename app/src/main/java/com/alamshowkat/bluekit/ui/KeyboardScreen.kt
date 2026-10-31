package com.alamshowkat.bluekit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alamshowkat.bluekit.KeyboardUiState
import com.alamshowkat.bluekit.hid.HidKey
import com.alamshowkat.bluekit.hid.HidModifier
import com.alamshowkat.bluekit.hid.HidState
import com.alamshowkat.bluekit.ui.theme.Electric
import com.alamshowkat.bluekit.ui.theme.Ink
import com.alamshowkat.bluekit.ui.theme.Line
import com.alamshowkat.bluekit.ui.theme.Panel
import com.alamshowkat.bluekit.ui.theme.PanelHigh
import com.alamshowkat.bluekit.ui.theme.Paper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class KeySpec(
    val label: String,
    val usage: Int = 0,
    val modifier: Int = 0,
    val shifted: Boolean = false,
    val weight: Float = 1f,
    val repeat: Boolean = false,
    val tinted: Boolean = false,
    val pageToggle: Boolean = false
)

private fun key(
    label: String,
    usage: Int,
    weight: Float = 1f,
    repeat: Boolean = false,
    shifted: Boolean = false,
    tinted: Boolean = false
) = KeySpec(
    label = label,
    usage = usage,
    weight = weight,
    repeat = repeat,
    shifted = shifted,
    tinted = tinted
)

private fun mod(label: String, bit: Int, weight: Float = 1f) =
    KeySpec(label = label, modifier = bit, weight = weight, tinted = true)

private val ABC_ROWS = listOf(
    listOf(
        key("Esc", HidKey.ESCAPE, 1.1f, tinted = true),
        key("1", HidKey.NUM_1), key("2", HidKey.NUM_2), key("3", HidKey.NUM_3),
        key("4", HidKey.NUM_4), key("5", HidKey.NUM_5), key("6", HidKey.NUM_6),
        key("7", HidKey.NUM_7), key("8", HidKey.NUM_8), key("9", HidKey.NUM_9),
        key("0", HidKey.NUM_0),
        key("⌫", HidKey.BACKSPACE, 1.4f, repeat = true, tinted = true)
    ),
    listOf(
        key("Tab", HidKey.TAB, 1.25f, tinted = true),
        key("q", HidKey.Q), key("w", HidKey.W), key("e", HidKey.E), key("r", HidKey.R),
        key("t", HidKey.T), key("y", HidKey.Y), key("u", HidKey.U), key("i", HidKey.I),
        key("o", HidKey.O), key("p", HidKey.P),
        key("[", HidKey.LEFT_BRACKET), key("]", HidKey.RIGHT_BRACKET)
    ),
    listOf(
        key("a", HidKey.A), key("s", HidKey.S), key("d", HidKey.D), key("f", HidKey.F),
        key("g", HidKey.G), key("h", HidKey.H), key("j", HidKey.J), key("k", HidKey.K),
        key("l", HidKey.L), key(";", HidKey.SEMICOLON), key("'", HidKey.APOSTROPHE),
        key("Enter", HidKey.ENTER, 1.5f, tinted = true)
    ),
    listOf(
        key("Caps", HidKey.CAPS_LOCK, 1.3f, tinted = true),
        key("z", HidKey.Z), key("x", HidKey.X), key("c", HidKey.C), key("v", HidKey.V),
        key("b", HidKey.B), key("n", HidKey.N), key("m", HidKey.M),
        key(",", HidKey.COMMA), key(".", HidKey.PERIOD), key("/", HidKey.SLASH),
        mod("Shift", HidModifier.SHIFT, 1.35f)
    )
)

private val SYMBOL_ROWS = listOf(
    listOf(
        key("!", HidKey.NUM_1, shifted = true),
        key("@", HidKey.NUM_2, shifted = true),
        key("#", HidKey.NUM_3, shifted = true),
        key("$", HidKey.NUM_4, shifted = true),
        key("%", HidKey.NUM_5, shifted = true),
        key("^", HidKey.NUM_6, shifted = true),
        key("&", HidKey.NUM_7, shifted = true),
        key("*", HidKey.NUM_8, shifted = true),
        key("(", HidKey.NUM_9, shifted = true),
        key(")", HidKey.NUM_0, shifted = true),
        key("Del", HidKey.DELETE, 1.1f, tinted = true),
        key("⌫", HidKey.BACKSPACE, 1.3f, repeat = true, tinted = true)
    ),
    listOf(
        key("`", HidKey.GRAVE), key("~", HidKey.GRAVE, shifted = true),
        key("-", HidKey.MINUS), key("_", HidKey.MINUS, shifted = true),
        key("=", HidKey.EQUALS), key("+", HidKey.EQUALS, shifted = true),
        key("[", HidKey.LEFT_BRACKET), key("{", HidKey.LEFT_BRACKET, shifted = true),
        key("]", HidKey.RIGHT_BRACKET), key("}", HidKey.RIGHT_BRACKET, shifted = true),
        key("\\", HidKey.BACKSLASH), key("|", HidKey.BACKSLASH, shifted = true)
    ),
    listOf(
        key(";", HidKey.SEMICOLON), key(":", HidKey.SEMICOLON, shifted = true),
        key("'", HidKey.APOSTROPHE), key("\"", HidKey.APOSTROPHE, shifted = true),
        key(",", HidKey.COMMA), key("<", HidKey.COMMA, shifted = true),
        key(".", HidKey.PERIOD), key(">", HidKey.PERIOD, shifted = true),
        key("/", HidKey.SLASH), key("?", HidKey.SLASH, shifted = true),
        key("Home", HidKey.HOME, 1.1f, tinted = true),
        key("End", HidKey.END, 1.1f, tinted = true)
    ),
    listOf(
        key("F1", HidKey.F1), key("F2", HidKey.F2), key("F3", HidKey.F3),
        key("F4", HidKey.F4), key("F5", HidKey.F5), key("F6", HidKey.F6),
        key("F7", HidKey.F7), key("F8", HidKey.F8), key("F9", HidKey.F9),
        key("F10", HidKey.F10), key("F11", HidKey.F11), key("F12", HidKey.F12)
    )
)

private val ABC_BOTTOM = listOf(
    KeySpec("123", weight = 1.3f, tinted = true, pageToggle = true),
    mod("Ctrl", HidModifier.CTRL, 1.2f),
    mod("Alt", HidModifier.ALT, 1.2f),
    mod("Super", HidModifier.GUI, 1.2f),
    key("space", HidKey.SPACE, 2.4f),
    key("←", HidKey.LEFT, repeat = true),
    key("↑", HidKey.UP, repeat = true),
    key("↓", HidKey.DOWN, repeat = true),
    key("→", HidKey.RIGHT, repeat = true)
)

private val SYMBOL_BOTTOM = listOf(
    KeySpec("ABC", weight = 1.3f, tinted = true, pageToggle = true),
    mod("Ctrl", HidModifier.CTRL, 1.2f),
    mod("Alt", HidModifier.ALT, 1.2f),
    mod("Super", HidModifier.GUI, 1.2f),
    key("space", HidKey.SPACE, 2.4f),
    key("←", HidKey.LEFT, repeat = true),
    key("↑", HidKey.UP, repeat = true),
    key("↓", HidKey.DOWN, repeat = true),
    key("→", HidKey.RIGHT, repeat = true)
)

@Composable
fun KeyboardScreen(
    state: HidState,
    keyboard: KeyboardUiState,
    onBack: () -> Unit,
    onPressKey: (Int, Boolean) -> Unit,
    onReleaseKey: (Int) -> Unit,
    onTapKey: (Int, Boolean) -> Unit,
    onToggleModifier: (Int) -> Unit,
    onReleaseAll: () -> Unit
) {
    var symbols by rememberSaveable { mutableStateOf(false) }
    val rows = if (symbols) SYMBOL_ROWS else ABC_ROWS
    val bottom = if (symbols) SYMBOL_BOTTOM else ABC_BOTTOM

    ModeScaffold(
        title = "Keyboard",
        subtitle = "US QWERTY · Shift is one-shot, Ctrl/Alt/Super latch",
        state = state,
        onBack = onBack
    ) {
        DisconnectedNote(state, Modifier.padding(bottom = 8.dp))
        KeyboardSurface(
            modifier = Modifier.weight(1f),
            rows = rows,
            bottom = bottom,
            keyboard = keyboard,
            onPressKey = onPressKey,
            onReleaseKey = onReleaseKey,
            onTapKey = onTapKey,
            onToggleModifier = onToggleModifier,
            onPageToggle = {
                onReleaseAll()
                symbols = !symbols
            }
        )
    }
}

@Composable
private fun KeyboardSurface(
    modifier: Modifier = Modifier,
    rows: List<List<KeySpec>>,
    bottom: List<KeySpec>,
    keyboard: KeyboardUiState,
    onPressKey: (Int, Boolean) -> Unit,
    onReleaseKey: (Int) -> Unit,
    onTapKey: (Int, Boolean) -> Unit,
    onToggleModifier: (Int) -> Unit,
    onPageToggle: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    ) {
        rows.forEach { row ->
            KeyRow(
                modifier = Modifier.weight(1f),
                row = row,
                keyboard = keyboard,
                onPressKey = onPressKey,
                onReleaseKey = onReleaseKey,
                onTapKey = onTapKey,
                onToggleModifier = onToggleModifier,
                onPageToggle = onPageToggle
            )
        }
        KeyRow(
            modifier = Modifier.weight(1f),
            row = bottom,
            keyboard = keyboard,
            onPressKey = onPressKey,
            onReleaseKey = onReleaseKey,
            onTapKey = onTapKey,
            onToggleModifier = onToggleModifier,
            onPageToggle = onPageToggle
        )
    }
}

@Composable
private fun KeyRow(
    modifier: Modifier,
    row: List<KeySpec>,
    keyboard: KeyboardUiState,
    onPressKey: (Int, Boolean) -> Unit,
    onReleaseKey: (Int) -> Unit,
    onTapKey: (Int, Boolean) -> Unit,
    onToggleModifier: (Int) -> Unit,
    onPageToggle: () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        row.forEach { spec ->
            val pressed = spec.usage > 0 && keyboard.pressed.contains(spec.usage)
            val active = spec.modifier != 0 && (keyboard.modifiers and spec.modifier) != 0
            KeyCap(
                modifier = Modifier.weight(spec.weight),
                spec = spec,
                pressed = pressed,
                active = active,
                onPress = {
                    when {
                        spec.pageToggle -> onPageToggle()
                        spec.modifier != 0 -> onToggleModifier(spec.modifier)
                        spec.repeat -> onTapKey(spec.usage, spec.shifted)
                        else -> onPressKey(spec.usage, spec.shifted)
                    }
                },
                onRelease = {
                    if (!spec.repeat && !spec.pageToggle && spec.modifier == 0) {
                        onReleaseKey(spec.usage)
                    }
                },
                onRepeat = {
                    if (spec.repeat) onTapKey(spec.usage, spec.shifted)
                }
            )
        }
    }
}

@Composable
private fun KeyCap(
    modifier: Modifier,
    spec: KeySpec,
    pressed: Boolean,
    active: Boolean,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    onRepeat: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var held by remember { mutableStateOf(false) }
    val lit = pressed || active || held
    val background = when {
        active -> Electric
        lit -> Electric.copy(alpha = 0.72f)
        spec.tinted -> PanelHigh
        else -> Panel.copy(alpha = 0.92f)
    }
    val content = if (lit) Ink else Paper
    val border = if (lit) Electric else Line.copy(alpha = 0.55f)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(2.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(9.dp))
            .pointerInput(spec) {
                detectTapGestures(
                    onPress = {
                        held = true
                        onPress()
                        val repeatJob = if (spec.repeat) {
                            scope.launch {
                                delay(420)
                                while (true) {
                                    onRepeat()
                                    delay(55)
                                }
                            }
                        } else {
                            null
                        }
                        try {
                            tryAwaitRelease()
                        } finally {
                            repeatJob?.cancel()
                            held = false
                            onRelease()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = spec.label,
            color = content,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (lit) FontWeight.Bold else FontWeight.Medium,
            fontSize = when {
                spec.label.length >= 5 -> 9.sp
                spec.label.length >= 3 -> 10.sp
                else -> 13.sp
            },
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}
