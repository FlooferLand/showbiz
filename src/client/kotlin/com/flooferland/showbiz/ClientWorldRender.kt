package com.flooferland.showbiz

import com.flooferland.showbiz.entities.DecorEntity
import com.flooferland.showbiz.renderers.*
import com.mojang.blaze3d.vertex.PoseStack
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.renderer.MultiBufferSource

object ClientWorldRender {
    init {
        WorldRenderEvents.AFTER_ENTITIES.register { ctx ->
            render(ctx) { mc, _, _, buffer, poseStack, partialTick ->
                StackPlacePreviewRenderer.render(mc, buffer, poseStack, partialTick)
            }
        }

        WorldRenderEvents.LAST.register { ctx ->
            render(ctx) { mc, level, player, buffer, poseStack, partialTick ->
                JukeboxLyricRenderer.render(level, player, buffer, poseStack)
                CollidePartEntityRenderer.render(mc, buffer, poseStack)
                ModelPartEntityRenderer.render(mc, buffer, poseStack)
                CurtainBlockEntityRenderer.render(mc, buffer, poseStack)
                BotEntityRenderer.render(mc, buffer, poseStack)
                ConnectionRenderer.draw(player, buffer, partialTick)
                ConnectionRenderer.renderDeferred(poseStack)
                DecorEntity.moveAll(ctx)
            }
        }
    }

    private inline fun render(
        ctx: WorldRenderContext?,
        action: (Minecraft, ClientLevel, LocalPlayer, MultiBufferSource.BufferSource, PoseStack, Float) -> Unit
    ) {
        if (ctx == null) return
        val mc = Minecraft.getInstance() ?: return
        val player = mc.player ?: return
        val level = player.clientLevel ?: return
        val partialTick = ctx.tickCounter().getGameTimeDeltaPartialTick(true)
        val cam = ctx.camera() ?: return
        val poseStack = ctx.matrixStack() ?: return
        val buffer = mc.renderBuffers().bufferSource()

        poseStack.pushPose()
        poseStack.translate(-cam.position.x, -cam.position.y, -cam.position.z)
        try {
            action(mc, level, player, buffer, poseStack, partialTick)
        } catch (throwable: Throwable) {
            Showbiz.log.error("Render error", throwable)
        }
        buffer.endBatch()
        poseStack.popPose()
    }
}