package com.flooferland.showbiz

import com.flooferland.showbiz.registry.*
import net.fabricmc.api.ModInitializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Showbiz : ModInitializer {
    const val MOD_ID = "showbiz"
    val log: Logger = LoggerFactory.getLogger(MOD_ID)

    override fun onInitialize() {
        // Making sure the JVM compiles these
        @Suppress("UnusedExpression")
        run {
            ModBlocks.entries
            ModItems.entries
            ModItemGroups.entries
            ModSounds.entries
            ModComponents.WandBind
            ModCommands
        }

        // Networking
        ModPackets.registerS2C()

        // Finished
        log.info("Enjoy the show!")
    }
}