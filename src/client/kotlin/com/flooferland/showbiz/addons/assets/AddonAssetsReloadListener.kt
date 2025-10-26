package com.flooferland.showbiz.addons.assets

import com.akuleshov7.ktoml.Toml
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.ShowbizClient
import com.flooferland.showbiz.utils.ResourcePath
import com.flooferland.showbiz.utils.rl
import com.flooferland.showbiz.utils.rlCustom
import com.flooferland.showbiz.utils.toPath
import com.google.gson.JsonObject
import kotlinx.serialization.decodeFromString
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
import net.minecraft.resources.*
import net.minecraft.server.packs.*
import net.minecraft.server.packs.resources.*
import net.minecraft.util.*
import net.minecraft.util.profiling.*
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.loading.json.raw.Model
import software.bernie.geckolib.loading.json.typeadapter.KeyFramesAdapter
import software.bernie.geckolib.loading.`object`.BakedAnimations
import software.bernie.geckolib.loading.`object`.BakedModelFactory
import software.bernie.geckolib.loading.`object`.GeometryTree

// TODO: Optimize this class loads.
//       Currently loading and storing a ton of unnecessary models, and is not async

data class LoadedAssets(
    var addons: List<AddonAssets> = mutableListOf(),
    val models: MutableMap<ResourceLocation, BakedGeoModel> = mutableMapOf(),
    val animations: MutableMap<ResourceLocation, BakedAnimations> = mutableMapOf()
)


@Environment(EnvType.CLIENT)
object AddonAssetsReloadListener : SimplePreparableReloadListener<LoadedAssets>(), IdentifiableResourceReloadListener {
    const val ASSETS_NAME = "assets.toml"
    const val BITMAP_NAME = "bitmap.toml"

    override fun getFabricId(): ResourceLocation = rl("assets")

    override fun prepare(manager: ResourceManager, profiler: ProfilerFiller): LoadedAssets {
        val out = LoadedAssets()
        val addons = mutableListOf<AddonAssets>()
        for (pack in manager.listPacks()) {
            val packId = pack.packId()
            if (packId.startsWith("fabric-")) continue
            fun err(msg: String, throwable: Throwable? = null) =
                Showbiz.log.error("Addon '$packId' (resource pack): $msg\n", throwable)
            fun getResAsString(location: ResourceLocation) =
                pack.getResource(PackType.CLIENT_RESOURCES, location)?.get()?.readAllBytes()?.decodeToString()
            fun getResAsString(path: String) =
                getResAsString(rlCustom(packId, path))

            // Finding assets
            data class BotLoadAssets(var rootPath: ResourcePath? = null, var model: ResourceLocation? = null, var animations: ResourceLocation? = null)
            val botAssets = mutableMapOf<String, BotLoadAssets>()
            val onList = PackResources.ResourceOutput() { location, stream ->
                val botsIntro = "${Showbiz.MOD_ID}/bots/"
                if (!location.path.startsWith(botsIntro)) return@ResourceOutput
                val botId = location.path.replace(botsIntro, "").split('/').firstOrNull() ?: return@ResourceOutput

                // Models
                if (location.path.endsWith(".geo.json")) {
                    val assets = botAssets.getOrPut(botId, { BotLoadAssets() })
                    assets.model = location
                    runCatching {
                        out.models.put(
                            location,
                            loadBakedModel(location, stream.get()!!.readAllBytes()!!.decodeToString())
                        )
                    }
                }

                // Animations
                if (location.path.endsWith(".animation.json")) {
                    val assets = botAssets.getOrPut(botId, { BotLoadAssets() })
                    assets.animations = location
                    runCatching {
                        out.animations.put(
                            location,
                            loadBakedAnimation(stream.get()!!.readAllBytes()!!.decodeToString())
                        )
                    }
                }

                // assets.toml
                if (location.path.endsWith("/$ASSETS_NAME")) {
                    val assets = botAssets.getOrPut(botId, { BotLoadAssets() })
                    assets.rootPath = location.toPath().parent
                }
            }
            pack.listResources(PackType.CLIENT_RESOURCES, packId, Showbiz.MOD_ID, onList)

            // Finding toml def files
            val bots = mutableMapOf<String, AddonBot>()
            for ((id, bot) in botAssets) {
                fun <T> getToml(filename: String, fetchToml: (String) -> T?): T? {
                    val path = bot.rootPath!!.toLocation().withSuffix("/$filename")
                    val res = getResAsString(path) ?: run { err("Failed to find '$path'"); return null }
                    val toml = runCatching { fetchToml(res) }
                    toml.exceptionOrNull()?.let { throwable -> err("Failed to parse TOML '$path'", throwable) }
                    return toml.getOrNull()
                }
                if (bot.rootPath == null || bot.model == null) {
                    err("INTERNAL: Failed to load root path or model for '${id}'")
                    continue
                }

                val assets = getToml(ASSETS_NAME)
                    { Toml.decodeFromString<BotAssetsFile>(it) } ?: continue
                val bitmap = getToml(BITMAP_NAME)
                    { Toml.decodeFromString<BotBitmapFile>(it) } ?: continue
                bots[id] = AddonBot(assets, bitmap, resPath = bot.rootPath!!, model = bot.model!!, animations = bot.animations)
            }

            if (bots.isNotEmpty()) {
                val assets = AddonAssets(
                    id = packId,
                    bots = bots
                )
                addons.add(assets)
            }
        }
        out.addons = addons
        return out
    }

    override fun apply(loaded: LoadedAssets, manager: ResourceManager, profiler: ProfilerFiller) {
        ShowbizClient.addons = loaded.addons

        // Collecting bots
        val bots = mutableMapOf<String, AddonBot>()
        for (addon in loaded.addons) {
            Showbiz.log.info("Loaded addon '${addon.id}' (client-side)")
            for ((id, bot) in addon.bots) {
                bots[id] = bot
            }
        }
        ShowbizClient.bots = bots

        // TODO: Add these to GeckoLib cache, and compare the existing assets with the loaded ones to tell what to remove/add to the Gecko cache
        ShowbizClient.models = loaded.models
        ShowbizClient.animations = loaded.animations
    }

    fun loadBakedModel(location: ResourceLocation, json: String): BakedGeoModel {
        val model = GsonHelper.fromJson(KeyFramesAdapter.GEO_GSON, json, Model::class.java)
        val geo = GeometryTree.fromModel(model)
        return BakedModelFactory.getForNamespace(location.namespace).constructGeoModel(geo)
    }
    fun loadBakedAnimation(json: String): BakedAnimations {
        val json = GsonHelper.fromJson(KeyFramesAdapter.GEO_GSON, json, JsonObject::class.java)
        return KeyFramesAdapter.GEO_GSON.fromJson(
            GsonHelper.getAsJsonObject(json, "animations"),
            BakedAnimations::class.java
        );
    }
}