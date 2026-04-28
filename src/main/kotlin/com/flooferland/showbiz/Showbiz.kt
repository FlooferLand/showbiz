package com.flooferland.showbiz

import net.minecraft.server.packs.*
import com.flooferland.showbiz.addons.data.AddonBotEntry
import com.flooferland.showbiz.addons.data.AddonData
import com.flooferland.showbiz.addons.data.AddonDataReloadListener
import com.flooferland.showbiz.network.packets.BotListPacket
import com.flooferland.showbiz.network.packets.ProgrammerKeyPressPacket
import com.flooferland.showbiz.network.packets.ProgrammerPlayerUpdatePacket
import com.flooferland.showbiz.registry.*
import com.flooferland.showbiz.types.ResourceId
import com.flooferland.showbiz.types.connection.ServerConnections
import com.flooferland.showbiz.types.entity.PlayerProgrammingData
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Showbiz : ModInitializer {
    const val MOD_ID = "showbiz"
    val log: Logger = LoggerFactory.getLogger(MOD_ID)

    var addons = listOf<AddonData>()
    var bots = mapOf<ResourceId, AddonBotEntry>()

    override fun onInitialize() {
        // Making sure the JVM compiles these
        @Suppress("UnusedExpression")
        run {
            ModComponents
            ModPackets
            ModBlocks.entries
            ModItems.entries
            ModItemGroups.entries
            ModSounds.entries
            ModPlayerSynchedData
            ModCommands
            ModScreenHandlers
            ServerConnections
            FileStorage
            FileServer
            UpdateChecker
        }

        // Services
        UpdateChecker.check()
        ServerTickEvents.START_SERVER_TICK.register { server ->
            FileServer.update(server)
        }

        // Server addons
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(AddonDataReloadListener)

        // Bot selection
        ServerPlayNetworking.registerGlobalReceiver(BotListPacket.type) { _, ctx ->
            ServerPlayNetworking.send(ctx.player(), BotListPacket(toClient = true, bots = Showbiz.bots))
        }

        // Programming
        ServerPlayNetworking.registerGlobalReceiver(ProgrammerKeyPressPacket.type) { packet, ctx ->
            val player = ctx.player()
            val data = PlayerProgrammingData.getFromPlayer(player)
            data.heldKeys[packet.key] = packet.pressed
            data.saveToPlayer(player)
        }
        ServerPlayNetworking.registerGlobalReceiver(ProgrammerPlayerUpdatePacket.type) { packet, ctx ->
            val player = ctx.player()
            val data = PlayerProgrammingData.getFromPlayer(player)
            data.keysToBits = packet.keysToBits
            data.recording = packet.recording
            data.saveToPlayer(player)
        }
        ServerPlayerEvents.JOIN.register { player -> PlayerProgrammingData.resetPlayerState(player) }
        ServerPlayerEvents.AFTER_RESPAWN.register { oldPlayer, newPlayer, alive ->
            val data = PlayerProgrammingData.getFromPlayer(oldPlayer)
            data.cleanBasic()
            data.saveToPlayer(newPlayer)
        }

        // Finished
        log.info("Enjoy the show!")
        log.debug("Debugging log level enabled! More information will be printed (bogos binted 👽)")
    }
}