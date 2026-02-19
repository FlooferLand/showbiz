package com.flooferland.showbiz.renderers

import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.blockentity.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.blocks.entities.CurtainBlockEntity
import com.flooferland.showbiz.utils.DrawUtils
import com.flooferland.showbiz.utils.rl
import com.mojang.blaze3d.vertex.PoseStack
import kotlin.math.sin

class CurtainBlockEntityRenderer(val context: BlockEntityRendererProvider.Context) : BlockEntityRenderer<CurtainBlockEntity> {
    override fun render(blockEntity: CurtainBlockEntity, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int) {
        val level = blockEntity.level ?: return
        val mainConsumer = bufferSource.getBuffer(RenderType.entitySmoothCutout(rl("textures/block/curtain_block.png")))
        val endConsumer = bufferSource.getBuffer(RenderType.entitySmoothCutout(rl("textures/block/curtain_block_end.png")))
        val withEndConsumer = bufferSource.getBuffer(RenderType.entitySmoothCutout(rl("textures/block/curtain_block_with_end.png")))
        val color = 0xff6767

        // TODO: Add a nicer open animation to the curtains
        if (blockEntity.isOpen) {
            poseStack.pushPose()
            poseStack.translate(0f, 0.0f, 0f)
            DrawUtils.drawBox(
                poseStack, withEndConsumer,
                AABB(0.0, 0.5, 0.0, 1.0, 1.0, 1.0),
                packedLight = packedLight,
                packedOverlay = packedOverlay,
                sidesOnly = true,
                color = color
            )
            DrawUtils.drawBox(
                poseStack, endConsumer,
                AABB(0.0, 0.0, 0.0, 1.0, 0.5, 1.0),
                packedLight = packedLight,
                packedOverlay = packedOverlay,
                sidesOnly = true
            )
            poseStack.popPose()
            return
        }

        val end = 6
        var isLast = false
        for (y in 0..end) {
            for (y2 in 1..2) {
                val below = blockEntity.blockPos.below(y + y2)
                if ((level.getBlockState(below)?.isSolidRender(level, below) ?: true)) {
                    isLast = true
                    break
                }
            }

            poseStack.pushPose()
            poseStack.translate(0f, y * -1f, 0f)
            DrawUtils.drawBox(
                poseStack, mainConsumer,
                AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0),
                packedLight = packedLight,
                packedOverlay = packedOverlay,
                color = color
            )
            if (isLast || y == end) {
                poseStack.pushPose()
                poseStack.translate(0f, -0.5f, 0f)
                DrawUtils.drawBox(
                    poseStack, endConsumer,
                    AABB(0.0, 0.0, 0.0, 1.0, 0.5, 1.0),
                    packedLight = packedLight,
                    packedOverlay = packedOverlay,
                    sidesOnly = true
                )
                poseStack.popPose()
            }
            poseStack.popPose()

            if (isLast) break
        }
    }
}