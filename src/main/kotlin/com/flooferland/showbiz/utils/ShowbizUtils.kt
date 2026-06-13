package com.flooferland.showbiz.utils

import net.minecraft.*
import net.minecraft.network.chat.*
import net.minecraft.resources.*
import net.minecraft.util.*
import com.flooferland.showbiz.Showbiz
import com.google.gson.JsonObject
import net.fabricmc.loader.api.FabricLoader
import software.bernie.geckolib.loading.json.raw.Model
import software.bernie.geckolib.loading.json.typeadapter.KeyFramesAdapter
import software.bernie.geckolib.loading.`object`.BakedAnimations
import software.bernie.geckolib.loading.`object`.BakedModelFactory
import software.bernie.geckolib.loading.`object`.GeometryTree

object ShowbizUtils {
    fun isSilly() = when (System.getProperty("user.name").lowercase()) {
        "flooferland", "monsterwaill" -> true
        else -> false
    }

    fun clientHasVeil() =
        FabricLoader.getInstance()?.getModContainer("veil")?.isPresent == true

    fun hasComputerCraft() =
        FabricLoader.getInstance()?.getModContainer("computercraft")?.isPresent == true

    fun loadBakedModel(location: ResourceLocation, json: String) = runCatching {
        val model = GsonHelper.fromJson(KeyFramesAdapter.GEO_GSON, json, Model::class.java)
        val geo = GeometryTree.fromModel(model)
        BakedModelFactory.getForNamespace(location.namespace).constructGeoModel(geo)
    }.onFailure { Showbiz.log.error("Failed to load GeckoLib model '${location}'", it) }.getOrNull()

    fun itemTooltip(id: ResourceLocation, tooltip: MutableList<Component>) {
        val comp = Component.translatableWithFallback("tooltip.${id.namespace}.block.${id.path}", "")
        if (comp.string.isEmpty()) return
        for (text in comp.string.split('\n')) {
            var comp = Component.literal(text).withStyle(ChatFormatting.GRAY)
            if (text.startsWith('(') && text.endsWith(')')) {
                comp = comp.withStyle(ChatFormatting.DARK_GRAY)
            }
            tooltip.add(comp)
        }
    }

    fun loadBakedAnimation(location: ResourceLocation, json: String) = runCatching {
        val json = GsonHelper.fromJson(KeyFramesAdapter.GEO_GSON, json, JsonObject::class.java)
        KeyFramesAdapter.GEO_GSON.fromJson(
            GsonHelper.getAsJsonObject(json, "animations"),
            BakedAnimations::class.java
        )
    }.onFailure { Showbiz.log.error("Failed to load GeckoLib animation '${location}'", it) }.getOrNull()
}