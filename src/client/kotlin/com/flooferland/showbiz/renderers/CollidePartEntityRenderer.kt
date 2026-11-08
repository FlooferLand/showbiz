package com.flooferland.showbiz.renderers

import com.flooferland.showbiz.entities.CollidePartEntity
import com.flooferland.showbiz.utils.DrawUtils
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity

class CollidePartEntityRenderer(val context: EntityRendererProvider.Context) : EntityRenderer<CollidePartEntity>(context) {
    override fun getTextureLocation(entity: CollidePartEntity) = null
    override fun render(entity: CollidePartEntity, entityYaw: Float, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int) {}

    companion object {
        var prevPick: Entity? = null
        var pickZoom = 0f
        fun render(mc: Minecraft, buffer: MultiBufferSource.BufferSource, poseStack: PoseStack) {
            if (mc.debugOverlay?.showDebugScreen() != true) return
            val entity = mc.crosshairPickEntity as? CollidePartEntity ?: run {
                prevPick = null
                pickZoom = 0f
                return
            }

            val text = Component.literal(entity.partId.toString())
            DrawUtils.drawText(poseStack, entity.position(), text, scale = 0.8f - pickZoom, buffer = buffer)

            if (prevPick?.id != entity.id) {
                pickZoom = 0.1f
                prevPick = entity
            } else if (pickZoom > 0f) {
                pickZoom = (pickZoom - (mc.timer.realtimeDeltaTicks * 0.1f)).coerceAtLeast(0f)
            }
        }
    }
}