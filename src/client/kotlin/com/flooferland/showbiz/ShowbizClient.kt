package com.flooferland.showbiz

import com.flooferland.showbiz.addons.assets.AddonAssets
import com.flooferland.showbiz.addons.assets.AddonAssetsReloadListener
import com.flooferland.showbiz.addons.assets.AddonBot
import com.flooferland.showbiz.audio.ShowbizShowAudio
import com.flooferland.showbiz.blocks.entities.ReelToReelBlockEntity
import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity
import com.flooferland.showbiz.items.WandItem
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.registry.ModItems
import com.flooferland.showbiz.registry.ModPackets
import com.flooferland.showbiz.renderers.PlaybackBlockEntityRenderer
import com.flooferland.showbiz.renderers.StagedBotBlockEntityRenderer
import com.flooferland.showbiz.renderers.WandItemRenderer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.minecraft.client.renderer.blockentity.*
import net.minecraft.resources.*
import net.minecraft.server.packs.*
import net.minecraft.world.level.block.entity.*
import software.bernie.geckolib.animatable.client.GeoRenderProvider
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.loading.`object`.BakedAnimations

object ShowbizClient : ClientModInitializer {
    var addons: List<AddonAssets> = listOf()
    var bots: Map<String, AddonBot> = mapOf()
    var models: Map<ResourceLocation, BakedGeoModel> = mapOf()
    var animations: Map<ResourceLocation, BakedAnimations> = mapOf()

    override fun onInitializeClient() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(AddonAssetsReloadListener)
        ModPackets.registerC2S()
        ShowbizShowAudio.init()

        // Block entity renderers (should find a nicer way to register these)
        @Suppress("UNCHECKED_CAST")
        run {
            BlockEntityRenderers.register(
                ModBlocks.StagedBot.entity!! as BlockEntityType<StagedBotBlockEntity>,
                ::StagedBotBlockEntityRenderer
            )
            BlockEntityRenderers.register(
                ModBlocks.ReelToReel.entity!! as BlockEntityType<ReelToReelBlockEntity>,
                ::PlaybackBlockEntityRenderer
            )
        }

        // GeckoLib renderers
        (ModItems.Wand.item as WandItem).renderProviderHolder.value = object : GeoRenderProvider {
            var renderer: WandItemRenderer? = null
            override fun getGeoItemRenderer(): WandItemRenderer {
                if (renderer == null) renderer = WandItemRenderer()
                return renderer!!
            }
        }
    }
}