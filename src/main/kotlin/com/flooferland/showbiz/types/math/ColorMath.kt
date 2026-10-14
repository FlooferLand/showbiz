package com.flooferland.showbiz.types.math

import kotlin.math.pow
import kotlin.math.roundToInt

object ColorMath {
    fun srgbToLinear(channel: Int): Float {
        val c = channel / 255f
        return if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
    }

    fun linearToSrgb(channel: Float): Int {
        val clamped = channel.coerceIn(0f, 1f)
        val srgb = if (clamped <= 0.0031308f) {
            clamped * 12.92f
        } else {
            (1.055 * clamped.toDouble().pow(1.0 / 2.4) - 0.055).toFloat()
        }
        return (srgb.coerceIn(0f, 1f) * 255f).roundToInt()
    }
}