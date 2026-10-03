package com.flooferland.showbiz.renderers

import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.culling.*
import net.minecraft.client.renderer.entity.*
import com.flooferland.showbiz.entities.SeatEntity
import com.mojang.blaze3d.vertex.PoseStack

class SeatEntityRenderer(ctx: EntityRendererProvider.Context) : EntityRenderer<SeatEntity>(ctx) {
    override fun getTextureLocation(entity: SeatEntity) = null
    override fun shouldRender(livingEntity: SeatEntity, camera: Frustum, camX: Double, camY: Double, camZ: Double) = true
    override fun render(entity: SeatEntity, entityYaw: Float, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int) {}
}