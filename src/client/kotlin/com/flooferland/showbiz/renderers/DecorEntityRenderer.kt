package com.flooferland.showbiz.renderers

import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.entity.*
import com.flooferland.showbiz.entities.DecorEntity
import com.flooferland.showbiz.models.DecorModel
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import software.bernie.geckolib.renderer.GeoEntityRenderer

class DecorEntityRenderer(ctx: EntityRendererProvider.Context) : GeoEntityRenderer<DecorEntity>(ctx, DecorModel()) {
    override fun render(entity: DecorEntity, entityYaw: Float, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int) {
        if (Minecraft.getInstance()?.gui?.debugOverlay?.showDebugScreen() == true) {
            val dispatcher = entityRenderDispatcher
            val scale = 0.01f
            val text = entity.decorId.toString()

            RenderSystem.disableDepthTest()
            poseStack.pushPose()
            poseStack.translate(0.0, entity.eyeHeight / 2.0, 0.0)
            poseStack.mulPose(dispatcher.cameraOrientation())
            poseStack.scale(scale, -scale, scale)
            val xOffset = -font.width(text) / 2f
            val matrix = poseStack.last().pose()
            font.drawInBatch(text, xOffset, 0f, 0xffff, true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight)
            poseStack.popPose()
            RenderSystem.enableDepthTest()
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight)
    }
}