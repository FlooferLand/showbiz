package com.flooferland.showbiz.renderers

import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.entity.*
import com.flooferland.showbiz.entities.BotPartEntity
import com.mojang.blaze3d.vertex.PoseStack

class BotPartEntityRenderer(val context: EntityRendererProvider.Context) : EntityRenderer<BotPartEntity>(context) {
    override fun getTextureLocation(entity: BotPartEntity) = null
    override fun render(entity: BotPartEntity, entityYaw: Float, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int) {

    }
}