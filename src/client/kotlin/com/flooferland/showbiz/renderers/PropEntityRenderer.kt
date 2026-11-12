package com.flooferland.showbiz.renderers

import com.flooferland.showbiz.entities.PropEntity
import com.flooferland.showbiz.models.PropModel
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRendererProvider
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.renderer.GeoEntityRenderer

class PropEntityRenderer(ctx: EntityRendererProvider.Context) : GeoEntityRenderer<PropEntity>(ctx, PropModel<PropEntity>()) {
    override fun shouldShowName(entity: PropEntity) = false
    override fun actuallyRender(poseStack: PoseStack, animatable: PropEntity, model: BakedGeoModel, renderType: RenderType?, bufferSource: MultiBufferSource?, buffer: VertexConsumer?, isReRender: Boolean, partialTick: Float, packedLight: Int, packedOverlay: Int, colour: Int) {
        if (!isReRender) run {
            val angle = (360f - animatable.yRot) % 360f
            poseStack.mulPose(Axis.YP.rotationDegrees(angle))
        }
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour)
    }
}