package com.flooferland.showbiz.addons.data

import com.akuleshov7.ktoml.Toml
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.addons.AddonId
import com.flooferland.showbiz.utils.rl
import com.flooferland.showbiz.utils.rlCustom
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.serializer
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
import net.minecraft.resources.*
import net.minecraft.server.packs.*
import net.minecraft.server.packs.resources.*
import net.minecraft.util.profiling.*
import java.io.InputStream
import kotlin.jvm.optionals.getOrNull

// TODO: Move this to a more efficient async reloader

object AddonDataReloadListener : SimplePreparableReloadListener<List<AddonData>>(), IdentifiableResourceReloadListener {
    const val MANIFEST_NAME = "showbiz.addon.toml"

    override fun getFabricId(): ResourceLocation = rl("data")

    override fun prepare(manager: ResourceManager, profiler: ProfilerFiller): List<AddonData> {
        val deserializer = serializer<AddonManifest>()
        val addons = mutableListOf<AddonData>()
        for (pack in manager.listPacks()) {
            val packId = pack.packId()
            if (packId.startsWith("fabric-")) continue
            fun err(msg: String, throwable: Throwable? = null) =
                Showbiz.log.error("Addon '$packId' (datapack): $msg", throwable)
            fun warn(msg: String) =
                Showbiz.log.warn("Addon '$packId' (datapack): $msg")
            fun getRes(path: String): IoSupplier<InputStream>? =
                pack.getResource(PackType.SERVER_DATA, rlCustom(packId, path))

            // Reading all the manifest data, and skipping irrelevant packs
            val manifestString = getRes(MANIFEST_NAME)
                ?.get()?.readAllBytes()?.decodeToString()
                ?: continue
            val manifest = runCatching { Toml.partiallyDecodeFromString(deserializer, manifestString, "addon") }
                .getOrElse { err ->
                    err("Failed to parse $MANIFEST_NAME for addon '$packId'", err)
                    continue
                }

            // Validating ID
            val isShowbizMod = (packId == Showbiz.MOD_ID)
                    && (pack.knownPackInfo().getOrNull()?.let { !it.isVanilla && it.id == Showbiz.MOD_ID } ?: false)
            if (isShowbizMod) {
                Showbiz.log.info("Found Showbiz built-in '${packId}'")
            } else {
                if (!AddonId.checkValid(packId, manifest, ::err, ::warn))
                    continue
            }

            // Reading pack data
            val bots = run {
                val e = getRes("${Showbiz.MOD_ID}/bots.toml")
                    ?.get()?.readAllBytes()?.decodeToString()
                    ?: run { err("Failed to find bots.toml"); continue }
                runCatching { Toml.decodeFromString<HashMap<String, AddonBotEntry>>(e) }
                    .getOrElse { err ->
                        err("Failed to parse $MANIFEST_NAME for addon '$packId'", err)
                        continue
                    }
            }

            // Adding the addon to the addon adding adder
            val addon = AddonData(
                manifest,
                bots
            )
            addons.add(addon)
        }
        return addons
    }

    override fun apply(addons: List<AddonData>, manager: ResourceManager, profiler: ProfilerFiller) {
        Showbiz.addons = addons

        val bots = mutableMapOf<String, AddonBotEntry>()
        for (addon in addons) {
            for ((id, bot) in addon.bots) {
                bots[id] = bot
            }
        }
        Showbiz.bots = bots
    }
}