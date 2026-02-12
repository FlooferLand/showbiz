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
        val rotationAngle = run {
            val target = entity.seek.toFloat() * 200f
            state.visualSeek += (target - state.visualSeek)
            state.visualSeek
        }

        // Rendering a temporary reel holder
        runCatching {
            val model = Minecraft.getInstance().modelManager.getModel(ModelResourceLocation(rlVanilla("polished_blackstone_pressure_plate"), "inventory")) ?: return@runCatching
            poseStack.pushPose()
            poseStack.translate(0.5, 1.43, 0.5)
            poseStack.scale(0.86f, 0.86f, 0.86f)
            Minecraft.getInstance().itemRenderer.render(
                Items.POLISHED_BLACKSTONE_PRESSURE_PLATE.defaultInstance,
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
        runCatching {
            val model = Minecraft.getInstance().modelManager.getModel(ModelResourceLocation(rlVanilla("polished_blackstone"), "inventory")) ?: return@runCatching
            poseStack.pushPose()
            poseStack.translate(0.5, 1.1, 0.5)
            poseStack.scale(0.1f, 0.2f, 0.1f)
            poseStack.mulPose(Quaternionf().fromAxisAngleDeg(Vector3f(0f, 1f, 0f), rotationAngle))
            Minecraft.getInstance().itemRenderer.render(
                Items.POLISHED_BLACKSTONE.defaultInstance,
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

        // Rendering the reel
        runCatching {
            val itemStack = ModItems.Reel.item.defaultInstance
            val model = Minecraft.getInstance().modelManager.getModel(ModelResourceLocation(ModItems.Reel.id, "inventory")) ?: return@runCatching
            if (entity.show.isLoaded) {
                poseStack.pushPose()
                poseStack.translate(0.5, 1.555, 0.5)
                poseStack.mulPose(Quaternionf().fromAxisAngleDeg(Vector3f(0f, 1f, 0f), rotationAngle))
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