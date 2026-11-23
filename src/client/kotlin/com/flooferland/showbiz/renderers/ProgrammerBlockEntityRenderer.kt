package com.flooferland.showbiz.renderers

import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.blocks.entities.ProgrammerBlockEntity
import com.flooferland.showbiz.utils.DrawUtils
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.Vec3

class ProgrammerBlockEntityRenderer(ctx: BlockEntityRendererProvider.Context) : BlockEntityRenderer<ProgrammerBlockEntity> {
    override fun render(blockEntity: ProgrammerBlockEntity, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int) {
        val facing = blockEntity.blockState.getValue(FacingEntityBlock.FACING) ?: return

        if (blockEntity.operators.isNotEmpty()) {
            val text = Component.literal("Operators:\n").withStyle(ChatFormatting.GRAY)
            for (operator in blockEntity.operators) {
                text.append(operator.name.copy().withStyle(ChatFormatting.WHITE))
            }
            poseStack.pushPose()
            poseStack.translate(0.5, 0.0, 0.5)
            poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 180f))
            poseStack.translate(-0.5, 0.0, -0.5)
            DrawUtils.drawText(poseStack, Vec3(0.305, 0.7, 0.495), text) {
                scale = 0.35f
                rotation = 180f
                billboard = false
                drawBackground = false
                depth = true
                vAlign = 0f
            }
            poseStack.popPose()
        }
    }

    companion object {
        fun render(mc: Minecraft, buffer: MultiBufferSource.BufferSource, poseStack: PoseStack) {

        }
    }
}
