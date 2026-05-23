package com.flooferland.showbiz.renderers.base

import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.level.block.entity.BlockEntity
import com.flooferland.showbiz.types.GeoWorkaroundRenderHook
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import software.bernie.geckolib.animatable.GeoAnimatable
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.cache.`object`.GeoBone
import software.bernie.geckolib.model.GeoModel
import software.bernie.geckolib.renderer.GeoBlockRenderer

/**
 * Workaround for GeckoLib #841: worldSpaceMatrix is broken inside GeoBlockRenderer
 * Credit to https://duzo.is-a.dev
 */
open class GeoFixedBlockEntityRenderer<T>(model: GeoModel<T>) : GeoBlockRenderer<T>(model) where T: GeoAnimatable, T: BlockEntity {
    val hook = GeoWorkaroundRenderHook()

    override fun renderCubesOfBone(poseStack: PoseStack, bone: GeoBone, buffer: VertexConsumer?, packedLight: Int, packedOverlay: Int, colour: Int) {
        hook.beforeRenderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, colour)
        super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, colour)
    }

    override fun preRender(poseStack: PoseStack, animatable: T, model: BakedGeoModel, bufferSource: MultiBufferSource?, buffer: VertexConsumer?, isReRender: Boolean, partialTick: Float, packedLight: Int, packedOverlay: Int, colour: Int) {
        hook.beforePreRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour)
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour)
    }

    override fun postRender(poseStack: PoseStack, animatable: T, model: BakedGeoModel, bufferSource: MultiBufferSource, buffer: VertexConsumer?, isReRender: Boolean, partialTick: Float, packedLight: Int, packedOverlay: Int, colour: Int) {
        hook.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour)
    }
}