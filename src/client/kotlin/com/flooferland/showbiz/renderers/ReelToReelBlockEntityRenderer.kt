package com.flooferland.showbiz.renderers

import net.minecraft.client.*
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.blockentity.*
import net.minecraft.client.resources.model.*
import net.minecraft.world.item.*
import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.blocks.entities.ReelToReelBlockEntity
import com.flooferland.showbiz.registry.ModItems
import com.flooferland.showbiz.utils.rlVanilla
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import org.joml.Quaternionf
import org.joml.Vector3f

class ReelToReelBlockEntityRenderer(val context: BlockEntityRendererProvider.Context) : BlockEntityRenderer<ReelToReelBlockEntity> {
    override fun render(entity: ReelToReelBlockEntity, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int) {
        val facing = entity.blockState.getValue(FacingEntityBlock.FACING) ?: return

        // Time
        val rotationAngle = entity.seek.toFloat() * 200f

        poseStack.pushPose()
        poseStack.translate(0.5, 0.0, 0.5)
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 180f))
        poseStack.translate(-0.5, 0.0, -0.5)

        // Recording
        run {
            val minecraft = Minecraft.getInstance() ?: return@run
            val modelId = if (entity.recording) rlVanilla("redstone_block") else rlVanilla("gray_concrete")
            val item = if (entity.recording) Items.REDSTONE_BLOCK else Items.GRAY_CONCRETE
            val recordModel = minecraft.modelManager.getModel(ModelResourceLocation(modelId, "inventory")) ?: return@run
            poseStack.pushPose()
            poseStack.translate(0.5, 0.0, 0.5)
            poseStack.translate(0.275, 0.4, -0.15)
            poseStack.scale(0.07f, 0.07f, 0.07f)
            minecraft.itemRenderer.render(
                item.defaultInstance,
                ItemDisplayContext.NONE,
                false,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                recordModel
            )

            poseStack.pushPose()
            poseStack.translate(0.25, 0.3, -0.53)
            poseStack.scale(0.08f, -0.08f, 0.08f)
            poseStack.mulPose(Axis.YP.rotationDegrees(180f))
            minecraft.font.drawInBatch("R", 0f, 0f, 0xFF_FF_FF_FF.toInt(), false, poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0x00_00_00_00, packedLight)
            poseStack.popPose()
            poseStack.popPose()
        }

        // Rendering the reel bands
        // NOTE: Innacurate, reel to reels have the reels going to separate reel pickup points, not to each-other
        /*if (entity.show.isLoaded) run {
            val reelBand = Minecraft.getInstance().modelManager.getModel(ModelResourceLocation(rlVanilla("gray_concrete"), "inventory")) ?: return@run
            for (i in 0..1) {
                val offset = if (i == 0) 0.9 else 0.55
                poseStack.pushPose()
                poseStack.translate(0.5, offset, 0.3)
                poseStack.scale(0.5f, 0.01f, 0.038f)
                Minecraft.getInstance().itemRenderer.render(
                    Items.GRAY_CONCRETE.defaultInstance,
                    ItemDisplayContext.NONE,
                    false,
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    reelBand
                )
                poseStack.popPose()
            }
        }*/

        // Rendering the reels
        for (i in 0..1) {
            val offset = if (i == 0) 1f else -1f
            runCatching {
                val itemStack = ModItems.Reel.item.defaultInstance
                val model = Minecraft.getInstance().modelManager.getModel(ModelResourceLocation(ModItems.Reel.id, "inventory")) ?: return@runCatching
                if (entity.showData.isLoaded || i == 1) {
                    poseStack.pushPose()
                    poseStack.translate(0.5 + (offset * 0.3), 0.7, 0.58)
                    poseStack.scale(0.6f, 0.6f, 0.6f)
                    poseStack.mulPose(Quaternionf().fromAxisAngleDeg(Vector3f(0f, 0f, 1f), -rotationAngle))
                    poseStack.mulPose(Quaternionf().fromAxisAngleDeg(Vector3f(1f, 0f, 0f), 90.0f))
                    Minecraft.getInstance().itemRenderer.render(
                        itemStack,
                        ItemDisplayContext.NONE,
                        false,
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay,
                        model
                    )
                    poseStack.popPose()
                }
            }.onFailure { poseStack.popPose() }
        }
        poseStack.popPose()
    }
}