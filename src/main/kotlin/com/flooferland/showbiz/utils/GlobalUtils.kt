package com.flooferland.showbiz.utils

import net.minecraft.network.chat.*
import net.minecraft.resources.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.Showbiz.MOD_ID
import com.flooferland.showbiz.utils.Extensions.count
import kotlin.math.roundToInt

/** Creates a [ResourceLocation] using the [MOD_ID] namespace */
fun rl(path: String): ResourceLocation {
    return ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
}

/** Creates a [ResourceLocation] using the vanilla Minecraft namespace */
fun rlVanilla(path: String): ResourceLocation {
    return ResourceLocation.withDefaultNamespace(path)
}

/** Creates a [ResourceLocation] using a custom namespace */
fun rlCustom(namespace: String, path: String): ResourceLocation {
    return ResourceLocation.fromNamespaceAndPath(namespace, path)
}

/** Creates a [ResourceLocation] from a `namespace:path` string */
fun rlString(string: String): ResourceLocation {
    return ResourceLocation.bySeparator(string, ':')
}

/**
 * Creates a translatable [net.minecraft.network.chat.Component] using the [MOD_ID] id </br>
 * TODO: Replace this with static compilation (have a ShowbizTranslations) class thats automatically generated
 */
fun tc(suffix: String, path: String, vararg params: Any): MutableComponent {
    val key = "${suffix}.${MOD_ID}.${path}"
    val component = Component.translatableWithFallback(key, "FALLBACK", *params)
    if (ShowbizEnv.isDev()) {
        if (component.string == "FALLBACK")
            Showbiz.log.error("No valid translation found for '$key'")
        val missingArgs = component.string.count("[Ljava.")
        if (missingArgs > 0)
            Showbiz.log.error("$missingArgs parameter(s) are missing/broken for '$key' (output='${component.string}')")
    }
    return component
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

fun voxelSnap(value: Float, step: Float = 16f): Float {
    val step = 1f / step
    return (value / step).roundToInt().toFloat() * step
}
