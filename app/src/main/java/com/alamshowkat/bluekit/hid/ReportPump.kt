package com.alamshowkat.bluekit.hid

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Accumulates pointer movement so it can be sent as few, larger reports.
 *
 * The host polls the Bluetooth interrupt channel at its own rate (often
 * 50-90 Hz). Sending one report per touch sample at 120-240 Hz therefore builds
 * an ever-growing backlog on the phone, which is what makes the pointer - and
 * everything queued behind it - feel delayed. Movement is merged instead, and
 * at most one step per pump tick is released.
 */
internal class MouseAccumulator {

    private var pendingX = 0
    private var pendingY = 0
    private var pendingWheel = 0

    val hasPending: Boolean
        get() = pendingX != 0 || pendingY != 0 || pendingWheel != 0

    fun add(dx: Int, dy: Int, wheel: Int) {
        pendingX += dx
        pendingY += dy
        pendingWheel += wheel
    }

    /**
     * Removes up to [limit] units per axis and returns them. Anything above the
     * limit stays pending for the next tick, so fast swipes are spread out
     * instead of being clamped away or sent as a burst.
     */
    fun takeStep(limit: Int = 127): Triple<Int, Int, Int> {
        val stepX = pendingX.coerceIn(-limit, limit)
        val stepY = pendingY.coerceIn(-limit, limit)
        val stepWheel = pendingWheel.coerceIn(-limit, limit)
        pendingX -= stepX
        pendingY -= stepY
        pendingWheel -= stepWheel
        return Triple(stepX, stepY, stepWheel)
    }

    fun clear() {
        pendingX = 0
        pendingY = 0
        pendingWheel = 0
    }
}

/**
 * Single-threaded HID report pipeline.
 *
 * Reports are handed to the Bluetooth stack from one dedicated thread, in
 * order. Discrete events (key presses, mouse button transitions, media keys,
 * gamepad state) are queued as-is; only pointer movement is coalesced, so a
 * click can never be merged away and a keystroke never waits behind a backlog
 * of pointer samples.
 */
internal class ReportPump(
    private val sink: (reportId: Int, report: ByteArray) -> Unit
) {

    private companion object {
        /** How many discrete reports may wait before the oldest is dropped. */
        const val MAX_QUEUE = 16

        /** 60 Hz ceiling for pointer movement, below a typical host poll rate. */
        const val MOUSE_INTERVAL_NS = 16_000_000L

        const val MOUSE_STEP = 127
    }

    private val lock = ReentrantLock()
    private val signal = lock.newCondition()
    private val queue = ArrayDeque<Report>()
    private val lastSent = HashMap<Int, ByteArray>()
    private val motion = MouseAccumulator()

    private var buttons = 0
    private var lastMouseAt = 0L

    @Volatile
    private var running = true

    private val worker = Thread({ loop() }, "bluekit-hid-pump").apply {
        isDaemon = true
        start()
    }

    private class Report(val id: Int, val data: ByteArray)

    /** Queues a state report (keyboard, consumer, gamepad) in arrival order. */
    fun post(reportId: Int, report: ByteArray, coalesce: Boolean = false) {
        lock.withLock {
            if (coalesce) {
                // Only the newest state matters for these reports, so an older
                // queued one can be replaced instead of delaying the new one.
                queue.removeAll { it.id == reportId }
            }
            enqueue(Report(reportId, report))
            signal.signalAll()
        }
    }

    /**
     * Queues pointer input. Movement is accumulated; a button transition is a
     * discrete event and is flushed immediately so clicks keep their down/up
     * pair even when they arrive in the same frame.
     */
    fun postMouse(buttons: Int, dx: Int, dy: Int, wheel: Int) {
        lock.withLock {
            if (buttons != this.buttons) {
                this.buttons = buttons
                motion.add(dx, dy, wheel)
                val (stepX, stepY, stepWheel) = motion.takeStep(MOUSE_STEP)
                enqueue(
                    Report(
                        HidDescriptor.MOUSE_REPORT_ID,
                        MouseReport.encode(buttons, stepX, stepY, stepWheel)
                    )
                )
            } else {
                motion.add(dx, dy, wheel)
            }
            signal.signalAll()
        }
    }

    /** Drops everything pending, e.g. when the host disconnects. */
    fun reset() {
        lock.withLock {
            queue.clear()
            lastSent.clear()
            motion.clear()
            buttons = 0
            signal.signalAll()
        }
    }

    fun stop() {
        running = false
        lock.withLock { signal.signalAll() }
    }

    private fun enqueue(report: Report) {
        if (queue.size >= MAX_QUEUE) {
            // Keyboard, consumer and gamepad reports are full state, so an
            // older one can be dropped safely; mouse button reports cannot.
            val droppable = queue.indexOfFirst { it.id != HidDescriptor.MOUSE_REPORT_ID }
            if (droppable >= 0) {
                queue.removeAt(droppable)
            } else {
                queue.removeFirst()
            }
        }
        queue.addLast(report)
    }

    private fun loop() {
        while (running) {
            var next: Report? = null
            lock.withLock {
                next = when {
                    queue.isNotEmpty() -> queue.removeFirst()

                    motion.hasPending -> {
                        val now = System.nanoTime()
                        val waitNs = MOUSE_INTERVAL_NS - (now - lastMouseAt)
                        if (waitNs > 0) {
                            signal.awaitNanos(waitNs)
                            null
                        } else {
                            lastMouseAt = now
                            val (stepX, stepY, stepWheel) = motion.takeStep(MOUSE_STEP)
                            Report(
                                HidDescriptor.MOUSE_REPORT_ID,
                                MouseReport.encode(buttons, stepX, stepY, stepWheel)
                            )
                        }
                    }

                    else -> {
                        signal.await()
                        null
                    }
                }
            }
            next?.let(::deliver)
        }
    }

    private fun deliver(report: Report) {
        val previous = lastSent[report.id]
        if (previous != null && previous.contentEquals(report.data)) return
        lastSent[report.id] = report.data
        sink(report.id, report.data)
    }
}
