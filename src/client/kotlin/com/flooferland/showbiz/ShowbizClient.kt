package com.flooferland.showbiz

import com.flooferland.showbiz.blocks.entities.PlaybackControllerBlockEntity
import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.registry.ModPackets
import com.flooferland.showbiz.renderers.PlaybackBlockEntityRenderer
import com.flooferland.showbiz.renderers.StagedBotBlockEntityRenderer
import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.renderer.blockentity.*
import net.minecraft.world.level.block.entity.*

class ShowbizClient : ClientModInitializer {
    override fun onInitializeClient() {
        ModPackets.registerC2S()

        BlockEntityRenderers.register(ModBlocks.StagedBot.entity!! as BlockEntityType<StagedBotBlockEntity>, ::StagedBotBlockEntityRenderer)
        BlockEntityRenderers.register(ModBlocks.PlaybackController.entity!! as BlockEntityType<PlaybackControllerBlockEntity>, ::PlaybackBlockEntityRenderer)
    }
}