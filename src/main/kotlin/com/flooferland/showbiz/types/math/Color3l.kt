package com.flooferland.showbiz.types.math

/** Linear RGB color */
data class Color3l(var r: Float = 0f, var g: Float = 0f, var b: Float = 0f) {
    val luminance: Double get() = (0.2126 * r) + (0.7152 * g) + (0.0722 * b)
    fun toSRGB() = Color3(
        ColorMath.linearToSrgb(r),
        ColorMath.linearToSrgb(g),
        ColorMath.linearToSrgb(b)
    )

    operator fun plusAssign(o: Color3l) { r += o.r; g += o.g; b += o.b }
    operator fun divAssign(i: Int) { r /= i; g /= i; b /= i }

    companion object {
        val ZERO get() = Color3l()
    }
}