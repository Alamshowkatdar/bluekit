package com.alamshowkat.bluekit.hid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportPumpTest {

    private val mouse = byteArrayOf(0, 1, 0, 0)

    @Test
    fun identicalRelativeMovementIsNeverDeduplicated() {
        val dedupe = ReportDedupe()

        assertTrue(dedupe.shouldSend(HidDescriptor.MOUSE_REPORT_ID, mouse, carriesMotion = true))
        dedupe.remember(HidDescriptor.MOUSE_REPORT_ID, mouse)

        // The same delta twice is two real movements. Dropping the second one
        // is what made steady drags stutter and fall behind.
        assertTrue(dedupe.shouldSend(HidDescriptor.MOUSE_REPORT_ID, mouse, carriesMotion = true))
    }

    @Test
    fun identicalStateReportsAreDeduplicated() {
        val dedupe = ReportDedupe()
        val report = KeyboardReport.encode(0, intArrayOf())

        assertTrue(dedupe.shouldSend(HidDescriptor.KEYBOARD_REPORT_ID, report, false))
        dedupe.remember(HidDescriptor.KEYBOARD_REPORT_ID, report)

        assertFalse(dedupe.shouldSend(HidDescriptor.KEYBOARD_REPORT_ID, report, false))
    }

    @Test
    fun changedStateIsSentAgain() {
        val dedupe = ReportDedupe()
        val released = KeyboardReport.encode(0, intArrayOf())
        val pressed = KeyboardReport.encode(0, intArrayOf(HidKey.A))

        dedupe.remember(HidDescriptor.KEYBOARD_REPORT_ID, released)

        assertTrue(dedupe.shouldSend(HidDescriptor.KEYBOARD_REPORT_ID, pressed, false))
    }

    @Test
    fun resetForgetsEverything() {
        val dedupe = ReportDedupe()
        val report = GamepadState().encode()
        dedupe.remember(HidDescriptor.GAMEPAD_REPORT_ID, report)

        dedupe.clear()

        assertTrue(dedupe.shouldSend(HidDescriptor.GAMEPAD_REPORT_ID, report, false))
    }

    @Test
    fun aReservedStepIsRemovedFromThePendingMovement() {
        val motion = MouseAccumulator()
        motion.add(5, -3, 0)

        val step = motion.takeStep()

        assertEquals(Triple(5, -3, 0), step)
        assertFalse(motion.hasPending)
    }

    @Test
    fun aRejectedStepGoesBackIntoThePendingMovement() {
        val motion = MouseAccumulator()
        motion.add(9, 0, 0)
        val reserved = motion.takeStep()
        motion.add(2, 0, 0)

        // A rejected report must not lose the movement it carried.
        motion.restore(reserved)

        assertEquals(Triple(11, 0, 0), motion.takeStep())
    }
}
