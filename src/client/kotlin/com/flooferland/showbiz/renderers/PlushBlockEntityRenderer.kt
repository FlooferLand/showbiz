package com.flooferland.showbiz.renderers

import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.*
import net.minecraft.world.level.block.state.properties.RotationSegment
import com.flooferland.showbiz.blocks.PlushBlock
import com.flooferland.showbiz.blocks.entities.PlushBlockEntity
import com.flooferland.showbiz.models.PlushModel
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.renderer.GeoBlockRenderer

class PlushBlockEntityRenderer(ctx: BlockEntityRendererProvider.Context) : GeoBlockRenderer<PlushBlockEntity>(PlushModel()) {
    override fun actuallyRender(poseStack: PoseStack, animatable: PlushBlockEntity, model: BakedGeoModel?, renderType: RenderType?, bufferSource: MultiBufferSource?, buffer: VertexConsumer?, isReRender: Boolean, partialTick: Float, packedLight: Int, packedOverlay: Int, colour: Int) {
        if (!isReRender) run {
            val rotation = animatable.blockState.getValue(PlushBlock.ROTATION) ?: return@run
            val angle = (360f - RotationSegment.convertToDegrees(rotation)) % 360f
            poseStack.mulPose(Axis.YP.rotationDegrees(angle))

            if (animatable.level?.getBlockState(animatable.blockPos.below())?.block is PlushBlock) {
                var count = 0;
                for (i in 1..5) {
                    if (animatable.level?.getBlockState(animatable.blockPos.below(i))?.block !is PlushBlock) break
                    count += 1
                }
                poseStack.translate(0.0, -0.25 * count, 0.0)
            }
        }
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour)
    }
}