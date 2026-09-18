package com.flooferland.showbiz.types.math

import java.awt.Color

/** HSB color */
data class Color3h(var h: Float = 0f, var s: Float = 0f, var b: Float = 0f) {
    fun toRGB() = Color3.fromPacked(Color.HSBtoRGB(h, s, b))

    operator fun plusAssign(o: Color3h) { h += o.h; s += o.s; b += o.b }

    companion object {
        val ZERO get() = Color3h()
    }
}