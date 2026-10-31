package com.alamshowkat.bluekit.hid

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Accumulates pointer movement so it can be sent as few, larger reports.
 *
 * Touchscreens sample at 120-240 Hz, which is more often than the pointer needs
 * to be updated. Movement is merged here, and at most one step per pump tick is
 * released, so a fast swipe cannot flood the Bluetooth stack.
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
     * Removes up to [limit] units per axis and returns them.
     *
     * The step is reserved for the report that is being sent. If the stack
     * rejects that report, [restore] puts it back so no part of a drag is lost.
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

    /** Puts a rejected step back in front of the movement still pending. */
    fun restore(step: Triple<Int, Int, Int>) {
        pendingX += step.first
        pendingY += step.second
        pendingWheel += step.third
    }

    fun clear() {
        pendingX = 0
        pendingY = 0
        pendingWheel = 0
    }
}

/**
 * Decides whether a report still has to be sent.
 *
 * Full-state reports (keyboard, media, gamepad) may be skipped when nothing
 * changed. Relative mouse movement must never be: two identical deltas are two
 * real movements, and dropping one makes the pointer stutter and fall behind.
 */
internal class ReportDedupe {

    private val lastSent = HashMap<Int, ByteArray>()

    fun shouldSend(reportId: Int, report: ByteArray, carriesMotion: Boolean): Boolean {
        if (carriesMotion) return true
        val previous = lastSent[reportId] ?: return true
        return !previous.contentEquals(report)
    }

    fun remember(reportId: Int, report: ByteArray) {
        lastSent[reportId] = report
    }

    fun clear() = lastSent.clear()
}

/**
 * Single-threaded HID report pipeline.
 *
 * Reports are handed to the Bluetooth stack from one dedicated thread, in
 * order. Discrete events (key presses, mouse button transitions, media keys,
 * gamepad state) are queued as-is; only pointer movement is paced and merged,
 * so a click can never be merged away and a keystroke never waits behind a
 * backlog of pointer samples.
 *
 * Movement starts at 100 Hz, which is what a real Bluetooth mouse does and is
 * smooth on both 60 Hz and 120 Hz hosts. If the stack rejects a report or takes
 * too long to accept one, the pace backs off and the movement stays queued
 * instead of being lost.
 */
internal class ReportPump(
    private val sink: (reportId: Int, report: ByteArray) -> Boolean
) {

    private companion object {
        /** How many discrete reports may wait before the oldest is dropped. */
        const val MAX_QUEUE = 16

        /** 100 Hz: smooth on a 60 Hz and a 120 Hz host, below touch sampling. */
        const val MIN_MOUSE_INTERVAL_NS = 10_000_000L

        /** Slowest pace we fall back to while the stack is struggling. */
        const val MAX_MOUSE_INTERVAL_NS = 50_000_000L

        /** A send slower than this is treated as backpressure. */
        const val SLOW_SEND_NS = 20_000_000L

        const val MOUSE_STEP = 127
    }

    private val lock = ReentrantLock()
    private val signal = lock.newCondition()
    private val queue = ArrayDeque<Report>()
    private val dedupe = ReportDedupe()
    private val motion = MouseAccumulator()

    private var buttons = 0
    private var lastMouseAt = 0L
    private var mouseIntervalNs = MIN_MOUSE_INTERVAL_NS

    @Volatile
    private var running = true

    private val worker = Thread({ loop() }, "bluekit-hid-pump").apply {
        isDaemon = true
        start()
    }

    private class Report(
        val id: Int,
        val data: ByteArray,
        val motion: Triple<Int, Int, Int>? = null,
        /**
         * Discrete state transitions are retried once when the stack rejects
         * them, so a click cannot leave the host with a button stuck down.
         */
        var retryOnReject: Boolean = false
    ) {
        val carriesMotion: Boolean
            get() = motion != null &&
                (motion.first != 0 || motion.second != 0 || motion.third != 0)
    }

    /** Queues a state report (keyboard, consumer, gamepad) in arrival order. */
    fun post(reportId: Int, report: ByteArray, coalesce: Boolean = false) {
        lock.withLock {
            if (coalesce) {
                // Only the newest state matters for these reports, so an older
                // queued one can be replaced instead of delaying the new one.
                queue.removeAll { it.id == reportId }
            }
            enqueue(Report(reportId, report, retryOnReject = true))
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
                val step = motion.takeStep(MOUSE_STEP)
                enqueue(
                    Report(
                        HidDescriptor.MOUSE_REPORT_ID,
                        MouseReport.encode(buttons, step.first, step.second, step.third),
                        step,
                        retryOnReject = true
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
            dedupe.clear()
            motion.clear()
            buttons = 0
            lastMouseAt = 0L
            mouseIntervalNs = MIN_MOUSE_INTERVAL_NS
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
            val dropped = if (droppable >= 0) {
                queue.removeAt(droppable)
            } else {
                queue.removeFirst()
            }
            // A dropped report may have reserved some movement; keep it.
            dropped.motion?.let(motion::restore)
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
                        val waitNs = mouseIntervalNs - (now - lastMouseAt)
                        if (waitNs > 0) {
                            signal.awaitNanos(waitNs)
                            null
                        } else {
                            val step = motion.takeStep(MOUSE_STEP)
                            Report(
                                HidDescriptor.MOUSE_REPORT_ID,
                                MouseReport.encode(
                                    buttons,
                                    step.first,
                                    step.second,
                                    step.third
                                ),
                                step
                            )
                        }
                    }

                    else -> {
                        signal.await()
                        null
                    }
                }
            }

            val report = next ?: continue
            val startedAt = System.nanoTime()
            val accepted = deliver(report)
            val elapsedNs = System.nanoTime() - startedAt

            lock.withLock {
                if (report.motion != null) {
                    lastMouseAt = System.nanoTime()
                    if (accepted) {
                        mouseIntervalNs =
                            (mouseIntervalNs - 1_000_000L).coerceAtLeast(MIN_MOUSE_INTERVAL_NS)
                    } else {
                        // Give the link a break; the movement stays reserved
                        // for the retry or is put back below.
                        mouseIntervalNs =
                            (mouseIntervalNs * 2).coerceAtMost(MAX_MOUSE_INTERVAL_NS)
                    }
                }
                if (accepted && elapsedNs > SLOW_SEND_NS) {
                    mouseIntervalNs =
                        (mouseIntervalNs * 2).coerceAtMost(MAX_MOUSE_INTERVAL_NS)
                }
                if (!accepted) {
                    if (report.retryOnReject) {
                        // Retry the transition once; a dead link is detected by
                        // the controller after a run of rejected reports.
                        report.retryOnReject = false
                        queue.addFirst(report)
                    } else if (report.motion != null) {
                        // Nothing was delivered, so the movement goes back to
                        // the accumulator instead of being lost.
                        motion.restore(report.motion)
                    }
                }
            }
        }
    }

    private fun deliver(report: Report): Boolean {
        if (!dedupe.shouldSend(report.id, report.data, report.carriesMotion)) {
            return true
        }
        val accepted = sink(report.id, report.data)
        if (accepted) dedupe.remember(report.id, report.data)
        return accepted
    }
}
