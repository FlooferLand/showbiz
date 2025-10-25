package com.flooferland.showbiz.utils

import com.flooferland.showbiz.Showbiz
import net.minecraft.resources.*

/** Creates a [ResourceLocation] using the [Showbiz.MOD_ID] namespace */
fun rl(path: String): ResourceLocation {
    return ResourceLocation.fromNamespaceAndPath(Showbiz.MOD_ID, path)
}

/** Creates a [ResourceLocation] using the vanilla Minecraft namespace */
fun rlVanilla(path: String): ResourceLocation {
    return ResourceLocation.withDefaultNamespace(path)
}

/** Creates a [ResourceLocation] using a custom namespace */
fun rlCustom(namespace: String, path: String): ResourceLocation {
    return ResourceLocation.fromNamespaceAndPath(namespace, path)
}

fun <E> MutableList<E>.copy(): MutableList<E> {
    return ArrayList(this)
}

fun lerp(a: Double, b: Double, t: Double): Double {
    return a * (1.0 - t) + b * t
}
fun lerp(a: Float, b: Float, t: Float): Float {
    return a * (1.0f - t) + b * t
}
