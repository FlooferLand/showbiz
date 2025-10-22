package com.flooferland.showbiz.renderers

import com.flooferland.showbiz.blocks.entities.PlaybackControllerBlockEntity
import com.flooferland.showbiz.registry.ModItems
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.*
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.blockentity.*
import net.minecraft.client.resources.model.*
import net.minecraft.world.item.*
import org.joml.Quaternionf
import org.joml.Vector3f

class PlaybackBlockEntityRenderer(val context: BlockEntityRendererProvider.Context) : BlockEntityRenderer<PlaybackControllerBlockEntity> {
    override fun render(entity: PlaybackControllerBlockEntity, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int) {
        val itemStack = ModItems.Reel.item.defaultInstance
        val model = Minecraft.getInstance().modelManager.getModel(ModelResourceLocation(ModItems.Reel.id, "inventory")) ?: return

        if (entity.show.isLoaded) {
            val time = (entity.level?.gameTime?.toFloat() ?: 0f) + partialTick
            poseStack.pushPose()
            poseStack.translate(0.5, 1.55, 0.5)
            if (entity.playing) {
                poseStack.mulPose(Quaternionf().fromAxisAngleDeg(Vector3f(0f, 1f, 0f), time * 5.0f))
            }
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