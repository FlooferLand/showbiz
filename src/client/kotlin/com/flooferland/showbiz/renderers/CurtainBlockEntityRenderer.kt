package com.flooferland.showbiz.renderers

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.blockentity.*
import net.minecraft.core.BlockPos
import net.minecraft.util.FastColor
import net.minecraft.world.phys.*
import com.flooferland.showbiz.blocks.CurtainBlock
import com.flooferland.showbiz.blocks.entities.CurtainBlockEntity
import com.flooferland.showbiz.utils.DrawUtils
import com.flooferland.showbiz.utils.rl
import com.flooferland.showbiz.utils.voxelSnap
import com.mojang.blaze3d.vertex.PoseStack
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

class CurtainBlockEntityRenderer(val context: BlockEntityRendererProvider.Context) : BlockEntityRenderer<CurtainBlockEntity> {
    val curtainRenderType = RenderType.entityTranslucent(rl("textures/block/curtain_block.png"))!!
    val curtainRenderTypeCull = RenderType.entityTranslucentCull(rl("textures/block/curtain_block.png"))!!
    val endRenderType = RenderType.entityTranslucent(rl("textures/block/curtain_block_end.png"))!!
    val endRenderTypeCull = RenderType.entityTranslucentCull(rl("textures/block/curtain_block_end.png"))!!

    fun isVisuallyOpen(pos: BlockPos, center: BlockPos, openAmount: Float, maxDist: Float): Boolean {
        val dist = maxOf(
            abs(pos.x - center.x),
            maxOf(abs(pos.y - center.y), abs(pos.z - center.z))
        ).toFloat()
        val movingWaveMax = maxOf(0f, maxDist - 2f) + 0.1f
        val wavePos = openAmount * movingWaveMax
        return wavePos > dist && dist < (maxDist - 1.5f)
    }

    fun canDrawColumn(level: ClientLevel, blockEntity: CurtainBlockEntity, pos: BlockPos, center: BlockPos?, maxDist: Float) = level.getBlockState(pos)?.let { state ->
        if (state.block !is CurtainBlock) return@let !state.isSolidRender(level, pos)
        if (center == null) return@let false

        val isOpen = isVisuallyOpen(blockEntity.blockPos, center, blockEntity.openAmount, maxDist)
        val neighbourOpen = isVisuallyOpen(pos, center, blockEntity.openAmount, maxDist)
        return@let isOpen != neighbourOpen
    } ?: true

    override fun render(blockEntity: CurtainBlockEntity, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int) {
        val level = blockEntity.level as? ClientLevel ?: return
        var color = blockEntity.color

        val rails = blockEntity.findConnectedCurtains()
        val center = blockEntity.findCenter(rails)
        var isOpen = false
        var maxDist = 1f
        if (center != null) {
            maxDist = rails.maxOfOrNull { maxOf(abs(it.x - center.x), maxOf(abs(it.y - center.y), abs(it.z - center.z))) }?.toFloat() ?: 1f
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
            DrawUtils.drawBox(
                poseStack, bufferSource.getBuffer(curtainRenderType),
                AABB(0.0, 0.5, 0.0, 1.0, 1.0, 1.0),
                packedLight = packedLight,
                packedOverlay = packedOverlay,
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