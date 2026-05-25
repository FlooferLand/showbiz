package com.flooferland.showbiz.utils

import net.minecraft.util.*

/** Because Minecraft's colour classes suck */
object ColorUtils {
    fun toStringRgb(packedColor: Int) =
        "${FastColor.ARGB32.red(packedColor)}, ${FastColor.ARGB32.green(packedColor)}, ${FastColor.ARGB32.blue(packedColor)}"
}