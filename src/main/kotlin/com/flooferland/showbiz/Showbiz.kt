package com.flooferland.showbiz

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import com.flooferland.showbiz.addons.data.AddonBotEntry
import com.flooferland.showbiz.addons.data.AddonData
import com.flooferland.showbiz.addons.data.AddonDataReloadListener
import com.flooferland.showbiz.registry.*
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.minecraft.server.packs.*
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.jvm.optionals.getOrNull

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
        }

        // Server addons
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(AddonDataReloadListener)
        
        // Finished
        log.info("Enjoy the show!")
        log.debug("Debugging log level enabled! More information will be printed (bogos binted 👽)")

        // Temporary beta warning
        ServerPlayerEvents.JOIN.register { player ->
            val mod = FabricLoader.getInstance()?.getModContainer(MOD_ID)?.getOrNull() ?: return@register
            val modInfo = mod.metadata
            val logo = Component.literal("Showbiz").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
            val version = Component.literal(modInfo.version.friendlyString).withStyle(ChatFormatting.GRAY, ChatFormatting.UNDERLINE, ChatFormatting.BOLD)
            player.displayClientMessage(
                logo.append(" ").append(version).append("\n"),
                false
            )
            player.displayClientMessage(
                Component.literal("WARNING: \n").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
                    .append(Component.literal(
                        "Showbiz is still in very early access.\n" +
                                "Expect the mod's blocks, items, and other things to vanish or corrupt when you update the mod."
                    ).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD)),
                false
            )
            player.displayClientMessage(
                Component.literal("KNOWN BUGS: \n").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
                    .append(Component.literal(
                        "- If a show's signal data desyncs from its audio, rejoin the game or restart the show\n" +
                        "- If you can't see connections, rejoin the game."
                    ).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD)),
                false
            )
        }
    }
}