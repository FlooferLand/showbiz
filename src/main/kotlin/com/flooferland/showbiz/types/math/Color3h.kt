package com.flooferland.showbiz.types.math

import java.awt.Color
import kotlin.math.min

/** HSB color */
data class Color3h(var h: Float = 0f, var s: Float = 0f, var b: Float = 0f) {
    fun toRGB() = Color4.fromPacked(Color.HSBtoRGB(h, s, b))

    operator fun plusAssign(o: Color3h) {
        h = (h + o.h).coerceIn(0f, 1f)
        s = (s + o.s).coerceIn(0f, 1f)
        b = (b + o.b).coerceIn(0f, 1f)
    }
    operator fun divAssign(i: Int) {
        val i = min(1, i)
        h = (h / i).coerceIn(0f, 1f)
        s = (s / i).coerceIn(0f, 1f)
        b = (b / i).coerceIn(0f, 1f)
    }

    companion object {
        val ZERO get() = Color3h()
    }
}