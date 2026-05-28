package com.flooferland.showbiz.renderers

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.entity.*
import com.flooferland.showbiz.entities.CollidePartEntity
import com.mojang.blaze3d.vertex.PoseStack

class CollidePartEntityRenderer(val context: EntityRendererProvider.Context) : EntityRenderer<CollidePartEntity>(context) {
    override fun getTextureLocation(entity: CollidePartEntity) = null
    override fun render(entity: CollidePartEntity, entityYaw: Float, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int) {
        if (Minecraft.getInstance()?.gui?.debugOverlay?.showDebugScreen() != true) return
        val dispatcher = entityRenderDispatcher
        val scale = 0.01f
        val text = entity.partId.toString()

        poseStack.pushPose()
        poseStack.translate(0.0, entity.eyeHeight / 2.0, 0.0)
        poseStack.mulPose(dispatcher.cameraOrientation())
        poseStack.scale(scale, -scale, scale)
        val xOffset = -font.width(text) / 2f
        val matrix = poseStack.last().pose()
        font.drawInBatch(text, xOffset, 0f, 0xffff, true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight)
        poseStack.popPose()
    }
}