package com.alamshowkat.bluekit.hid

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HidReportsTest {

    @Test
    fun keyboardReportMatchesBootLayout() {
        val report = KeyboardReport.encode(
            HidModifier.LEFT_CTRL or HidModifier.LEFT_SHIFT,
            intArrayOf(HidKey.A, HidKey.B, 0, HidKey.C, HidKey.D, HidKey.E, HidKey.F, HidKey.G)
        )

        assertEquals(8, report.size)
        assertEquals((HidModifier.LEFT_CTRL or HidModifier.LEFT_SHIFT).toByte(), report[0])
        assertEquals(0.toByte(), report[1])
        assertEquals(HidKey.A.toByte(), report[2])
        assertEquals(HidKey.B.toByte(), report[3])
        assertEquals(HidKey.C.toByte(), report[4])
        assertEquals(HidKey.D.toByte(), report[5])
        assertEquals(HidKey.E.toByte(), report[6])
        assertEquals(HidKey.F.toByte(), report[7])
    }

    @Test
    fun emptyKeyboardReportReleasesEveryKey() {
        val report = KeyboardReport.encode(0, intArrayOf())

        assertArrayEquals(ByteArray(8), report)
    }

    @Test
    fun mouseReportClampsRelativeMovement() {
        val report = MouseReport.encode(0xFF, 500, -500, 0)

        assertArrayEquals(
            byteArrayOf(
                0xFF.toByte(),
                127,
                (-127).toByte(),
                0
            ),
            report
        )
    }

    @Test
    fun consumerReportIsLittleEndian() {
        assertArrayEquals(
            byteArrayOf(0xCD.toByte(), 0x00),
            ConsumerReport.encode(ConsumerUsage.PLAY_PAUSE)
        )
        assertArrayEquals(
            byteArrayOf(0xE9.toByte(), 0x00),
            ConsumerReport.encode(ConsumerUsage.VOLUME_UP)
        )
    }

    @Test
    fun neutralGamepadReportHasCentredAxes() {
        assertArrayEquals(
            byteArrayOf(
                0, 0,
                GamepadState.HAT_NEUTRAL.toByte(),
                128.toByte(), 128.toByte(), 0, 0, 128.toByte(), 128.toByte()
            ),
            GamepadState().encode()
        )
    }

    @Test
    fun gamepadReportPacksButtonsHatAndAxes() {
        val state = GamepadState(
            buttons = GamepadButton.A or GamepadButton.GUIDE,
            hat = GamepadState.HAT_EAST,
            leftX = 0,
            leftY = 255,
            leftTrigger = 255,
            rightTrigger = 64,
            rightX = 1,
            rightY = 200
        )

        assertArrayEquals(
            byteArrayOf(
                1, 1,
                GamepadState.HAT_EAST.toByte(),
                0,
                255.toByte(),
                255.toByte(),
                64,
                1,
                200.toByte()
            ),
            state.encode()
        )
    }

    @Test
    fun gamepadAxesAreClamped() {
        val state = GamepadState(
            leftX = -20,
            leftY = 400,
            leftTrigger = -1,
            rightTrigger = 999
        ).encode()

        assertEquals(0.toByte(), state[3])
        assertEquals(255.toByte(), state[4])
        assertEquals(0.toByte(), state[5])
        assertEquals(255.toByte(), state[6])
    }

    @Test
    fun descriptorDeclaresEveryReportId() {
        val descriptor = HidDescriptor.DESCRIPTOR
        val reportIds = mutableSetOf<Int>()
        var index = 0
        while (index < descriptor.size - 1) {
            if (descriptor[index] == 0x85.toByte()) {
                reportIds += descriptor[index + 1].toInt() and 0xFF
            }
            index++
        }

        assertEquals(
            setOf(
                HidDescriptor.KEYBOARD_REPORT_ID,
                HidDescriptor.MOUSE_REPORT_ID,
                HidDescriptor.CONSUMER_REPORT_ID,
                HidDescriptor.GAMEPAD_REPORT_ID
            ),
            reportIds
        )
    }

    @Test
    fun descriptorBalancesApplicationCollections() {
        var open = 0
        var close = 0
        var index = 0
        while (index < HidDescriptor.DESCRIPTOR.size - 1) {
            if (HidDescriptor.DESCRIPTOR[index] == 0xA1.toByte() &&
                HidDescriptor.DESCRIPTOR[index + 1] == 0x01.toByte()
            ) {
                open++
            }
            if (HidDescriptor.DESCRIPTOR[index] == 0xC0.toByte()) {
                close++
            }
            index++
        }

        assertEquals(4, open)
        assertTrue(close >= open)
    }
}
