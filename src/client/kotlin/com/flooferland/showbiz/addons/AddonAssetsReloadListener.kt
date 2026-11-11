package com.flooferland.showbiz.addons

import com.akuleshov7.ktoml.Toml
import com.flooferland.bizlib.bits.BitsMap
import com.flooferland.bizlib.bits.BotBitmapFile
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.ShowbizClient
import com.flooferland.showbiz.addons.AddonNotice.Side
import com.flooferland.showbiz.addons.data.BotModelData
import com.flooferland.showbiz.addons.data.PassivePlan
import com.flooferland.showbiz.types.IBot
import com.flooferland.showbiz.types.ResourceId
import com.flooferland.showbiz.types.ResourcePath
import com.flooferland.showbiz.types.math.Vec3fc
import com.flooferland.showbiz.types.physics.ChainRig
import com.flooferland.showbiz.utils.Extensions.getAllBones
import com.flooferland.showbiz.utils.Extensions.isSnakeCase
import com.flooferland.showbiz.utils.ShowbizEnv
import com.flooferland.showbiz.utils.ShowbizUtils
import com.flooferland.showbiz.utils.rl
import com.flooferland.showbiz.utils.rlCustom
import kotlinx.serialization.decodeFromString
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackResources
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.MultiPackResourceManager
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.profiling.InactiveProfiler
import net.minecraft.util.profiling.ProfilerFiller
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.cache.`object`.GeoBone
import software.bernie.geckolib.loading.json.raw.Model
import software.bernie.geckolib.loading.`object`.BakedAnimations

// TODO: Optimize this class loads.
//       Currently loading and storing a ton of unnecessary models, and is not async

// TODO: Should move to addons being placed inside assets/showbiz instead of assets/<their_namespace>/showbiz

internal data class LoadedAssets(
    val packs: MutableMap<String, MutableSet<String>> = mutableMapOf(),
    var assets: MutableMap<String, AddonAssets> = mutableMapOf(),
    val bots: MutableMap<ResourceLocation, MutableMap<String, LoadedVariant>> = mutableMapOf(),
)

data class LoadedVariant(
    val model: BakedGeoModel,
    val animation: BakedAnimations? = null,
    val animationFile: ResourceLocation? = null
)

@Environment(EnvType.CLIENT)
internal object AddonAssetsReloadListener : SimplePreparableReloadListener<LoadedAssets>(), IdentifiableResourceReloadListener {
    const val ASSETS_TOML_NAME = "assets.toml"
    const val BITMAP_BITS_NAME = "bitmap.bits"
    const val PHYSICS_TOML_NAME = "physics.toml"

    override fun getFabricId(): ResourceLocation = rl("assets")

