package com.flooferland.showbiz.renderers

import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.entity.*
import net.minecraft.util.*
import com.flooferland.showbiz.entities.ModelPartEntity
import com.mojang.blaze3d.vertex.PoseStack

class ModelPartEntityRenderer(val context: EntityRendererProvider.Context) : EntityRenderer<ModelPartEntity>(context) {
    override fun getTextureLocation(entity: ModelPartEntity?) = null

    override fun render(entity: ModelPartEntity, entityYaw: Float, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int) {
        val dispatcher = entityRenderDispatcher
        if (dispatcher.crosshairPickEntity != entity) return
        val scale = 0.006f
        val textColor = FastColor.ARGB32.color(255, 255, 255, 255)
        val backgroundColor = FastColor.ARGB32.color(200, 50, 50, 850)
        val text = entity.getName()

        poseStack.pushPose()
        poseStack.translate(0.0, 0.15, 0.0)
        poseStack.mulPose(dispatcher.cameraOrientation())
        poseStack.scale(scale, -scale, scale)
        val xOffset = -font.width(text) / 2f
        val matrix = poseStack.last().pose()
        font.drawInBatch(text, xOffset, 0f, backgroundColor, false, matrix, bufferSource, Font.DisplayMode.NORMAL, backgroundColor, packedLight)
        font.drawInBatch(text, xOffset, 0f, textColor, true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight)
        poseStack.popPose()
    }
}