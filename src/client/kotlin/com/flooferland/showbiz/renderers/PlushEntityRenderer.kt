package com.flooferland.showbiz.renderers

import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.entity.*
import com.flooferland.showbiz.entities.PlushEntity
import com.flooferland.showbiz.models.PlushModel
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.renderer.GeoEntityRenderer

class PlushEntityRenderer(ctx: EntityRendererProvider.Context) : GeoEntityRenderer<PlushEntity>(ctx, PlushModel<PlushEntity>()) {
    override fun actuallyRender(poseStack: PoseStack, animatable: PlushEntity, model: BakedGeoModel, renderType: RenderType?, bufferSource: MultiBufferSource?, buffer: VertexConsumer?, isReRender: Boolean, partialTick: Float, packedLight: Int, packedOverlay: Int, colour: Int) {
        if (!isReRender) run {
            val angle = (360f - animatable.yRot) % 360f
            poseStack.mulPose(Axis.YP.rotationDegrees(angle))
        }
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour)
    }
}