package com.flooferland.showbiz.addons.data

import com.akuleshov7.ktoml.Toml
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.addons.AddonId
import com.flooferland.showbiz.addons.AddonNotice
import com.flooferland.showbiz.addons.AddonNotice.Side
import com.flooferland.showbiz.addons.AddonNotices
import com.flooferland.showbiz.types.ResourceId
import com.flooferland.showbiz.utils.rl
import com.flooferland.showbiz.utils.rlCustom
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.serializer
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.IoSupplier
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import java.io.InputStream
import kotlin.jvm.optionals.getOrNull

// TODO: Move this to a more efficient async reloader

// TODO: Should move to addons being placed inside assets/showbiz instead of assets/<their_namespace>/showbiz

data class LoadedData(
    val packs: MutableMap<String, MutableSet<String>> = mutableMapOf(),
    var data: MutableMap<String, AddonData> = mutableMapOf()
)

object AddonDataReloadListener : SimplePreparableReloadListener<LoadedData>(), IdentifiableResourceReloadListener {
    const val MANIFEST_NAME = "showbiz.addon.toml"

    override fun getFabricId(): ResourceLocation = rl("data")

    override fun prepare(manager: ResourceManager, profiler: ProfilerFiller): LoadedData {
        AddonNotices.clear(AddonNotice.Side.Data)
        val deserializer = serializer<AddonManifest>()
        val addons = LoadedData()
        for (pack in manager.listPacks()) {
            if (pack.packId().startsWith("fabric-")) continue

            for (namespace in pack.getNamespaces(PackType.SERVER_DATA)) {
                val addonId: String = namespace
                fun err(msg: String, throwable: Throwable? = null) {
                    AddonNotices.add(
                        addonId,
                        AddonNotice(AddonNotice.Severity.Error, AddonNotice.Side.Data, Component.literal(msg), throwable = throwable)
                    )
                    Showbiz.log.error("Addon '$addonId' (datapack): $msg", throwable)
                }
                fun warn(msg: String) {
                    AddonNotices.add(
                        addonId,
                        AddonNotice(AddonNotice.Severity.Warning, AddonNotice.Side.Data, Component.literal(msg))
                    )
                    Showbiz.log.warn("Addon '$addonId' (datapack): $msg")
                }
                fun getRes(path: String): IoSupplier<InputStream>? =
                    pack.getResource(PackType.SERVER_DATA, rlCustom(namespace.replace("file/", ""), path))

                // Reading all the manifest data, and skipping irrelevant packs
                val manifestString = getRes(MANIFEST_NAME)
                    ?.get()?.readAllBytes()?.decodeToString()
                    ?: continue
                val manifest = runCatching { Toml.partiallyDecodeFromString(deserializer, manifestString, "addon") }
                    .getOrElse { err ->
                        err("Failed to parse $MANIFEST_NAME for addon '$addonId'", err)
                        continue
                    }
                if (addonId != manifest.id) {
                    err("Your addon's ID doesn't match its $MANIFEST_NAME manifest ID ($addonId != ${manifest.id})")
                }

                // Validating ID
                val isShowbizMod = (namespace == Showbiz.MOD_ID)
                        && (pack.knownPackInfo().getOrNull()?.let { !it.isVanilla && it.id == Showbiz.MOD_ID } ?: false)
                if (isShowbizMod) {
                    Showbiz.log.info("Found Showbiz built-in '$addonId'")
                } else {
                    if (!AddonId.checkValid(pack.packId(), manifest, ::err, ::warn))
                        continue
                }

                // Reading pack data
                val bots = run {
                    val e = getRes("${Showbiz.MOD_ID}/bots.toml")
                        ?.get()?.readAllBytes()?.decodeToString()
                        ?: run { err("Failed to find bots.toml"); continue }
                    runCatching { Toml.decodeFromString<HashMap<String, AddonBotEntry>>(e) }
                        .getOrElse { err ->
                            err("Failed to parse bots.toml for addon '$addonId'", err)
                            continue
                        }
                        .mapKeys { (key, _) -> ResourceId(addonId, key) }
                }

                // Adding the addon to the addon adding adder
                addons.packs.getOrPut(pack.packId()) { mutableSetOf() } += addonId
                addons.data[addonId] = AddonData(manifest, bots)
            }
        }
        return addons
    }

    override fun apply(loaded: LoadedData, manager: ResourceManager, profiler: ProfilerFiller) {
        Showbiz.addons = loaded.data

        val bots = mutableMapOf<ResourceId, AddonBotEntry>()
        for ((addonId, addon) in loaded.data) {
            Showbiz.log.info("Loaded addon '${addonId}' (data pack)")
            for ((id, bot) in addon.bots) {
                bots[id] = bot
            }
        }
        Showbiz.bots = bots

        // Documenting addon packs
        Showbiz.addonPacks = loaded.packs
        
        // Displaying the notice screen
        if (AddonNotices.shouldShowScreen(Side.Data)) {
            AddonNotices.showScreen()
        }
    }
}