package com.flooferland.showbiz

import net.minecraft.client.*
import net.minecraft.client.gui.screens.*
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.blockentity.*
import net.minecraft.client.telemetry.events.WorldLoadEvent
import net.minecraft.resources.*
import net.minecraft.server.packs.*
import net.minecraft.world.level.block.entity.*
import com.akuleshov7.ktoml.Toml
import com.flooferland.showbiz.Showbiz.MOD_ID
import com.flooferland.showbiz.addons.assets.AddonAssets
import com.flooferland.showbiz.addons.assets.AddonAssetsReloadListener
import com.flooferland.showbiz.addons.assets.AddonBot
import com.flooferland.showbiz.addons.data.BotModelData
import com.flooferland.showbiz.audio.ShowbizShowAudio
import com.flooferland.showbiz.blocks.entities.ReelToReelBlockEntity
import com.flooferland.showbiz.blocks.entities.ShowSelectorBlockEntity
import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity
import com.flooferland.showbiz.items.WandItem
import com.flooferland.showbiz.registry.*
import com.flooferland.showbiz.renderers.*
import com.flooferland.showbiz.resources.ModelPartReloadListener
import com.flooferland.showbiz.screens.ShowParserScreen
import com.flooferland.showbiz.types.BotSoundHandler
import com.flooferland.showbiz.types.ModelPartInstance
import com.flooferland.showbiz.types.ModelPartManager
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.loader.api.FabricLoader
import software.bernie.geckolib.animatable.client.GeoRenderProvider
import software.bernie.geckolib.loading.`object`.BakedAnimations

object ShowbizClient : ClientModInitializer {
    var addons: List<AddonAssets> = listOf()
    var bots: Map<String, AddonBot> = mapOf()
    var botModels: Map<ResourceLocation, BotModelData> = mapOf()
    var animations: Map<ResourceLocation, BakedAnimations> = mapOf()
    var config = ShowbizClientConfig()

    override fun onInitializeClient() {
        // Loading config
        val configResult = runCatching {
            val configFile = FabricLoader.getInstance().configDir.resolve("$MOD_ID.toml").toFile()
            if (configFile.exists()) {
                config = Toml.decodeFromString<ShowbizClientConfig>(configFile.readText())
            } else {
                configFile.writeText(Toml.encodeToString<ShowbizClientConfig>(config))
            }
        }
        configResult.onFailure { throwable ->
            Showbiz.log.error("Error loading config", throwable)
        }

        // Loading the mod
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(AddonAssetsReloadListener)
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(ModelPartReloadListener)
        @Suppress("UnusedExpression")
        run {
            ModPackets
            ModClientEntities
            ModClientCommands
        }
        ShowbizShowAudio.init()
        StagedBotBlockEntity.soundHandler = BotSoundHandler()
        ModelPartManager.modelPartInstancer = { owner, block -> ModelPartInstance(owner, block.id) }

        // Screens
        MenuScreens.register(ModScreenHandlers.ShowParser.type, ::ShowParserScreen)

        // Entity renderers (should find a nicer way to register these)
        @Suppress("UNCHECKED_CAST")
        run {
            BlockEntityRenderers.register(
                ModBlocks.StagedBot.entityType!! as BlockEntityType<StagedBotBlockEntity>,
                ::StagedBotBlockEntityRenderer
            )
            BlockEntityRenderers.register(
                ModBlocks.ReelToReel.entityType!! as BlockEntityType<ReelToReelBlockEntity>,
                ::PlaybackBlockEntityRenderer
            )
            BlockEntityRenderers.register(
                ModBlocks.ShowSelector.entityType!! as BlockEntityType<ShowSelectorBlockEntity>,
                ::ShowSelectorBlockEntityRenderer
            )
            EntityRendererRegistry.register(
                ModClientEntities.ModelPart.type,
                ::ModelPartEntityRenderer
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

        // Block properties / render layers
        for (block in ModBlocks.entries) {
            if (block.model?.transparent != true) continue
            BlockRenderLayerMap.INSTANCE.putBlock(block.block, RenderType.cutout())
        }

        // World
        WorldRenderEvents.LAST.register { context ->
            if (context == null) return@register
            val mc = Minecraft.getInstance() ?: return@register
            val player = mc.player ?: return@register
            val partialTick = context.tickCounter().getGameTimeDeltaPartialTick(true)
            val view = context.camera()?.position ?: return@register

            val pose = context.matrixStack() ?: return@register
            pose.pushPose()
            pose.translate(-view.x, -view.y, -view.z)
            ConnectionRenderer.render(player, mc.renderBuffers().bufferSource(), partialTick)
            ConnectionRenderer.renderDeferred(pose)
            pose.popPose()
        }
    }
}