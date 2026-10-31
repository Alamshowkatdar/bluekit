package com.alamshowkat.bluekit.hid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MouseAccumulatorTest {

    @Test
    fun smallMovementIsSentInOneStep() {
        val motion = MouseAccumulator()
        motion.add(7, -4, 0)

        assertEquals(Triple(7, -4, 0), motion.takeStep())
        assertFalse(motion.hasPending)
    }

    @Test
    fun largeMovementIsSpreadAcrossStepsWithoutLoss() {
        val motion = MouseAccumulator()
        motion.add(400, -300, 0)

        var totalX = 0
        var totalY = 0
        var steps = 0
        while (motion.hasPending) {
            val (x, y, _) = motion.takeStep()
            assertTrue(kotlin.math.abs(x) <= 127)
            assertTrue(kotlin.math.abs(y) <= 127)
            totalX += x
            totalY += y
            steps++
        }

        assertEquals(400, totalX)
        assertEquals(-300, totalY)
        assertTrue(steps >= 4)
    }

    @Test
    fun wheelIsClampedIndependentlyOfAxes() {
        val motion = MouseAccumulator()
        motion.add(0, 0, 500)

        assertEquals(Triple(0, 0, 127), motion.takeStep())
        assertEquals(Triple(0, 0, 127), motion.takeStep())
        assertEquals(Triple(0, 0, 127), motion.takeStep())
        assertEquals(Triple(0, 0, 119), motion.takeStep())
        assertFalse(motion.hasPending)
    }

    @Test
    fun clearDropsEverythingPending() {
        val motion = MouseAccumulator()
        motion.add(50, 60, 70)

        motion.clear()

        assertFalse(motion.hasPending)
        assertEquals(Triple(0, 0, 0), motion.takeStep())
    }
}
