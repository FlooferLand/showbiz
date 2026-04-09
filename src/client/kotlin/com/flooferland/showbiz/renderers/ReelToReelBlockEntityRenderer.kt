package com.flooferland.showbiz.renderers

import com.flooferland.showbiz.blocks.entities.ReelToReelBlockEntity
import com.flooferland.showbiz.registry.ModItems
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.*
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.blockentity.*
import net.minecraft.client.resources.model.*
import net.minecraft.world.item.*
import com.flooferland.showbiz.utils.lerp
import com.flooferland.showbiz.utils.rlVanilla
import org.joml.Quaternionf
import org.joml.Vector3f

class ReelToReelBlockEntityRenderer(val context: BlockEntityRendererProvider.Context) : BlockEntityRenderer<ReelToReelBlockEntity> {
    override fun render(entity: ReelToReelBlockEntity, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int) {
        val state = entity.rendererState
        val isAirAbove = entity.level?.getBlockState(entity.blockPos.above())?.isAir ?: true
        if (!isAirAbove) {
            return
        }

        // Time
        val rotationAngle = entity.seek.toFloat() * 200f

        // Rendering the reel
        for (i in 0..1) {
            val offset = if (i == 0) -1f else 1f
            runCatching {
                val itemStack = ModItems.Reel.item.defaultInstance
                val model = Minecraft.getInstance().modelManager.getModel(ModelResourceLocation(ModItems.Reel.id, "inventory")) ?: return@runCatching
                if (entity.show.isLoaded) {
                    poseStack.pushPose()
                    poseStack.translate(0.5 + (offset * 0.3), 0.7, 0.58)
                    poseStack.scale(0.6f, 0.6f, 0.6f)
                    poseStack.mulPose(Quaternionf().fromAxisAngleDeg(Vector3f(0f, 0f, 1f), rotationAngle))  // Real
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
            }
        }
    }
}