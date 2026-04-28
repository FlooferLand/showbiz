package com.flooferland.showbiz

import net.minecraft.*
import net.minecraft.client.*
import net.minecraft.client.gui.screens.*
import net.minecraft.client.multiplayer.*
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.blockentity.*
import net.minecraft.network.chat.*
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
import com.flooferland.showbiz.blocks.entities.*
import com.flooferland.showbiz.entities.BotPartEntity
import com.flooferland.showbiz.items.ReelItem
import com.flooferland.showbiz.items.WandItem
import com.flooferland.showbiz.items.base.GeoBlockItem
import com.flooferland.showbiz.models.BaseBotModel
import com.flooferland.showbiz.registry.*
import com.flooferland.showbiz.renderers.*
import com.flooferland.showbiz.resources.ModelPartReloadListener
import com.flooferland.showbiz.screens.*
import com.flooferland.showbiz.types.*
import com.flooferland.showbiz.utils.Extensions.secsToTicks
import java.nio.file.Files
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
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
    var addons: List<AddonAssets> = listOf()
    var bots: Map<ResourceId, AddonBot> = mapOf()
    var botModels: Map<ResourceLocation, BotModelData> = mapOf()
    var animations: Map<ResourceLocation, BakedAnimations> = mapOf()

    override fun onInitializeClient() {
        // Loading the mod
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(AddonAssetsReloadListener)
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(ModelPartReloadListener)
        @Suppress("UnusedExpression")
        run {
            ModPackets
            ModClientEntities
            ModClientCommands
            ModClientVeil
            ClientConnections
        }
        ShowbizShowAudio.init()
        StagedBotBlockEntity.soundHandler = BotSoundHandler()
        ModelPartManager.clientModelPartInstancer = { owner, block -> ClientModelPartInstance(owner, block.id) }
        for (block in ModBlocks.entries) {
            val path = FabricLoader.getInstance().getModContainer(MOD_ID).getOrNull()
                ?.findPath("assets/${MOD_ID}/geo/block/${block.id.path}.geo.json")?.getOrNull()
            val isGeckoLib = path?.let { Files.exists(it) } ?: false
            if (block.isGeckoLib != isGeckoLib && Showbiz.log.isDebugEnabled) {
                if (isGeckoLib) error("Block '${block.id}' is marked to not be using GeckoLib, but it actually is?")
                else error("Block '${block.id}' is marked to be using GeckoLib, but it actually isn't?")
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
            BlockEntityRenderers.register(
                ModBlocks.StagedBot.entityType!! as BlockEntityType<StagedBotBlockEntity>,
                ::StagedBotBlockEntityRenderer
            )
            BlockEntityRenderers.register(
                ModBlocks.ReelToReel.entityType!! as BlockEntityType<ReelToReelBlockEntity>,
                ::ReelToReelBlockEntityRenderer
            )
            BlockEntityRenderers.register(
                ModBlocks.ShowSelector.entityType!! as BlockEntityType<ShowSelectorBlockEntity>,
                ::ShowSelectorBlockEntityRenderer
            )
            BlockEntityRenderers.register(
                ModBlocks.CurtainBlock.entityType!! as BlockEntityType<CurtainBlockEntity>,
                ::CurtainBlockEntityRenderer
            )
            BlockEntityRenderers.register(
                ModBlocks.Spotlight.entityType!! as BlockEntityType<SpotlightBlockEntity>,
                ::SpotlightBlockEntityRenderer
            )
            EntityRendererRegistry.register(
                ModClientEntities.ModelPart.type,
                ::ModelPartEntityRenderer
            )
            EntityRendererRegistry.register(
                ModClientEntities.BotPart.type,
                ::BotPartEntityRenderer
            )
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
        for (block in ModBlocks.entries) {
            if (!block.isGeckoLib) continue
            val item = block.item as? GeoBlockItem ?: continue
            item.renderProviderHolder.value = object : GeoRenderProvider {
                inner class Renderer : GeoItemRenderer<GeoBlockItem>(DefaultedBlockGeoModel(block.id))
                var renderer: Renderer? = null
                override fun getGeoItemRenderer(): Renderer {
                    if (renderer == null) renderer = Renderer()
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
        ClientTickEvents.END_WORLD_TICK.register { level ->
            if (StagedBotBlockEntityRenderer.renderExceptionCountdown <= 0 && BaseBotModel.errorsTriggered.isNotEmpty()) {
                for (err in BaseBotModel.errorsTriggered) {
                    val message = "Render error '${err.name}'${err.botId?.let { " for bot '$it'" } ?: ""}: ${err.context}"
                    Showbiz.log.error(message)
                    Minecraft.getInstance()?.player?.displayClientMessage(
                        Component.literal(message + "\n").withStyle(ChatFormatting.RED).append("Please check the game logs."),
                        false
                    )
                }
                StagedBotBlockEntityRenderer.renderExceptionCountdown = 5f.secsToTicks().toFloat()
            } else {
                StagedBotBlockEntityRenderer.renderExceptionCountdown -= 1
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
        AbstractBotPart.clientSpawn = { level, id, owner ->
            val entity = BotPartEntity(level, id, owner)
            (level as? ClientLevel)?.addEntity(entity)
            entity
        }
    }

    fun resetAssetErrors() {
        BaseBotModel.errorsTriggered.clear()
        StagedBotBlockEntityRenderer.renderExceptionCountdown = 0f
    }
}