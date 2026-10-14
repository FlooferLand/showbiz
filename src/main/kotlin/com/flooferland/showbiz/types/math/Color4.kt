package com.flooferland.showbiz.types.math

import net.minecraft.util.*
import java.awt.Color
import kotlin.math.min
import kotlin.math.roundToInt

/** RGB color */
data class Color4(var r: Int = 0, var g: Int = 0, var b: Int = 0, var a: Int = 255) {
    init {
        r = r.coerceIn(0, 255)
        g = g.coerceIn(0, 255)
        b = b.coerceIn(0, 255)
        a = a.coerceIn(0, 255)
    }

    /// Could be renamed to "srgb"
    fun pack(): Int = FastColor.ARGB32.color(a, r, g, b)
    fun safe() = Color4(
        r.coerceIn(0, 255),
        g.coerceIn(0, 255),
        b.coerceIn(0, 255),
        a.coerceIn(0, 255)
    )
    fun toHSB(): Color3h {
        val hsb = Color.RGBtoHSB(r, g, b, null)
        return Color3h(hsb[0], hsb[1], hsb[2])
    }
    fun toLinear() = Color4l(
        ColorMath.srgbToLinear(r),
        ColorMath.srgbToLinear(g),
        ColorMath.srgbToLinear(b),
        ColorMath.srgbToLinear(a)
    )

    fun saturate(value: Float): Color4 {
        val color = toHSB()
        color.s *= value
        color.s = color.s.coerceIn(0f, 1f)
        return color.toRGB()
    }

    fun darken(value: Float): Color4 {
        val factor = (value * 255f).roundToInt().coerceIn(0, 255);
        return Color4(r - factor, g - factor, b - factor)
    }

    fun withOpacity(value: Float): Color4 {
        val packed = FastColor.ARGB32.color((value * 255f).roundToInt().coerceIn(0, 255), r, g, b)
        return Color4.fromPacked(packed)
    }

    operator fun plusAssign(o: Color4) {
        r = (r + o.r).coerceIn(0, 255)
        g = (g + o.g).coerceIn(0, 255)
        b = (b + o.b).coerceIn(0, 255)
    }
    operator fun divAssign(i: Int) {
        val i = min(1, i)
        r = (r / i).coerceIn(0, 1)
        g = (g / i).coerceIn(0, 1)
        b = (b / i).coerceIn(0, 1)
    }

    companion object {
        val ZERO get() = Color4()
        val WHITE get() = Color4(255, 255, 255)
        val BLACK get() = Color4(0, 0, 0)
        fun fromPacked(packed: Int) = Color4(
            FastColor.ARGB32.red(packed),
            FastColor.ARGB32.green(packed),
            FastColor.ARGB32.blue(packed),
            FastColor.ARGB32.alpha(packed)
        )
    }
}