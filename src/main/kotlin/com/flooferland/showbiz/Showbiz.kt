package com.flooferland.showbiz

import net.minecraft.server.packs.*
import com.flooferland.showbiz.addons.data.AddonBotEntry
import com.flooferland.showbiz.addons.data.AddonData
import com.flooferland.showbiz.addons.data.AddonDataReloadListener
import com.flooferland.showbiz.registry.*
import com.flooferland.showbiz.types.connection.GlobalConnections
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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
        
        // Finished
        log.info("Enjoy the show!")
        log.debug("Debugging log level enabled! More information will be printed (bogos binted 👽)")
    }
}