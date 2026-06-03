package com.flooferland.showbiz.renderers.base

import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.entity.*
import net.minecraft.world.entity.*
import com.flooferland.showbiz.types.GeoWorkaroundRenderHook
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import software.bernie.geckolib.animatable.GeoAnimatable
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.cache.`object`.GeoBone
import software.bernie.geckolib.model.GeoModel
import software.bernie.geckolib.renderer.GeoEntityRenderer

/**
 * Workaround for GeckoLib #841: worldSpaceMatrix is broken inside GeoBlockRenderer
 * Credit to https://duzo.is-a.dev
 */
open class GeoFixedEntityRenderer<T>(ctx: EntityRendererProvider.Context, model: GeoModel<T>) : GeoEntityRenderer<T>(ctx, model) where T: GeoAnimatable, T: Entity {
    val hook = GeoWorkaroundRenderHook()

    override fun renderCubesOfBone(poseStack: PoseStack, bone: GeoBone, buffer: VertexConsumer?, packedLight: Int, packedOverlay: Int, colour: Int) {
        if (bone.isHidden) return
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