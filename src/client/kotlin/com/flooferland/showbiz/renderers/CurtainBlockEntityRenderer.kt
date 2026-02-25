package com.flooferland.showbiz.renderers

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.blockentity.*
import net.minecraft.util.FastColor
import net.minecraft.world.phys.*
import com.flooferland.showbiz.blocks.CurtainBlock
import com.flooferland.showbiz.blocks.entities.CurtainBlockEntity
import com.flooferland.showbiz.utils.DrawUtils
import com.flooferland.showbiz.utils.rl
import com.flooferland.showbiz.utils.voxelSnap
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import kotlin.math.abs
import kotlin.math.sin

class CurtainBlockEntityRenderer(val context: BlockEntityRendererProvider.Context) : BlockEntityRenderer<CurtainBlockEntity> {
    val curtainRenderType = RenderType.entityTranslucent(rl("textures/block/curtain_block.png"))!!
    val curtainRenderTypeCull = RenderType.entityTranslucentCull(rl("textures/block/curtain_block.png"))!!
    val endRenderType = RenderType.entityTranslucent(rl("textures/block/curtain_block_end.png"))!!
    val endRenderTypeCull = RenderType.entityTranslucentCull(rl("textures/block/curtain_block_end.png"))!!

    override fun render(blockEntity: CurtainBlockEntity, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int) {
        val level = blockEntity.level ?: return
        var color = blockEntity.color // 0xff6767

        val (drawSouth, drawNorth, drawWest, drawEast) = arrayOf(
            level.getBlockState(blockEntity.blockPos.south())?.block !is CurtainBlock,
            level.getBlockState(blockEntity.blockPos.north())?.block !is CurtainBlock,
            level.getBlockState(blockEntity.blockPos.west())?.block !is CurtainBlock,
            level.getBlockState(blockEntity.blockPos.east())?.block !is CurtainBlock
        )
        val randomY = abs(sin((blockEntity.blockPos.x + blockEntity.blockPos.y + blockEntity.blockPos.z).toFloat()))
        val randomColor = FastColor.ARGB32.color(255 - (randomY * 30).toInt(), 255 - (randomY * 30).toInt(), 255 - (randomY * 30).toInt())
        color = FastColor.ARGB32.multiply(color, randomColor)

        // TODO: Add a nicer open animation to the curtains
        if (blockEntity.isOpen) {
            poseStack.pushPose()
            poseStack.translate(0f, 0.0f, 0f)
            DrawUtils.drawBox(
                poseStack, bufferSource.getBuffer(curtainRenderType),
                AABB(0.0, 0.5, 0.0, 1.0, 1.0, 1.0),
                packedLight = packedLight,
                packedOverlay = packedOverlay,
                sidesOnly = true,
                color = color,
                drawSouth = drawSouth,
                drawNorth = drawNorth,
                drawWest = drawWest,
                drawEast = drawEast
            )
            DrawUtils.drawBox(
                poseStack, bufferSource.getBuffer(endRenderType),
                AABB(0.0, 0.0, 0.0, 1.0, 0.5, 1.0),
                packedLight = packedLight,
                packedOverlay = packedOverlay,
                sidesOnly = true,
                drawSouth = drawSouth,
                drawNorth = drawNorth,
                drawWest = drawWest,
                drawEast = drawEast
            )
            poseStack.popPose()
            return
        }

        val length = blockEntity.findLength()
        val box = AABB(0.0, 1.0, 0.0, 1.0, (length * -1f).toDouble(), 1.0)
        val camera = Minecraft.getInstance().gameRenderer.mainCamera
        val worldBox = box.move(blockEntity.blockPos)
        val playerInside = worldBox.inflate(0.4).contains(camera.position)
        poseStack.pushPose()
        DrawUtils.drawBox(
            poseStack, if (camera.position.y > worldBox.maxY) bufferSource.getBuffer(curtainRenderType) else bufferSource.getBuffer(curtainRenderTypeCull),
            box,
            packedLight = packedLight,
            packedOverlay = packedOverlay,
            sidesOnly = true,
            color = color,
            alpha = if (playerInside) 0.5f else null,
            drawSouth = drawSouth,
            drawNorth = drawNorth,
            drawWest = drawWest,
            drawEast = drawEast
        )
        poseStack.translate(0f, voxelSnap(randomY * 0.05f), 0f)
        DrawUtils.drawBox(
            poseStack, bufferSource.getBuffer(endRenderTypeCull),
            AABB(0.0, (1f - length).toDouble() - 1.0, 0.0, 1.0, (1f - length).toDouble() - 1.5, 1.0).inflate(0.001),
            packedLight = packedLight,
            packedOverlay = packedOverlay,
            sidesOnly = true,
            alpha = if (playerInside) 0.5f else null,
            drawSouth = drawSouth,
            drawNorth = drawNorth,
            drawWest = drawWest,
            drawEast = drawEast
        )
        poseStack.popPose()
    }
}