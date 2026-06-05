package com.flooferland.showbiz

import net.minecraft.*
import net.minecraft.client.*
import net.minecraft.client.gui.screens.*
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.blockentity.*
import net.minecraft.network.chat.*
import net.minecraft.resources.*
import net.minecraft.server.packs.*
import net.minecraft.world.level.block.entity.*
import com.flooferland.showbiz.Showbiz.MOD_ID
import com.flooferland.showbiz.addons.assets.AddonAssets
import com.flooferland.showbiz.addons.assets.AddonAssetsReloadListener
import com.flooferland.showbiz.addons.assets.AddonBot
import com.flooferland.showbiz.addons.data.BotModelData
import com.flooferland.showbiz.audio.ShowbizShowAudio
import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity
import com.flooferland.showbiz.entities.DecorEntity
import com.flooferland.showbiz.items.PlushItem
import com.flooferland.showbiz.items.ReelItem
import com.flooferland.showbiz.items.WandItem
import com.flooferland.showbiz.items.base.GeoBlockItem
import com.flooferland.showbiz.models.BaseBotModel
import com.flooferland.showbiz.registry.*
import com.flooferland.showbiz.renderers.*
import com.flooferland.showbiz.resources.ModelPartReloadListener
import com.flooferland.showbiz.screens.*
import com.flooferland.showbiz.types.*
import com.flooferland.showbiz.types.collidepart.CollidePartManager
import com.flooferland.showbiz.types.modelpart.ModelPartManager
import com.flooferland.showbiz.utils.Extensions.secsToTicks
import com.flooferland.showbiz.utils.ShowbizUtils
import java.nio.file.Files
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.loader.api.FabricLoader
import software.bernie.geckolib.animatable.client.GeoRenderProvider
import software.bernie.geckolib.loading.`object`.BakedAnimations
import software.bernie.geckolib.model.DefaultedBlockGeoModel
import software.bernie.geckolib.renderer.GeoItemRenderer
import kotlin.jvm.optionals.getOrNull

object ShowbizClient : ClientModInitializer {
    var addons: List<AddonAssets> = emptyList()
    var bots: Map<ResourceId, AddonBot> = emptyMap()
    var botModels: Map<ResourceLocation, BotModelData> = emptyMap()
    var animations: Map<ResourceLocation, BakedAnimations> = emptyMap()

