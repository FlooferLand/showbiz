package com.flooferland.showbiz.utils

import com.google.gson.JsonObject
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.GsonHelper
import software.bernie.geckolib.cache.`object`.BakedGeoModel
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

    fun bakeModel(location: ResourceLocation, json: String, transform: ((Model) -> Model)? = null): Result<BakedGeoModel> = runCatching {
        val parsed = GsonHelper.fromJson(KeyFramesAdapter.GEO_GSON, json, Model::class.java)
        val model = transform?.invoke(parsed) ?: parsed
        val geo = GeometryTree.fromModel(model)
        BakedModelFactory.getForNamespace(location.namespace).constructGeoModel(geo)
    }

    fun bakeAnimation(location: ResourceLocation, json: String): Result<BakedAnimations> = runCatching {
        val json = GsonHelper.fromJson(KeyFramesAdapter.GEO_GSON, json, JsonObject::class.java)
        KeyFramesAdapter.GEO_GSON.fromJson(
            GsonHelper.getAsJsonObject(json, "animations"),
            BakedAnimations::class.java
        )
    }

    fun itemTooltip(id: ResourceLocation, tooltip: MutableList<Component>) = tooltip(id.withPrefix("item."), tooltip)
    fun blockTooltip(id: ResourceLocation, tooltip: MutableList<Component>) = tooltip(id.withPrefix("block."), tooltip)
    fun tooltip(id: ResourceLocation, tooltip: MutableList<Component>) {
        val comp = Component.translatableWithFallback("tooltip.${id.namespace}.${id.path}", "")
        if (comp.string.isEmpty()) return
        for (text in comp.string.split('\n')) {
            var comp = Component.literal(text).withStyle(ChatFormatting.GRAY)
            if (text.startsWith('(') && text.endsWith(')')) {
                comp = comp.withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)
            }
            tooltip.add(comp)
        }
    }
}