package com.flooferland.showbiz.renderers

import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity
import com.flooferland.showbiz.models.StagedBotBlockEntityModel
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.blockentity.*
import net.minecraft.world.level.*
import software.bernie.geckolib.renderer.GeoBlockRenderer

class StagedBotBlockEntityRenderer(val context: BlockEntityRendererProvider.Context) : GeoBlockRenderer<StagedBotBlockEntity>(StagedBotBlockEntityModel()) {
    override fun render(animatable: StagedBotBlockEntity, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int) {
        val level = animatable.level
        val sampled = if (level != null) {
            val samplePos = animatable.blockPos.above()
            val blockLight = level.getBrightness(LightLayer.BLOCK, samplePos)
            val skyLight = level.getBrightness(LightLayer.SKY, samplePos)
            LightTexture.pack(blockLight, skyLight)
        } else {
            LightTexture.FULL_BRIGHT
        }

        poseStack.pushPose()
        poseStack.translate(0.0, 1.0, 0.0)
        super.render(animatable, partialTick, poseStack, bufferSource, sampled, packedOverlay)
        poseStack.popPose()
    }
}