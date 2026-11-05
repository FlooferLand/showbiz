package com.flooferland.showbiz.renderers

import com.flooferland.showbiz.entities.ModelPartEntity
import com.flooferland.showbiz.utils.DrawUtils
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.world.entity.Entity

class ModelPartEntityRenderer(val context: EntityRendererProvider.Context) : EntityRenderer<ModelPartEntity>(context) {
    override fun getTextureLocation(entity: ModelPartEntity) = null
    override fun render(entity: ModelPartEntity, entityYaw: Float, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int) {}

    companion object {
        var prevPick: Entity? = null
        var pickZoom = 0f
        fun render(mc: Minecraft, buffer: MultiBufferSource.BufferSource, poseStack: PoseStack) {
            val entity = mc.crosshairPickEntity as? ModelPartEntity ?: run {
                prevPick = null
                pickZoom = 0f
                return
            }

            val text = entity.getName()
            DrawUtils.drawText(poseStack, buffer, entity.position(), text, scale = 0.8f - pickZoom)

            if (prevPick?.id != entity.id) {
                pickZoom = 0.1f
                prevPick = entity
            } else if (pickZoom > 0f) {
                pickZoom = (pickZoom - (mc.timer.realtimeDeltaTicks * 0.1f)).coerceAtLeast(0f)
            }
        }
    }
}