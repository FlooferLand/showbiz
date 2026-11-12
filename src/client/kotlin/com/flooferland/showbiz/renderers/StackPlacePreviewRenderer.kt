package com.flooferland.showbiz.renderers

import com.flooferland.showbiz.types.IStackPlacePreview
import com.flooferland.showbiz.utils.Extensions.getHeldItem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.BlockHitResult
import kotlin.math.roundToInt

object StackPlacePreviewRenderer {
    private var lastStack: ItemStack = ItemStack.EMPTY
    private var cachedEntity: Entity? = null

    fun render(mc: Minecraft, buffer: MultiBufferSource.BufferSource, poseStack: PoseStack, partialTicks: Float) {
        val player = mc.player ?: return
        val level = mc.level ?: return
        val heldStack = player.getHeldItem { it.item is IStackPlacePreview } ?: run {
            if (!lastStack.isEmpty && player.getHeldItem { it != lastStack } != null) {
                player.displayClientMessage(Component.empty(), true)
            }
            lastStack = ItemStack.EMPTY
            return
        }
        val heldItem = heldStack.item as? IStackPlacePreview ?: return

        val hitResult = mc.hitResult as? BlockHitResult ?: return
        val blockPos = hitResult.blockPos ?: return

        var entity = cachedEntity
        if (entity == null || heldStack != lastStack) {
            entity = heldItem.getEntity(level, heldStack)
        }
        heldItem.placeEntity(entity, hitResult.location,  player)
        cachedEntity = entity

        val voxelX = ((entity.x - blockPos.x) * 16.0).roundToInt().coerceIn(0, 16)
        val voxelZ = ((entity.z - blockPos.z) * 16.0).roundToInt().coerceIn(0, 16)
        player.displayClientMessage(
            Component.literal("x:$voxelX z:$voxelZ rot:${entity.yRot % 180}"),
            true
        )

        // :3
        val texture = mc.entityRenderDispatcher.getRenderer(entity).getTextureLocation(entity)
        val transType = net.minecraft.client.renderer.RenderType.entityTranslucent(texture)
        val transBuffer = MultiBufferSource { _ ->
            val inner = buffer.getBuffer(transType)
            object : VertexConsumer by inner {
                override fun setColor(r: Int, g: Int, b: Int, a: Int): com.mojang.blaze3d.vertex.VertexConsumer {
                    return inner.setColor(r, g, b, (a * 0.5f).toInt())
                }
            }
        }

        poseStack.pushPose()
        poseStack.translate(entity.x, entity.y, entity.z)
        mc.entityRenderDispatcher.render(
            entity,
            0.0, 0.0, 0.0,
            0f,
            partialTicks, poseStack, transBuffer,
            LevelRenderer.getLightColor(level, blockPos.above())
        )
        poseStack.popPose()
        lastStack = heldStack
    }
}