package com.flooferland.showbiz.renderers

import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.blockentity.*
import net.minecraft.world.level.block.state.properties.*
import com.flooferland.showbiz.blocks.PlushBlock
import com.flooferland.showbiz.blocks.entities.PlushBlockEntity
import com.flooferland.showbiz.models.PlushModel
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.renderer.GeoBlockRenderer

class PlushBlockEntityRenderer(ctx: BlockEntityRendererProvider.Context) : GeoBlockRenderer<PlushBlockEntity>(PlushModel()) {
    override fun preRender(poseStack: PoseStack, animatable: PlushBlockEntity, model: BakedGeoModel, bufferSource: MultiBufferSource?, buffer: VertexConsumer?, isReRender: Boolean, partialTick: Float, packedLight: Int, packedOverlay: Int, colour: Int) {
        if (!isReRender) run {
            val level = animatable.level ?: return@run
            val belowPos = animatable.blockPos.below()
            val below = level.getBlockState(belowPos) ?: return@run
            if (below.isAir) return@run
            val belowShape = below.getShape(level, belowPos)

            if (belowShape.isEmpty) return@run
            val underShape = below.getShape(level, belowPos)
            if (underShape.isEmpty) return@run
            val seatHeight = underShape.toAabbs()
                .filter { it.minX <= 0.5 && it.maxX >= 0.5 && it.minZ <= 0.5 && it.maxZ >= 0.5 }
                .maxOfOrNull { it.maxY }
                ?: underShape.bounds().maxY.coerceAtMost(1.0)
            poseStack.translate(0.0, seatHeight - 1.0, 0.0)
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour)
    }

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