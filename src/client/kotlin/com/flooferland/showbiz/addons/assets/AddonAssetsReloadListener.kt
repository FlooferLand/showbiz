package com.flooferland.showbiz.addons.assets

import com.akuleshov7.ktoml.Toml
import com.flooferland.bizlib.bits.BitsMap
import com.flooferland.bizlib.bits.BotBitmapFile
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.ShowbizClient
import com.flooferland.showbiz.addons.data.BotModelData
import com.flooferland.showbiz.types.ResourcePath
import com.flooferland.showbiz.types.Vec3f
import com.flooferland.showbiz.types.toPath
import com.flooferland.showbiz.utils.Extensions.getAllBones
import com.flooferland.showbiz.utils.rl
import com.flooferland.showbiz.utils.rlCustom
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
import com.flooferland.showbiz.models.BaseBotModel
import com.flooferland.showbiz.utils.ShowbizUtils
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
    const val ASSETS_TOML_NAME = "assets.toml"
    const val BITMAP_BITS_NAME = "bitmap.bits"

    override fun getFabricId(): ResourceLocation = rl("assets")

    override fun prepare(manager: ResourceManager, profiler: ProfilerFiller): LoadedAssets {
        val out = LoadedAssets()
        val addons = mutableListOf<AddonAssets>()
        for (pack in manager.listPacks()) {
            if (pack.packId().startsWith("fabric-")) continue
            for (namespace in pack.getNamespaces(PackType.CLIENT_RESOURCES)) {
                fun err(msg: String, throwable: Throwable? = null) =
                    Showbiz.log.error("Addon '$namespace' (resource pack): $msg\n", throwable)
                fun getResAsString(location: ResourceLocation) =
                    pack.getResource(PackType.CLIENT_RESOURCES, location)?.get()?.readAllBytes()?.decodeToString()

                if (pack.getResource(PackType.CLIENT_RESOURCES, rlCustom(namespace, "showbiz")) != null) {
                    Showbiz.log.info("Attempting to load addon '${namespace}'")
                }

                // Finding assets
                data class BotLoadAssets(
                    var rootPath: ResourcePath? = null,
                    var model: ResourceLocation? = null,
                    var animations: ResourceLocation? = null
                )

                val botAssets = mutableMapOf<String, BotLoadAssets>()
                val onList = PackResources.ResourceOutput() { location, stream ->
                    val botsIntro = "${Showbiz.MOD_ID}/bots/"
                    if (!location.path.startsWith(botsIntro)) return@ResourceOutput
                    val botId = location.path.replace(botsIntro, "").split('/').firstOrNull() ?: return@ResourceOutput
                    val string = stream.get()?.readAllBytes()?.decodeToString()

                    // Models
                    if (location.path.endsWith(".geo.json")) {
                        val assets = botAssets.getOrPut(botId) { BotLoadAssets() }
                        assets.model = location
                        val model = string?.let { ShowbizUtils.loadBakedModel(location, it) }
                        if (model != null) out.models[location] = model
                    }

                    // Animations
                    if (location.path.endsWith(".animation.json")) {
                        val assets = botAssets.getOrPut(botId, { BotLoadAssets() })
                        assets.animations = location
                        val anim = string?.let { ShowbizUtils.loadBakedAnimation(location, it) }
                        if (anim != null) out.animations[location] = anim
                    }

                    // assets.toml
                    if (location.path.endsWith("/$ASSETS_TOML_NAME")) {
                        val assets = botAssets.getOrPut(botId, { BotLoadAssets() })
                        assets.rootPath = location.toPath().parent
                    }
                }
                pack.listResources(PackType.CLIENT_RESOURCES, namespace, Showbiz.MOD_ID, onList)

                // Finding toml def files
                val bots = mutableMapOf<String, AddonBot>()
                for ((id, bot) in botAssets) {
                    if (bot.rootPath == null || bot.model == null) {
                        err("INTERNAL: Failed to load root path or model for '${id}'")
                        continue
                    }

                    fun <T> getToml(filename: String, fetchToml: (String) -> T?): T? {
                        val path = bot.rootPath!!.toLocation().withSuffix("/$filename")
                        val res = getResAsString(path) ?: run { err("Failed to find '$path'"); return null }
                        val toml = runCatching { fetchToml(res) }
                        toml.exceptionOrNull()?.let { throwable -> err("Failed to parse TOML '$path'", throwable) }
                        return toml.getOrNull()
                    }

                    fun getBits(filename: String): BotBitmapFile? {
                        val path = bot.rootPath!!.toLocation().withSuffix("/$filename")
                        val res = getResAsString(path) ?: run { err("Failed to find bitsmap file '$path'"); return null }
                        val bits = runCatching { BitsMap().load(res.byteInputStream()) }
                        bits.exceptionOrNull()?.let { throwable -> err("Failed to load bitsmap file '$filename' on '$path'", throwable) }
                        return bits.getOrNull()
                    }

                    val assets = getToml(ASSETS_TOML_NAME) { Toml.decodeFromString<BotAssetsFile>(it) } ?: continue
                    val bitmap = getBits(BITMAP_BITS_NAME) ?: continue
                    bots[id] = AddonBot(assets, bitmap, resPath = bot.rootPath!!, model = bot.model!!, animations = bot.animations)
                }

                if (bots.isNotEmpty()) {
                    val assets = AddonAssets(
                        id = namespace,
                        bots = bots
                    )
                    addons.add(assets)
                }
            }
        }
        out.addons = addons
        return out
    }

    override fun apply(loaded: LoadedAssets, manager: ResourceManager, profiler: ProfilerFiller) {
        ShowbizClient.addons = loaded.addons
        ShowbizClient.resetAssetErrors()

        // Collecting bots
        val bots = mutableMapOf<String, AddonBot>()
        for (addon in loaded.addons) {
            Showbiz.log.info("Loaded addon '${addon.id}' (resource pack)")
            for ((id, bot) in addon.bots) {
                bots[id] = bot
            }
        }
        ShowbizClient.bots = bots

        // TODO: Add these to GeckoLib cache, and compare the existing assets with the loaded ones to tell what to remove/add to the Gecko cache
        val models = mutableMapOf<ResourceLocation, BotModelData>()
        for ((id, model) in loaded.models) {
            val initBoneRots = mutableMapOf<String, Vec3f>()
            val initBoneMoves = mutableMapOf<String, Vec3f>()
            for (bone in model.getAllBones()) {
                initBoneRots[bone.name] = Vec3f(
                    bone.rotX,
                    bone.rotY,
                    bone.rotZ
                )
                initBoneMoves[bone.name] = Vec3f(
                    bone.posX,
                    bone.posY,
                    bone.posZ
                )
            }

            models[id] = BotModelData(
                initBoneRots = initBoneRots,
                initBoneMoves = initBoneMoves,
                bakedModel = model
            )
        }
        ShowbizClient.botModels = models
        ShowbizClient.animations = loaded.animations
    }
}