package com.alamshowkat.bluekit

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import com.alamshowkat.bluekit.hid.GamepadState
import com.alamshowkat.bluekit.hid.HidController
import com.alamshowkat.bluekit.hid.HidKey
import com.alamshowkat.bluekit.hid.HidModifier
import com.alamshowkat.bluekit.hid.HidState
import com.alamshowkat.bluekit.hid.MouseButton
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class KeyboardUiState(
    val modifiers: Int = 0,
    val pressed: Set<Int> = emptySet()
)

/**
 * Holds the input state for every mode and forwards reports to [HidController].
 *
 * State lives here (not in the screens) so a rotation or a trip to another mode
 * cannot leave a key or a mouse button stuck down on the host.
 */
class BlueKitViewModel(application: Application) : AndroidViewModel(application) {

    val hid = HidController(application)

    val state: StateFlow<HidState> = hid.state
    val logs: StateFlow<List<String>> = hid.logs

    // --------------------------------------------------------------- keyboard

    private val _keyboard = MutableStateFlow(KeyboardUiState())
    val keyboard: StateFlow<KeyboardUiState> = _keyboard.asStateFlow()

    private var stickyModifiers = 0
    private val pressedKeys = LinkedHashMap<Int, Boolean>()

    fun toggleModifier(bit: Int) {
        stickyModifiers = stickyModifiers xor bit
        publishKeyboard()
        sendKeyboardReport()
    }

    fun pressKey(usage: Int, shifted: Boolean = false) {
        if (usage <= 0) return
        pressedKeys[usage] = shifted
        sendKeyboardReport()
        publishKeyboard()
        if (stickyModifiers and HidModifier.SHIFT != 0) {
            // Shift is one-shot: it applies to the key that was just pressed and
            // is then cleared for the following report.
            stickyModifiers = stickyModifiers and HidModifier.SHIFT.inv()
            publishKeyboard()
        }
    }

    fun releaseKey(usage: Int) {
        if (pressedKeys.remove(usage) == null) return
        sendKeyboardReport()
        publishKeyboard()
    }

    fun tapKey(usage: Int, shifted: Boolean = false) {
        pressKey(usage, shifted)
        releaseKey(usage)
    }

    /** Shortcut used by the media screen for PageUp/PageDown/F5/Esc etc. */
    fun tapShortcut(usage: Int, modifiers: Int = 0) {
        hid.sendKeyboard(modifiers, intArrayOf(usage))
        hid.sendKeyboard(0, intArrayOf())
    }

    fun releaseAllKeys() {
        pressedKeys.clear()
        stickyModifiers = 0
        hid.sendKeyboard(0, intArrayOf())
        publishKeyboard()
    }

    private fun sendKeyboardReport() {
        val shifted = pressedKeys.values.any { it }
        val modifiers = stickyModifiers or (if (shifted) HidModifier.SHIFT else 0)
        hid.sendKeyboard(modifiers, pressedKeys.keys.toIntArray())
    }

    private fun publishKeyboard() {
        _keyboard.value = KeyboardUiState(
            modifiers = stickyModifiers,
            pressed = pressedKeys.keys.toSet()
        )
    }

    // ------------------------------------------------------------------ mouse

    private var mouseButtons = 0
    private val _dragLocked = MutableStateFlow(false)
    val dragLocked: StateFlow<Boolean> = _dragLocked.asStateFlow()

    fun mouseMove(dx: Int, dy: Int) {
        if (dx == 0 && dy == 0) return
        // The controller merges movement into at most one report per pump tick,
        // so large deltas must not be split into a burst here.
        hid.sendMouse(mouseButtons, dx, dy, 0)
    }

    fun mouseScroll(steps: Int) {
        if (steps == 0) return
        hid.sendMouse(mouseButtons, 0, 0, steps)
    }

    fun setMouseButton(button: Int, down: Boolean) {
        mouseButtons = if (down) {
            mouseButtons or button
        } else {
            mouseButtons and button.inv()
        }
        hid.sendMouse(mouseButtons, 0, 0, 0)
    }

    fun click(button: Int = MouseButton.LEFT) {
        setMouseButton(button, true)
        setMouseButton(button, false)
    }

    fun toggleDragLock(): Boolean {
        val next = !_dragLocked.value
        _dragLocked.value = next
        setMouseButton(MouseButton.LEFT, next)
        return next
    }

    fun isDragLocked(): Boolean = _dragLocked.value

    fun releaseMouse() {
        _dragLocked.value = false
        if (mouseButtons != 0) {
            mouseButtons = 0
            hid.sendMouse(0, 0, 0, 0)
        }
    }

    // ---------------------------------------------------------------- gamepad

    private val _gamepad = MutableStateFlow(GamepadState())
    val gamepad: StateFlow<GamepadState> = _gamepad.asStateFlow()

    fun setGamepadButton(button: Int, down: Boolean) {
        updateGamepad { current ->
            current.copy(
                buttons = if (down) current.buttons or button else current.buttons and button.inv()
            )
        }
    }

    fun setLeftStick(x: Float, y: Float) {
        updateGamepad { it.copy(leftX = axis(x), leftY = axis(y)) }
    }

    fun setRightStick(x: Float, y: Float) {
        updateGamepad { it.copy(rightX = axis(x), rightY = axis(y)) }
    }

    fun setTrigger(left: Boolean, value: Int) {
        updateGamepad {
            if (left) it.copy(leftTrigger = value.coerceIn(0, 255))
            else it.copy(rightTrigger = value.coerceIn(0, 255))
        }
    }

    fun setHat(hat: Int) {
        updateGamepad { it.copy(hat = hat) }
    }

    fun releaseGamepad() {
        val neutral = GamepadState()
        if (_gamepad.value != neutral) {
            _gamepad.value = neutral
            hid.sendGamepad(neutral)
        }
    }

    private fun updateGamepad(transform: (GamepadState) -> GamepadState) {
        val next = transform(_gamepad.value)
        if (next == _gamepad.value) return
        _gamepad.value = next
        hid.sendGamepad(next)
    }

    private fun axis(value: Float): Int =
        ((value.coerceIn(-1f, 1f) + 1f) * 127.5f).roundToInt().coerceIn(0, 255)

    // ------------------------------------------------------------------ media

    fun mediaDown(usage: Int) {
        hid.sendConsumer(usage)
    }

    fun mediaUp() {
        hid.sendConsumer(0)
    }

    fun mediaTap(usage: Int) {
        hid.sendConsumer(usage)
        hid.sendConsumer(0)
    }

    // ------------------------------------------------------------- connection

    fun startHid() = hid.start()

    fun refreshBluetooth() = hid.refresh()

    fun restartHid() = hid.restart()

    fun connect(device: BluetoothDevice): Boolean = hid.connect(device)

    fun disconnect(): Boolean = hid.disconnect()

    fun bondedDevices(): List<BluetoothDevice> = hid.bondedDevices()

    fun adapterName(): String? = hid.adapterName()

    fun releaseAll() {
        releaseAllKeys()
        releaseMouse()
        releaseGamepad()
        hid.sendConsumer(0)
    }

    override fun onCleared() {
        releaseAll()
        hid.close()
    }

    companion object {
        /** Convenience for the media screen's presentation shortcuts. */
        val PRESENTATION_KEYS = listOf(
            HidKey.PAGE_UP,
            HidKey.PAGE_DOWN,
            HidKey.F5,
            HidKey.ESCAPE
        )
    }
}
