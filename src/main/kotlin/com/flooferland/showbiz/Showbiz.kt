package com.flooferland.showbiz

import net.minecraft.server.packs.*
import com.akuleshov7.ktoml.Toml
import com.flooferland.showbiz.addons.data.AddonBotEntry
import com.flooferland.showbiz.addons.data.AddonData
import com.flooferland.showbiz.addons.data.AddonDataReloadListener
import com.flooferland.showbiz.network.packets.BotListPacket
import com.flooferland.showbiz.network.packets.ProgrammerKeyPressPacket
import com.flooferland.showbiz.network.packets.ProgrammerPlayerUpdatePacket
import com.flooferland.showbiz.network.packets.ServerCapabilitiesPacket
import com.flooferland.showbiz.registry.*
import com.flooferland.showbiz.types.BitChartStore
import com.flooferland.showbiz.types.FFmpeg
import com.flooferland.showbiz.types.ResourceId
import com.flooferland.showbiz.types.connection.ServerConnections
import com.flooferland.showbiz.types.entity.PlayerProgrammingData
import com.flooferland.showbiz.utils.ShowbizUtils
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Showbiz : ModInitializer {
    const val MOD_ID = "showbiz"
    val log: Logger = LoggerFactory.getLogger(MOD_ID)

    var addons = listOf<AddonData>()
    var bots = mapOf<ResourceId, AddonBotEntry>()
    val charts = BitChartStore()
    var config = ModConfig()

    override fun onInitialize() {
        // Loading config
        val configResult = runCatching {
            val configFile = FabricLoader.getInstance().configDir.resolve("$MOD_ID.toml").toFile()
            if (configFile.exists()) {
                config = Toml.decodeFromString<ModConfig>(configFile.readText())
            } else {
                configFile.writeText(Toml.encodeToString<ModConfig>(config))
            }
        }
        configResult.onFailure { throwable ->
            Showbiz.log.error("Error loading config", throwable)
        }

        // Loading FFMPEG
        FFmpeg.init()

        // Making sure the JVM compiles these
        @Suppress("UnusedExpression")
        run {
            ModComponents.register()
            ModPackets.register()
            ModEntities.register()
            ModBlocks.entries
            ModItems.entries
            ModMusicDiscs.entries
            ModItemGroups.entries
            ModSounds.entries
            ModPlayerSynchedData
            ModCommands
            ModScreenHandlers
            if (ShowbizUtils.hasComputerCraft()) run { ModPeripherals.register() }
            ServerConnections
            FileStorage
            FileServer
            UpdateChecker
            ServerPackets.init()
        }

        // Services
        UpdateChecker.check()
        ServerTickEvents.START_SERVER_TICK.register { server ->
            FFmpeg.serverAvailable = FFmpeg.localAvailable
            FileServer.update(server)
        }
        ServerPlayerEvents.JOIN.register { player ->
            ServerPlayNetworking.send(player, ServerCapabilitiesPacket(hasFFmpeg = FFmpeg.localAvailable))
        }

        // Server addons
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(AddonDataReloadListener)

        // Bot selection
        ServerPackets.listen(BotListPacket.type) { _, ctx ->
            ServerPlayNetworking.send(ctx.player(), BotListPacket(toClient = true, bots = Showbiz.bots))
        }

        // Programming
        ServerPackets.listen(ProgrammerKeyPressPacket.type) { packet, ctx ->
            val player = ctx.player()
            val data = PlayerProgrammingData.getFromPlayer(player)
            data.heldKeys[packet.key] = packet.pressed
            data.saveToPlayer(player)
        }
        ServerPackets.listen(ProgrammerPlayerUpdatePacket.type) { packet, ctx ->
            val player = ctx.player()
            val data = PlayerProgrammingData.getFromPlayer(player)
            packet.keysToBits.forEachIndexed { i, receivedBits ->
                data.keysToBits[i].set(receivedBits)
            }
            data.saveToPlayer(player)
        }
        ServerPlayerEvents.JOIN.register { player ->
            PlayerProgrammingData.resetPlayerState(player)
            FileServer.serverPlayerUploads.remove(player.id)
        }
        ServerPlayerEvents.LEAVE.register { player ->
            PlayerProgrammingData.resetPlayerState(player)
            FileServer.serverPlayerUploads.remove(player.id)
        }
        ServerPlayerEvents.AFTER_RESPAWN.register { oldPlayer, newPlayer, alive ->
            val data = PlayerProgrammingData.getFromPlayer(oldPlayer)
            data.cleanBasic()
            data.saveToPlayer(newPlayer)
            FileServer.serverPlayerUploads.remove(oldPlayer.id)
        }

        // Finished
        log.info("Enjoy the show!")
        log.debug("Debugging log level enabled! More information will be printed (bogos binted 👽)")
    }
}