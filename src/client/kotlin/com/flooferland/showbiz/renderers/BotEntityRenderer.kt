package com.flooferland.showbiz.renderers

import net.minecraft.client.gui.*
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.culling.*
import net.minecraft.client.renderer.entity.*
import net.minecraft.util.*
import com.flooferland.showbiz.entities.BotEntity
import com.flooferland.showbiz.models.BaseBotModel
import com.flooferland.showbiz.models.BotModel
import com.flooferland.showbiz.renderers.base.GeoFixedEntityRenderer
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack

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
}