package com.flooferland.showbiz

import com.flooferland.showbiz.addons.assets.AddonAssets
import com.flooferland.showbiz.addons.assets.AddonAssetsReloadListener
import com.flooferland.showbiz.addons.assets.AddonBot
import com.flooferland.showbiz.blocks.entities.PlaybackControllerBlockEntity
import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.registry.ModPackets
import com.flooferland.showbiz.renderers.PlaybackBlockEntityRenderer
import com.flooferland.showbiz.renderers.StagedBotBlockEntityRenderer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.minecraft.client.renderer.blockentity.*
import net.minecraft.resources.*
import net.minecraft.server.packs.*
import net.minecraft.world.level.block.entity.*
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.loading.`object`.BakedAnimations

object ShowbizClient : ClientModInitializer {
    var addons: List<AddonAssets> = listOf()
    var bots: Map<String, AddonBot> = mapOf()
    var models: Map<ResourceLocation, BakedGeoModel> = mapOf()
    var animations: Map<ResourceLocation, BakedAnimations> = mapOf()

    override fun onInitializeClient() {
        ModPackets.registerC2S()
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(AddonAssetsReloadListener)

        // Block entity renderers (should find a nicer way to register these)
        @Suppress("UNCHECKED_CAST")
        run {
            BlockEntityRenderers.register(
                ModBlocks.StagedBot.entity!! as BlockEntityType<StagedBotBlockEntity>,
                ::StagedBotBlockEntityRenderer
            )
            BlockEntityRenderers.register(
                ModBlocks.PlaybackController.entity!! as BlockEntityType<PlaybackControllerBlockEntity>,
                ::PlaybackBlockEntityRenderer
            )
        }
    }
}