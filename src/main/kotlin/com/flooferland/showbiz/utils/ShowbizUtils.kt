package com.flooferland.showbiz.utils

import net.minecraft.resources.*
import net.minecraft.util.*
import com.flooferland.showbiz.Showbiz
import com.google.gson.JsonObject
import software.bernie.geckolib.loading.json.raw.Model
import software.bernie.geckolib.loading.json.typeadapter.KeyFramesAdapter
import software.bernie.geckolib.loading.`object`.BakedAnimations
import software.bernie.geckolib.loading.`object`.BakedModelFactory
import software.bernie.geckolib.loading.`object`.GeometryTree

object ShowbizUtils {
    fun loadBakedModel(location: ResourceLocation, json: String) = runCatching {
        val model = GsonHelper.fromJson(KeyFramesAdapter.GEO_GSON, json, Model::class.java)
        val geo = GeometryTree.fromModel(model)
        BakedModelFactory.getForNamespace(location.namespace).constructGeoModel(geo)
    }.onFailure { Showbiz.log.error("Failed to load GeckoLib model '${location}'", it) }.getOrNull()
    fun loadBakedAnimation(location: ResourceLocation, json: String) = runCatching {
        val json = GsonHelper.fromJson(KeyFramesAdapter.GEO_GSON, json, JsonObject::class.java)
        KeyFramesAdapter.GEO_GSON.fromJson(
            GsonHelper.getAsJsonObject(json, "animations"),
            BakedAnimations::class.java
        )
    }.onFailure { Showbiz.log.error("Failed to load GeckoLib animation '${location}'", it) }.getOrNull()
}