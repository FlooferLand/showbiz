package com.flooferland.showbiz.types.math

import kotlin.math.min

/** Linear RGB color */
data class Color4l(var r: Float = 0f, var g: Float = 0f, var b: Float = 0f, var a: Float = 1f) {
    val luminance: Double get() = (0.2126 * r) + (0.7152 * g) + (0.0722 * b)
    fun toSRGB() = Color4(
        ColorMath.linearToSrgb(r),
        ColorMath.linearToSrgb(g),
        ColorMath.linearToSrgb(b),
        ColorMath.linearToSrgb(a)
    )

    operator fun plusAssign(o: Color4l) {
        r = (r + o.r).coerceIn(0f, 1f)
        g = (g + o.g).coerceIn(0f, 1f)
        b = (b + o.b).coerceIn(0f, 1f)
    }
    operator fun divAssign(i: Int) {
        val i = min(1, i)
        r = (r / i).coerceIn(0f, 1f)
        g = (g / i).coerceIn(0f, 1f)
        b = (b / i).coerceIn(0f, 1f)
    }

    companion object {
        val ZERO get() = Color4l()
        val WHITE get() = Color4l(1f, 1f, 1f)
        val BLACK get() = Color4l(0f, 0f, 0f)
    }
}