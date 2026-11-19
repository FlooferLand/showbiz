@file:Suppress("unused")

package com.flooferland.showbiz.utils

import com.flooferland.showbiz.Showbiz
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit

/** Utility for figuring out how much time it takes to run some code */
object Task {
    val minTime get() = 5.milliseconds

    private typealias ListenFunc = (Instance?) -> Unit

    fun begin(name: String) = Instance(
        name = name,
        start = System.nanoTime()
    )

    /** Executes a block and measures how long it took to run */
    inline fun <T> measured(name: String, listener: Listener? = null, threshold: Duration = minTime, block: () -> T): T {
        if (!Showbiz.log.isDebugEnabled) return block()

        val instance = begin(name)
        try {
            listener?.set(instance)
            return block()
        } catch(t: Throwable) {
            instance.error = t
            throw t
        } finally {
            instance.log(threshold)
            listener?.clear()
        }
    }

    data class Instance(val name: String, val start: Long) {
        @Volatile var error: Throwable? = null

        fun duration(): Duration =
            (System.nanoTime() - start).nanoseconds
        fun millis(): Long =
            duration().inWholeMilliseconds
        fun secs(): Double =
            duration().toDouble(DurationUnit.SECONDS)

        fun log(threshold: Duration = minTime) {
            if (!Showbiz.log.isDebugEnabled) return
            val ms = millis()
            if (ms > threshold.inWholeMilliseconds) {
                val err = error?.let { " (Err: ${it.message})" } ?: ""
                Showbiz.log.debug("(Task) '$name' took ${ms}ms (${secs()}s)$err")
            }
        }
    }

    class Listener(private var func: ListenFunc = {}) {
        @Volatile private var active: Instance? = null
        @Volatile private var totalTasks = 0
        @Volatile private var totalTime = Duration.ZERO

        fun reset() {
            active = null
            totalTasks = 0
            totalTime = Duration.ZERO
        }

        fun listen(func: ListenFunc) {
            this.func = func
        }

        fun set(instance: Instance) {
            active = instance
            totalTasks += 1
            func(active)
        }

        fun clear() {
            active?.let { totalTime += it.duration() }
            active = null
            func(null)
        }

        fun end() {
            Showbiz.log.debug("(Task) $totalTasks tasks took ${totalTime.inWholeMilliseconds}ms (${totalTime.toDouble(DurationUnit.SECONDS)}s)")
            clear()
            reset()
        }
    }
}