package com.flooferland.showbiz.renderers.layers

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import software.bernie.geckolib.animatable.GeoAnimatable
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.renderer.GeoRenderer
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer

/** Quite a mouthful */
class BetterAutoGlowingGeoLayer<T>(renderer: GeoRenderer<T>) : AutoGlowingGeoLayer<T>(renderer) where T: GeoAnimatable {
    var shouldRender = false

    override fun getRenderType(animatable: T, bufferSource: MultiBufferSource?): RenderType? {
        if (!shouldRender) return null
        return super.getRenderType(animatable, bufferSource)
    }

    override fun render(poseStack: PoseStack, animatable: T, bakedModel: BakedGeoModel, renderType: RenderType?, bufferSource: MultiBufferSource?, buffer: VertexConsumer?, partialTick: Float, packedLight: Int, packedOverlay: Int) {
        if (!shouldRender) return
        super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay)
    }
}