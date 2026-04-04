package com.flooferland.showbiz

import net.minecraft.core.component.DataComponentPatch
import net.minecraft.server.packs.*
import com.flooferland.showbiz.FileServer.sendShowsToClient
import com.flooferland.showbiz.FileStorage.fetchShows
import com.flooferland.showbiz.addons.data.AddonBotEntry
import com.flooferland.showbiz.addons.data.AddonData
import com.flooferland.showbiz.addons.data.AddonDataReloadListener
import com.flooferland.showbiz.items.ReelItem
import com.flooferland.showbiz.network.packets.BotListPacket
import com.flooferland.showbiz.network.packets.BotListSelectPacket
import com.flooferland.showbiz.network.packets.ShowFileListPacket
import com.flooferland.showbiz.network.packets.ShowFileSelectPacket
import com.flooferland.showbiz.registry.*
import com.flooferland.showbiz.types.connection.GlobalConnections
import com.flooferland.showbiz.utils.Extensions.getHeldItem
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.io.path.name

object Showbiz : ModInitializer {
    const val MOD_ID = "showbiz"
    val log: Logger = LoggerFactory.getLogger(MOD_ID)

    var addons = listOf<AddonData>()
    var bots = mapOf<String, AddonBotEntry>()

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
            ModCommands
            ModScreenHandlers
            GlobalConnections
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
        
        // Finished
        log.info("Enjoy the show!")
        log.debug("Debugging log level enabled! More information will be printed (bogos binted 👽)")
    }
}