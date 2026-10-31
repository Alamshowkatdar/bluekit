package com.alamshowkat.bluekit.hid

/** USB HID keyboard/keypad usage codes (usage page 0x07). */
object HidKey {
    const val A = 0x04
    const val B = 0x05
    const val C = 0x06
    const val D = 0x07
    const val E = 0x08
    const val F = 0x09
    const val G = 0x0A
    const val H = 0x0B
    const val I = 0x0C
    const val J = 0x0D
    const val K = 0x0E
    const val L = 0x0F
    const val M = 0x10
    const val N = 0x11
    const val O = 0x12
    const val P = 0x13
    const val Q = 0x14
    const val R = 0x15
    const val S = 0x16
    const val T = 0x17
    const val U = 0x18
    const val V = 0x19
    const val W = 0x1A
    const val X = 0x1B
    const val Y = 0x1C
    const val Z = 0x1D

    const val NUM_1 = 0x1E
    const val NUM_2 = 0x1F
    const val NUM_3 = 0x20
    const val NUM_4 = 0x21
    const val NUM_5 = 0x22
    const val NUM_6 = 0x23
    const val NUM_7 = 0x24
    const val NUM_8 = 0x25
    const val NUM_9 = 0x26
    const val NUM_0 = 0x27

    const val ENTER = 0x28
    const val ESCAPE = 0x29
    const val BACKSPACE = 0x2A
    const val TAB = 0x2B
    const val SPACE = 0x2C
    const val MINUS = 0x2D
    const val EQUALS = 0x2E
    const val LEFT_BRACKET = 0x2F
    const val RIGHT_BRACKET = 0x30
    const val BACKSLASH = 0x31
    const val SEMICOLON = 0x33
    const val APOSTROPHE = 0x34
    const val GRAVE = 0x35
    const val COMMA = 0x36
    const val PERIOD = 0x37
    const val SLASH = 0x38
    const val CAPS_LOCK = 0x39

    const val F1 = 0x3A
    const val F2 = 0x3B
    const val F3 = 0x3C
    const val F4 = 0x3D
    const val F5 = 0x3E
    const val F6 = 0x3F
    const val F7 = 0x40
    const val F8 = 0x41
    const val F9 = 0x42
    const val F10 = 0x43
    const val F11 = 0x44
    const val F12 = 0x45

    const val PRINT_SCREEN = 0x46
    const val SCROLL_LOCK = 0x47
    const val PAUSE = 0x48
    const val INSERT = 0x49
    const val HOME = 0x4A
    const val PAGE_UP = 0x4B
    const val DELETE = 0x4C
    const val END = 0x4D
    const val PAGE_DOWN = 0x4E
    const val RIGHT = 0x4F
    const val LEFT = 0x50
    const val DOWN = 0x51
    const val UP = 0x52
}

/** Bits in the keyboard report's modifier byte. */
object HidModifier {
    const val LEFT_CTRL = 0x01
    const val LEFT_SHIFT = 0x02
    const val LEFT_ALT = 0x04
    const val LEFT_GUI = 0x08
    const val RIGHT_CTRL = 0x10
    const val RIGHT_SHIFT = 0x20
    const val RIGHT_ALT = 0x40
    const val RIGHT_GUI = 0x80

    const val SHIFT = LEFT_SHIFT
    const val CTRL = LEFT_CTRL
    const val ALT = LEFT_ALT
    const val GUI = LEFT_GUI
}

/** Consumer page usages (usage page 0x0C). */
object ConsumerUsage {
    const val PLAY_PAUSE = 0x00CD
    const val STOP = 0x00B7
    const val NEXT_TRACK = 0x00B5
    const val PREVIOUS_TRACK = 0x00B6
    const val FAST_FORWARD = 0x00B3
    const val REWIND = 0x00B4
    const val VOLUME_UP = 0x00E9
    const val VOLUME_DOWN = 0x00EA
    const val MUTE = 0x00E2
    const val BRIGHTNESS_UP = 0x006F
    const val BRIGHTNESS_DOWN = 0x0070
    const val AC_HOME = 0x0223
    const val AC_BACK = 0x0224
    const val AC_FORWARD = 0x0225
}

/** Mouse button bits used by the HID mouse report. */
object MouseButton {
    const val LEFT = 0x01
    const val RIGHT = 0x02
    const val MIDDLE = 0x04
}

/** Gamepad button bits used by the HID gamepad report. */
object GamepadButton {
    const val A = 1 shl 0
    const val B = 1 shl 1
    const val X = 1 shl 2
    const val Y = 1 shl 3
    const val LEFT_BUMPER = 1 shl 4
    const val RIGHT_BUMPER = 1 shl 5
    const val BACK = 1 shl 6
    const val START = 1 shl 7
    const val GUIDE = 1 shl 8
    const val LEFT_STICK = 1 shl 9
    const val RIGHT_STICK = 1 shl 10
}

/**
 * Full gamepad report state. Analog values use the HID range 0..255 with 128
 * as the centre; the hat uses 0..7 clockwise from north, and 8 for neutral.
 */
data class GamepadState(
    val buttons: Int = 0,
    val hat: Int = HAT_NEUTRAL,
    val leftX: Int = AXIS_CENTER,
    val leftY: Int = AXIS_CENTER,
    val leftTrigger: Int = 0,
    val rightTrigger: Int = 0,
    val rightX: Int = AXIS_CENTER,
    val rightY: Int = AXIS_CENTER
) {
    fun encode(): ByteArray = byteArrayOf(
        (buttons and 0xFF).toByte(),
        ((buttons ushr 8) and 0xFF).toByte(),
        (if (hat in 0..7) hat else HAT_NEUTRAL).toByte(),
        leftX.coerceIn(0, 255).toByte(),
        leftY.coerceIn(0, 255).toByte(),
        leftTrigger.coerceIn(0, 255).toByte(),
        rightTrigger.coerceIn(0, 255).toByte(),
        rightX.coerceIn(0, 255).toByte(),
        rightY.coerceIn(0, 255).toByte()
    )

    companion object {
        const val AXIS_CENTER = 128
        const val HAT_NEUTRAL = 8
        const val HAT_NORTH = 0
        const val HAT_NE = 1
        const val HAT_EAST = 2
        const val HAT_SE = 3
        const val HAT_SOUTH = 4
        const val HAT_SW = 5
        const val HAT_WEST = 6
        const val HAT_NW = 7
    }
}

object KeyboardReport {
    /** 8-byte keyboard report: modifiers, reserved byte, then up to six keys. */
    fun encode(modifiers: Int, keys: IntArray): ByteArray {
        val report = ByteArray(8)
        report[0] = modifiers.toByte()
        var index = 0
        for (key in keys) {
            if (key <= 0) continue
            report[2 + index] = key.toByte()
            index++
            if (index == 6) break
        }
        return report
    }
}

object MouseReport {
    /** 4-byte mouse report: button bits, dx, dy, wheel. */
    fun encode(buttons: Int, dx: Int, dy: Int, wheel: Int): ByteArray = byteArrayOf(
        (buttons and 0xFF).toByte(),
        dx.coerceIn(-127, 127).toByte(),
        dy.coerceIn(-127, 127).toByte(),
        wheel.coerceIn(-127, 127).toByte()
    )
}

object ConsumerReport {
    /** 2-byte little-endian consumer usage; zero releases the key. */
    fun encode(usage: Int): ByteArray = byteArrayOf(
        (usage and 0xFF).toByte(),
        ((usage ushr 8) and 0xFF).toByte()
    )
}