    override fun prepare(manager: ResourceManager, profiler: ProfilerFiller): LoadedAssets {
        AddonNotices.clear(AddonNotice.Side.Assets)
        val out = LoadedAssets()
        val addons = mutableMapOf<String, AddonAssets>()
        for (pack in manager.listPacks()) {
            if (pack.packId().let { it.startsWith("fabric-") || it == "minecraft" || it == "realms" }) continue
            val devIgnoreAddons = ShowbizEnv.isDev() && pack.packId() != Showbiz.MOD_ID
            var errors = 0
            var warnings = 0

            namespace@ for (namespace in pack.getNamespaces(PackType.CLIENT_RESOURCES)) {
                val addonId = namespace
                fun err(type: AddonErr, throwable: Throwable? = null) {
                    errors += 1
                    AddonNotices.add(
                        addonId,
                        AddonNotice(AddonNotice.Severity.Error, AddonNotice.Side.Assets, type.getText(), throwable = throwable)
                    )
                    Showbiz.log.error("Addon '$addonId': ${type.getText().string}\n", throwable)
                }
                fun bitmapWarnHandler(id: String, message: BitsMap.LogMessage) {
                    warnings += 1
                    if (devIgnoreAddons) return
                    AddonNotices.add(
                        addonId,
                        AddonNotice(AddonNotice.Severity.Warning, AddonNotice.Side.Assets, rlCustom(addonId, id), message)
                    )
                    Showbiz.log.warn("Bitmap '$addonId:$id': $message")
                }
                fun getResAsString(location: ResourceLocation) =
                    pack.getResource(PackType.CLIENT_RESOURCES, location)?.get()?.readAllBytes()?.decodeToString()

                val physicsData = mutableMapOf<String, PhysicsData?>()
                fun getPhysics(botId: String): PhysicsData? {
                    if (botId in physicsData) return physicsData[botId]
                    val path = rlCustom(namespace, "${Showbiz.MOD_ID}/bots/$botId/$PHYSICS_TOML_NAME")
                    val physics = getResAsString(path)?.let { text ->
                        runCatching { PhysicsData.readOrThrow(text) }
                            .onFailure { err(AddonErr.FailedToParse(path.toString()), it) }.getOrNull()
                    }
                    physicsData[botId] = physics
                    return physics
                }
                fun chainTransform(botId: String): ((Model) -> Model)? {
                    val chains = getPhysics(botId)?.chains ?: return null
                    if (chains.isEmpty()) return null
                    val texture = pack.getResource(PackType.CLIENT_RESOURCES, rlCustom(namespace, "${Showbiz.MOD_ID}/bots/$botId/textures/$botId.png"))?.get()?.readAllBytes()
                        ?: run { err(AddonErr.ChainMissingTexture(botId)); return null }
                    return ChainRig.transform(texture, chains, botId)
                }

                if (pack.getResource(PackType.CLIENT_RESOURCES, rlCustom(namespace, "showbiz")) != null) {
                    Showbiz.log.info("Loading addon '${addonId}'")
                }

                // Finding bots
                data class FoundBot(
                    var rootPath: ResourcePath? = null,
                    var models: MutableSet<Pair<ResourceLocation, BakedGeoModel?>> = mutableSetOf(),
                    var textures: MutableSet<ResourceLocation> = mutableSetOf(),
                    var animations: MutableSet<Pair<ResourceLocation, BakedAnimations?>> = mutableSetOf()
                )

                val botsIntro = "${Showbiz.MOD_ID}/bots/"
                val foundBots = mutableMapOf<String, FoundBot>()
                val allResources = mutableSetOf<ResourceLocation>()
                val onList = PackResources.ResourceOutput() { location, stream ->
                    allResources += location

                    // Found a bot maybe..
                    if (!location.path.startsWith(botsIntro)) return@ResourceOutput
                    val botId = location.path.replace(botsIntro, "").split('/').firstOrNull() ?: return@ResourceOutput
                    foundBots.computeIfAbsent(botId) { FoundBot(rootPath = ResourcePath(location.namespace, botsIntro + botId)) }

                    // Reading whatever file was found
                    val string = stream.get()?.readAllBytes()?.decodeToString()

                    // Models
                    if (location.path.endsWith(".geo.json")) {
                        val model = string?.let { ShowbizUtils.loadBakedModel(location, it, chainTransform(botId)) }
                        foundBots[botId]?.models?.add(Pair(location, model))
                    }

                    // Textures
                    if (location.path.endsWith(".png")) {
                        foundBots[botId]?.textures?.add(location)
                    }

                    // Animations
                    if (location.path.endsWith(".animation.json")) {
                        val anim = string?.let { ShowbizUtils.loadBakedAnimation(location, it) }
                        foundBots[botId]?.animations?.add(Pair(location, anim))
                    }
                }
                pack.listResources(PackType.CLIENT_RESOURCES, namespace, Showbiz.MOD_ID, onList)

                // Finding toml def files
                val bots = mutableMapOf<String, AddonBot?>()
                for ((botId, bot) in foundBots) {
                    bots[botId] = null
                    if (bot.rootPath == null) {
                        err(AddonErr.Other("Failed to find a bot for '${botId}'"))
                        continue
                    }
                    if (!botId.isSnakeCase()) {
                        err(AddonErr.IdWrongCase(botId, botId, "bot"))
                    }

                    fun <T> getToml(filename: String, optional: Boolean = false, fetchToml: (String) -> T?): T? {
                        val path = bot.rootPath!!.toLocation().withSuffix("/$filename")
                        val res = getResAsString(path) ?: run {
                            if (!optional) err(AddonErr.FailedToFind("$path"))
                            return null
                        }
                        val toml = runCatching { fetchToml(res) }
                        toml.exceptionOrNull()?.let { throwable -> err(AddonErr.FailedToParse("$path"), throwable) }
                        return toml.getOrNull()
                    }

                    fun getBits(filename: String): BotBitmapFile? {
                        val path = bot.rootPath!!.toLocation().withSuffix("/$filename")
                        val res = getResAsString(path) ?: run { err(AddonErr.FailedToFind("$path")); return null }
                        val format = BitsMap()
                        format.warnHandler = { bitmapWarnHandler(botId, it) }
                        val bits = runCatching { format.load(res.byteInputStream()) }
                        bits.exceptionOrNull()?.let { throwable -> err(AddonErr.FailedToParse("$path"), throwable) }
                        return bits.getOrNull()
                    }

                    // Parsing assets
                    val assets = getToml(ASSETS_TOML_NAME, optional = true) { Toml.decodeFromString<BotAssetsFile>(it) }
                        ?: BotAssetsFile(mutableMapOf(IBot.DEFAULT_VARIANT to BotAssetsFile.Variant()))
                    assets.variants.computeIfAbsent(IBot.DEFAULT_VARIANT) { BotAssetsFile.Variant() }
                    for ((id, variant) in assets.variants) {
                        variant.model = variant.model ?: botId
                        variant.texture = variant.texture ?: botId
                        if (variant.animations == null) {
                            val defaultAnim = BotAssetsFile.animationLocation(bot.rootPath!!, botId)
                            if (defaultAnim in allResources) {
                                variant.animations = variant.animations ?: botId
                            }
                        }
                        if (!id.isSnakeCase()) {
                            err(AddonErr.IdWrongCase(botId, id, "variant"))
                        }
                    }

                    val variants = assets.variants.mapValues { it.value.real(bot.rootPath!!) }
                    for ((id, variant) in variants) {
                        if (bot.models.none { it.first == variant.model }) {
                            err(AddonErr.BotMissingVariant(botId, id, variant.model, "model"))
                        }
                        if (bot.textures.none { it == variant.texture }) {
                            err(AddonErr.BotMissingVariant(botId, id, variant.texture, "texture"))
                        }
                        val anims = variant.animations ?: continue
                        if (bot.animations.none { it.first == anims }) {
                            err(AddonErr.BotMissingVariant(botId, id, variant.animations, "animation file"))
                        }
                    }
                    if (assets.variants[IBot.DEFAULT_VARIANT]?.model == null) {
                        err(AddonErr.BotMissingVariant(botId, IBot.DEFAULT_VARIANT, null, "model"))
                    }
                    if (assets.variants[IBot.DEFAULT_VARIANT]?.texture == null) {
                        err(AddonErr.BotMissingVariant(botId, IBot.DEFAULT_VARIANT, null, "texture"))
                    }

                    // Parsing bits
                    val bitmap = getBits(BITMAP_BITS_NAME) ?: continue

                    // Adding the bot
                    bots[botId] = AddonBot(assets, bitmap, resPath = bot.rootPath!!, variants = variants, physics = getPhysics(botId))
                    for ((id, variant) in variants) {
                        val model = bot.models.firstOrNull { it.first == variant.model }?.second ?: continue
                        val anim = bot.animations.firstOrNull { it.first == variant.animations }?.second
                        out.bots.getOrPut(variant.model!!) { mutableMapOf() }[id] = LoadedVariant(model, anim, variant.animations)
                    }
                }

                if (bots.isNotEmpty()) {
                    bots.entries.removeIf { (botId, bot) -> botId.isEmpty() || bot == null }
                    out.packs.getOrPut(pack.packId()) { mutableSetOf() } += addonId
                    addons[addonId] = AddonAssets(
                        bots = bots.mapKeys { (key, _) -> ResourceId(addonId, key) }.mapValues { (_, v) -> v!! },
                    )
                }
            }

            if (devIgnoreAddons) {
                if (warnings > 0 || errors > 0) Showbiz.log.warn("Pack '${pack.packId()}' had $warnings warnings and $errors errors")
            }
        }
        out.assets = addons
        return out
    }

