package com.flooferland.showbiz.types.math

import net.minecraft.util.*
import java.awt.Color

/** RGB color */
data class Color3(var r: Int = 0, var g: Int = 0, var b: Int = 0) {
    fun pack(): Int = FastColor.ARGB32.color(r, g, b)
    fun safe() = Color3(
        r.coerceIn(0, 255),
        g.coerceIn(0, 255),
        b.coerceIn(0, 255)
    )
    fun toHSB(): Color3h {
        val hsb = Color.RGBtoHSB(r, g, b, null)
        return Color3h(hsb[0], hsb[1], hsb[2])
    }
    fun toLinear() = Color3l(
        ColorMath.srgbToLinear(r),
        ColorMath.srgbToLinear(g),
        ColorMath.srgbToLinear(b)
    )

    fun saturate(value: Float): Color3 {
        val color = toHSB()
        color.s *= value
        color.s = color.s.coerceIn(0f, 1f)
        return color.toRGB()
    }

    operator fun plusAssign(o: Color3) { r += o.r; g += o.g; b += o.b }
    operator fun divAssign(i: Int) { r /= i; g /= i; b /= i }

    companion object {
        val ZERO get() = Color3()
        fun fromPacked(packed: Int) = Color3(
            FastColor.ARGB32.red(packed),
            FastColor.ARGB32.green(packed),
            FastColor.ARGB32.blue(packed)
        )
    }
}