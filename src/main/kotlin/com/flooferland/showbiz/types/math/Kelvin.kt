package com.flooferland.showbiz.types.math

import net.minecraft.util.*
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

object Kelvin {
    /** Thanks to https://tannerhelland.com/2012/09/18/convert-temperature-rgb-algorithm-code.html */
    fun toColor(kelvin: Int): Int {
        val temperature = kelvin / 100.0

        val red = if (temperature <= 66.0) {
            255.0
        } else {
            329.698727446 * (temperature - 60.0).pow(-0.1332047592)
        }.coerceIn(0.0, 255.0)

        val green = if (temperature <= 66.0) {
            99.4708025861 * ln(temperature) - 161.1195681661
        } else {
            288.1221695283 * (temperature - 60.0).pow(-0.0755148492)
        }.coerceIn(0.0, 255.0)

        val blue = when {
            temperature >= 66.0 -> 255.0
            temperature <= 19.0 -> 0.0
            else -> 138.5177312231 * ln(temperature - 10.0) - 305.0447927307
        }.coerceIn(0.0, 255.0)

        return FastColor.ARGB32.color(red.roundToInt(), green.roundToInt(), blue.roundToInt())
    }
}