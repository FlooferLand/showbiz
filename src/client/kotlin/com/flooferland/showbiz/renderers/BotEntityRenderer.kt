package com.flooferland.showbiz.renderers

import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.entity.*
import com.flooferland.showbiz.entities.BotEntity
import com.flooferland.showbiz.models.BotModel
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.renderer.GeoEntityRenderer

class BotEntityRenderer(ctx: EntityRendererProvider.Context) : GeoEntityRenderer<BotEntity>(ctx, BotModel<BotEntity>()) {
    override fun actuallyRender(poseStack: PoseStack, animatable: BotEntity, model: BakedGeoModel, renderType: RenderType?, bufferSource: MultiBufferSource?, buffer: VertexConsumer?, isReRender: Boolean, partialTick: Float, packedLight: Int, packedOverlay: Int, colour: Int) {
        val angle = (360f - animatable.yRot) % 360f
        poseStack.mulPose(Axis.YP.rotationDegrees(angle))
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour)
    }
}