package com.flooferland.showbiz.renderers

import net.minecraft.client.*
import net.minecraft.client.multiplayer.*
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.blockentity.*
import net.minecraft.core.*
import net.minecraft.util.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.blocks.CurtainBlock
import com.flooferland.showbiz.blocks.entities.CurtainBlockEntity
import com.flooferland.showbiz.utils.DrawUtils
import com.flooferland.showbiz.utils.rl
import com.flooferland.showbiz.utils.voxelSnap
import com.mojang.blaze3d.vertex.PoseStack
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

class CurtainBlockEntityRenderer(val context: BlockEntityRendererProvider.Context) : BlockEntityRenderer<CurtainBlockEntity> {
    val curtainRenderType = RenderType.entityTranslucent(rl("textures/block/curtain_block.png"))!!
    val curtainRenderTypeCull = RenderType.entityTranslucentCull(rl("textures/block/curtain_block.png"))!!
    val endRenderType = RenderType.entityTranslucent(rl("textures/block/curtain_block_end.png"))!!
    val endRenderTypeCull = RenderType.entityTranslucentCull(rl("textures/block/curtain_block_end.png"))!!

    fun isVisuallyOpen(pos: BlockPos, center: BlockPos, openAmount: Float, maxDist: Double): Boolean {
        val dist = maxOf(
            abs(pos.x - center.x),
            maxOf(abs(pos.y - center.y), abs(pos.z - center.z))
        ).toFloat()
        val wavePos = openAmount * (maxDist + 0.1f)
        return wavePos > dist
    }

    fun canDrawColumn(level: ClientLevel, blockEntity: CurtainBlockEntity, neighborPos: BlockPos, center: BlockPos?, maxDist: Double) = level.getBlockState(neighborPos)?.let { state ->
        if (state.block !is CurtainBlock) return@let true
        if (center == null) return@let false

        val isOpen = isVisuallyOpen(blockEntity.blockPos, center, blockEntity.openAmount, maxDist)
        val neighbourOpen = isVisuallyOpen(neighborPos, center, blockEntity.openAmount, maxDist)
        return@let isOpen != neighbourOpen
    } ?: true

    override fun render(blockEntity: CurtainBlockEntity, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int) {
        val level = blockEntity.level as? ClientLevel ?: return
        val fastGraphics = Minecraft.getInstance()?.options?.graphicsMode()?.get() == GraphicsStatus.FAST
        var color = blockEntity.color

        val rails = blockEntity.connectedCurtains
        val center = blockEntity.centerCurtain
        var isOpen = false
        var maxDist = 1.0
        var dist = 0.0
        if (center != null) {
            val pos = blockEntity.blockPos
            dist = maxOf(abs(pos.x - center.x), abs(pos.y - center.y), abs(pos.z - center.z)).toDouble()
            maxDist = rails.maxOfOrNull { maxOf(abs(it.x - center.x), abs(it.y - center.y), abs(it.z - center.z)) }?.toDouble() ?: 1.0
            isOpen = isVisuallyOpen(blockEntity.blockPos, center, blockEntity.openAmount, maxDist)
        }

        val (drawSouth, drawNorth, drawWest, drawEast) = arrayOf(
            canDrawColumn(level, blockEntity, blockEntity.blockPos.south(), center, maxDist),
            canDrawColumn(level, blockEntity, blockEntity.blockPos.north(), center, maxDist),
            canDrawColumn(level, blockEntity, blockEntity.blockPos.west(), center, maxDist),
            canDrawColumn(level, blockEntity, blockEntity.blockPos.east(), center, maxDist)
        )
        val randomY = abs(sin((blockEntity.blockPos.x + blockEntity.blockPos.y + blockEntity.blockPos.z).toFloat()))
        val randomColor = FastColor.ARGB32.color(255 - (randomY * 30).toInt(), 255 - (randomY * 30).toInt(), 255 - (randomY * 30).toInt())
        color = FastColor.ARGB32.multiply(color, randomColor)

        if (isOpen) {
            poseStack.pushPose()
            poseStack.translate(0f, 0.0f, 0f)

            var lower = 0.1
            if (!fastGraphics) {
                lower += (dist / maxDist).pow(3.0) * 0.4
                lower = lower.coerceIn(0.1, 0.4)
            }
            DrawUtils.drawBox(
                poseStack, bufferSource.getBuffer(curtainRenderType),
                AABB(0.0, 0.5, 0.0, 1.0, 1.0, 1.0).expandTowards(0.0, -lower, 0.0),
                packedLight = packedLight,
                packedOverlay = packedOverlay,
                color = color,
                drawSouth = drawSouth || !fastGraphics,
                drawNorth = drawNorth || !fastGraphics,
                drawWest = drawWest || !fastGraphics,
                drawEast = drawEast || !fastGraphics
            )
            DrawUtils.drawBox(
                poseStack, bufferSource.getBuffer(endRenderType),
                AABB(0.0, 0.0, 0.0, 1.0, 0.5, 1.0).deflate(0.01, 0.0, 0.01).move(0.0, -lower, 0.0),
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
            color = color,
            alpha = if (playerInside) 0.5f else null,
            drawSouth = drawSouth,
            drawNorth = drawNorth,
            drawWest = drawWest,
            drawEast = drawEast,
            drawTop = camera.position.y > worldBox.maxY,
            drawBottom = camera.position.y < worldBox.minY
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