package com.flooferland.showbiz.utils

import com.flooferland.showbiz.Showbiz
import net.minecraft.resources.ResourceLocation

/** Creates a [ResourceLocation] using the [Showbiz.MOD_ID] mod id */
fun rl(path: String): ResourceLocation {
    return ResourceLocation.fromNamespaceAndPath(Showbiz.MOD_ID, path)
}

/** Creates a [ResourceLocation] using the vanilla Minecraft mod id */
fun rlVanilla(path: String): ResourceLocation {
    return ResourceLocation.withDefaultNamespace(path)
}