    override fun onInitializeClient() {
        // Loading the mod
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(AddonAssetsReloadListener)
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(ModelPartReloadListener)
        @Suppress("UnusedExpression")
        run {
            ModPackets
            ModClientEntities
            ModClientCommands
            if (ShowbizUtils.clientHasVeil()) {
                @Suppress("RedundantLambdaOrAnonymousFunction")
                { ModClientVeil.load() }()
            }
            ClientConnections
            ClientPackets.init()
        }
        ShowbizShowAudio.init()
        StagedBotBlockEntity.soundHandler = BotSoundHandler()
        for (block in ModBlocks.entries) {
            val container = FabricLoader.getInstance().getModContainer(MOD_ID).getOrNull() ?: break
            val paths = listOf(
                "assets/${MOD_ID}/geo/block/${block.id.path}.geo.json",
                "assets/${MOD_ID}/geo/${block.id.path}.geo.json",
            )
            val path = paths.firstOrNull { container.findPath(it)?.getOrNull()?.let { Files.exists(it) } == true }
            val geckoModelFound = (path != null)
            if (block.isGeckoLib != geckoModelFound && Showbiz.log.isDebugEnabled) {
                if (geckoModelFound) error("Block '${block.id}' is marked to not use GeckoLib, but a model was found? (${path})")
                else error("Block '${block.id}' is marked to use GeckoLib, but no model was found in:\n- ${paths.joinToString("\n - ")}")
            }
        }

        // Screens
        MenuScreens.register(ModScreenHandlers.ShowParserEdit.type, ::ShowParserEditScreen)
        MenuScreens.register(ModScreenHandlers.SpotlightEdit.type, ::SpotlightEditScreen)
        MenuScreens.register(ModScreenHandlers.CurtainControllerEdit.type, ::CurtainControllerEditScreen)
        MenuScreens.register(ModScreenHandlers.BotSelect.type, ::BotSelectScreen)
        MenuScreens.register(ModScreenHandlers.BitView.type, ::BitViewScreen)

        // Entity renderers (should find a nicer way to register these)
        @Suppress("UNCHECKED_CAST")
        run {
            fun <T> add(modBlock: ModBlocks, renderer: BlockEntityRendererProvider<T>) where T: BlockEntity =
                BlockEntityRenderers.register(
                    modBlock.entityType as BlockEntityType<T>,
                    renderer
                )
            JukeboxLyricRenderer.register()
            add(ModBlocks.StagedBot, ::StagedBotBlockBlockEntityRenderer)
            add(ModBlocks.ReelToReel, ::ReelToReelBlockEntityRenderer)
            add(ModBlocks.ShowSelector, ::ShowSelectorBlockEntityRenderer)
            add(ModBlocks.CurtainBlock, ::CurtainBlockEntityRenderer)
            add(ModBlocks.Spotlight, ::SpotlightBlockEntityRenderer)
            add(ModBlocks.Cymbal, ::CymbalBlockEntityRenderer)
            add(ModBlocks.ReelHolder, ::ReelHolderBlockEntityRenderer)
            add(ModBlocks.Monitor, ::MonitorBlockEntityRenderer)
            EntityRendererRegistry.register(ModClientEntities.ModelPart.type, ::ModelPartEntityRenderer)
            EntityRendererRegistry.register(ModClientEntities.CollidePart.type, ::CollidePartEntityRenderer)
            EntityRendererRegistry.register(ModClientEntities.Decor.type, ::DecorEntityRenderer)
            EntityRendererRegistry.register(ModEntities.Plush.type, ::PlushEntityRenderer)
        }

        // GeckoLib renderers
        // TODO: Automate GeckoLib item renderers, the same way I did for the block ones
        (ModItems.Wand.item as WandItem).renderProviderHolder.value = object : GeoRenderProvider {
            var renderer: WandItemRenderer? = null
            override fun getGeoItemRenderer(): WandItemRenderer {
                if (renderer == null) renderer = WandItemRenderer()
                return renderer!!
            }
        }
        (ModItems.Plush.item as PlushItem).renderProviderHolder.value = object : GeoRenderProvider {
            var renderer: PlushItemRenderer? = null
            override fun getGeoItemRenderer(): PlushItemRenderer {
                if (renderer == null) renderer = PlushItemRenderer()
                return renderer!!
            }
        }
        for (block in ModBlocks.entries) {
            if (!block.isGeckoLib) continue
            val item = block.item as? GeoBlockItem ?: continue
            item.renderProviderHolder.value = object : GeoRenderProvider {
                var renderer: GeoItemRenderer<*>? = null
                override fun getGeoItemRenderer(): GeoItemRenderer<*> {
                    if (renderer == null) renderer = when (block) {
                        else -> object : GeoItemRenderer<GeoBlockItem>(DefaultedBlockGeoModel(block.id)) {}
                    }
                    return renderer!!
                }
            }
        }

        // Block properties / render layers
        for (block in ModBlocks.entries) {
            if (block.model?.transparent != true) continue
            BlockRenderLayerMap.INSTANCE.putBlock(block.block, RenderType.cutout())
        }

        // World
        WorldRenderEvents.LAST.register { ctx ->
            if (ctx == null) return@register
            val mc = Minecraft.getInstance() ?: return@register
            val player = mc.player ?: return@register
            val level = player.clientLevel ?: return@register
            val partialTick = ctx.tickCounter().getGameTimeDeltaPartialTick(true)
            val view = ctx.camera()?.position ?: return@register

            val pose = ctx.matrixStack() ?: return@register
            pose.pushPose()
            pose.translate(-view.x, -view.y, -view.z)
            JukeboxLyricRenderer.render(level, player, mc.renderBuffers().bufferSource(), pose)
            ConnectionRenderer.render(player, mc.renderBuffers().bufferSource(), partialTick)
            ConnectionRenderer.renderDeferred(pose)
            pose.popPose()

            DecorEntity.moveAll(ctx)
        }
        ClientTickEvents.END_WORLD_TICK.register { level ->
            if (StagedBotBlockBlockEntityRenderer.renderExceptionCountdown <= 0 && BaseBotModel.errorsTriggered.isNotEmpty()) {
                for (err in BaseBotModel.errorsTriggered) {
                    val message = "Render error '${err.name}'${err.botId?.let { " for bot '$it'" } ?: ""}: ${err.context}"
                    Showbiz.log.error(message)
                    Minecraft.getInstance()?.player?.displayClientMessage(
                        Component.literal(message + "\n").withStyle(ChatFormatting.RED).append("Please check the game logs."),
                        false
                    )
                }
                StagedBotBlockBlockEntityRenderer.renderExceptionCountdown = 5f.secsToTicks().toFloat()
            } else {
                StagedBotBlockBlockEntityRenderer.renderExceptionCountdown -= 1
            }
            BaseBotModel.errorsTriggered.clear()
        }
        ClientPlayConnectionEvents.JOIN.register { listener, sender, minecraft ->
            resetAssetErrors()

            // Temporary beta warning
            val player = minecraft.player ?: return@register
            val logo = Component.literal("Showbiz").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
            val version = Component.literal(UpdateChecker.currentVersion).withStyle(ChatFormatting.GRAY, ChatFormatting.UNDERLINE, ChatFormatting.BOLD)
            player.displayClientMessage(logo.append(" ").append(version).append("\n"), false)
            player.displayClientMessage(
                Component.literal("WARNING: \n").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
                    .append(Component.literal(
                        "Showbiz is still in very early access.\n" +
                                "Expect the mod's blocks, items, and other things to vanish or break when you update the mod.\n"
                    ).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD)),
                false
            )
            /*player.displayClientMessage(
                Component.literal("KNOWN BUGS: \n").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
                    .append(Component.literal(
                        "- None"
                    ).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD)),
                false
            )*/

            // Version check
            if (UpdateChecker.newerVersion != null)
                minecraft.player?.displayClientMessage(UpdateChecker.getMessage(), false)
        }

        // DARN YOU SPLIT SOURCESETS
        ReelItem.openScreenClient = { stack -> Minecraft.getInstance()?.setScreen(ReelManagerScreen(stack)) }
        ModelPartManager.clientInstancer = { owner, block, customParts -> ClientModelPartInstance(owner, block.id, customParts) }
        CollidePartManager.clientInstancer = { owner -> ClientCollidePartInstance(owner) }
        StagedBotBlockEntity.decor = DecorEntity.decorTick
    }

    fun getDeltaTime() =
        Minecraft.getInstance().timer.gameTimeDeltaTicks.coerceAtMost(1.25f)

    fun resetAssetErrors() {
        BaseBotModel.errorsTriggered.clear()
        StagedBotBlockBlockEntityRenderer.renderExceptionCountdown = 0f
    }
}