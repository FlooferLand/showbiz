package com.flooferland.showbiz

import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.registry.ModItemGroups
import com.flooferland.showbiz.registry.ModItems
import net.fabricmc.api.ModInitializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Showbiz : ModInitializer {
    const val MOD_ID = "showbiz"
    val log: Logger = LoggerFactory.getLogger(MOD_ID)

    override fun onInitialize() {
        // Making sure the JVM compiles these
        run {
            ModBlocks.entries
            ModItems.entries
            ModItemGroups
        }

        log.info("Enjoy the show!")
    }
}