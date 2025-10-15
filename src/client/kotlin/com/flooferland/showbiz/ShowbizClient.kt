package com.flooferland.showbiz

import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.renderers.StagedBotBlockEntityRenderer
import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers
import net.minecraft.world.level.block.entity.BlockEntityType

class ShowbizClient : ClientModInitializer {
    override fun onInitializeClient() {
        // EntityRendererRegistry.register(ModEntities.Bot.type, ::BotEntityRenderer)
        BlockEntityRenderers.register(ModBlocks.StagedBot.entity!! as BlockEntityType<StagedBotBlockEntity>, ::StagedBotBlockEntityRenderer)
    }
}