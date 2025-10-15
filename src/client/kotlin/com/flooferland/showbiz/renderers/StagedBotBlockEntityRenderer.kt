package com.flooferland.showbiz.renderers

import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity
import com.flooferland.showbiz.models.StagedBotBlockEntityModel
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.world.level.LightLayer
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
        super.render(animatable, partialTick, poseStack, bufferSource, sampled, packedOverlay)
    }
}