    override fun apply(loaded: LoadedAssets, manager: ResourceManager, profiler: ProfilerFiller) {
        ShowbizClient.addons = loaded.assets
        ShowbizClient.resetAssetErrors()

        // Collecting bots
        val bots = mutableMapOf<ResourceId, AddonBot>()
        for ((addonId, addon) in loaded.assets) {
            Showbiz.log.info("Loaded addon '${addonId}' (resource pack)")
            for ((id, bot) in addon.bots) {
                bots[id] = bot
            }
        }
        ShowbizClient.bots = bots

        // TODO: Add these to GeckoLib cache, and compare the existing assets with the loaded ones to tell what to remove/add to the Gecko cache
        val models = mutableMapOf<ResourceLocation, MutableMap<String, BotModelData>>()
        for ((botId, variants) in loaded.bots) {
            for ((variantId, variant) in variants) {
                val initBoneRots = mutableMapOf<String, Vec3fc>()
                val initBoneMoves = mutableMapOf<String, Vec3fc>()
                for (bone in variant.model.getAllBones()) {
                    initBoneRots[bone.name] = Vec3fc(
                        bone.rotX,
                        bone.rotY,
                        bone.rotZ
                    )
                    initBoneMoves[bone.name] = Vec3fc(
                        bone.posX,
                        bone.posY,
                        bone.posZ
                    )
                }

                // Ordered walk, so parentIndex[i] is always less than i
                val order = ArrayList<String>()
                val parents = ArrayList<Int>()
                fun walk(bone: GeoBone, parent: Int) {
                    val index = order.size
                    order += bone.name
                    parents += parent
                    bone.childBones.forEach { walk(it, index) }
                }
                variant.model.topLevelBones.forEach { walk(it, -1) }

                val model = models.getOrPut(botId) { mutableMapOf() }
                model[variantId] = BotModelData(
                    initBoneRots = initBoneRots,
                    initBoneMoves = initBoneMoves,
                    bakedModel = variant.model
                ).also {
                    it.passivePlan = PassivePlan(order.toTypedArray(), parents.toIntArray())
                    it.chainLayout = ChainRig.layout(variant.model)
                }
            }
        }

        val animations = mutableMapOf<ResourceLocation, MutableMap<String, BakedAnimations>>()
        for ((_, variants) in loaded.bots) {
            for ((variantId, variant) in variants) {
                val file = variant.animationFile ?: continue
                animations.getOrPut(file) { mutableMapOf() }[variantId] = variant.animation ?: continue
            }
        }

        ShowbizClient.botModels = models
        ShowbizClient.botAnimations = animations

        // Documenting addon packs
        for ((packId, addons) in loaded.packs) {
            Showbiz.addonPacks[packId] = addons
        }

        // Displaying the notice screen
        if (AddonNotices.shouldShowScreen(Side.Assets)) {
            AddonNotices.showScreen()
        }
    }

    fun reloadPack(packId: String) {
        val minecraft = Minecraft.getInstance()
        minecraft.resourcePackRepository.reload()

        val pack = minecraft.resourcePackRepository.getPack(packId) ?: return
        MultiPackResourceManager(PackType.CLIENT_RESOURCES, listOf(pack.open())).use { manager ->
            val loaded = prepare(manager, InactiveProfiler.INSTANCE)
            apply(loaded, manager, InactiveProfiler.INSTANCE)
            minecraft.levelRenderer.allChanged()
        }
    }
}