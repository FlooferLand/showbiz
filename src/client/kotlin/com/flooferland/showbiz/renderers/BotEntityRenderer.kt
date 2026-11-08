package com.flooferland.showbiz.renderers

import com.flooferland.showbiz.ShowbizClient
import com.flooferland.showbiz.entities.BotEntity
import com.flooferland.showbiz.models.BaseBotModel
import com.flooferland.showbiz.models.BotModel
import com.flooferland.showbiz.renderers.base.GeoFixedEntityRenderer
import com.flooferland.showbiz.utils.DrawUtils
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.network.chat.Component
import net.minecraft.util.CommonColors

class BotEntityRenderer(ctx: EntityRendererProvider.Context) : GeoFixedEntityRenderer<BotEntity>(ctx, BotModel()) {
    override fun shouldShowName(entity: BotEntity) = false
    override fun shouldRender(livingEntity: BotEntity, camera: Frustum, camX: Double, camY: Double, camZ: Double): Boolean {
        // TODO: Figure out a better fix to solve Sodium cutting off entities early. Bad for performance.
        //       Not sure how to dynamically scale the entity dimensions based on the bot size cause of addons, or if that's even a good idea
        return true;
    }

    override fun render(entity: BotEntity, entityYaw: Float, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int) {
        BaseBotModel.errorsTriggered[entity]?.let { error -> runCatching {
            val dispatcher = entityRenderDispatcher
            val scale = 0.08f
            val text = "X"

            RenderSystem.disableDepthTest()
            poseStack.pushPose()
            poseStack.translate(0.0, entity.bbHeight / 2.0, 0.0)
            poseStack.mulPose(dispatcher.cameraOrientation())
            poseStack.scale(scale, -scale, scale)
            val xOffset = -font.width(text) / 2f
            val matrix = poseStack.last().pose()
            font.drawInBatch(text, xOffset, 0f, CommonColors.RED, false, matrix, bufferSource, Font.DisplayMode.SEE_THROUGH, 0, packedLight)
            poseStack.popPose()
            RenderSystem.enableDepthTest()
        } }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight)
    }

    companion object {
        fun render(mc: Minecraft, buffer: MultiBufferSource.BufferSource, poseStack: PoseStack) {
            if (mc.debugOverlay?.showDebugScreen() != true) return
            val entity = mc.crosshairPickEntity as? BotEntity ?: return
            val bot = ShowbizClient.bots[entity.botId] ?: return

            val text = Component.empty()
            listOf(
                Component.literal("${entity.botId}:${entity.botVariant}"),
                Component.literal("Show playing=${entity.show.data.playing} map=${entity.show.data.mapping}"),
                Component.literal("Bot mappings={${bot.bitmap.bits.map { "${it.key}=${it.value.size}" }.joinToString(" ")}}")
            ).forEach { text.append(it.append("\n")) }
            DrawUtils.drawText(poseStack, entity.eyePosition.add(0.0, 0.5, 0.0), text, maxWidth = 300, buffer = buffer, vAlign = 1f)
        }
    }
}