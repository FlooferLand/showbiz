package com.flooferland.showbiz.renderers

import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.multiplayer.*
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.blockentity.*
import net.minecraft.core.*
import net.minecraft.network.chat.*
import net.minecraft.util.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.blocks.CurtainBlock
import com.flooferland.showbiz.blocks.entities.CurtainBlockEntity
import com.flooferland.showbiz.utils.DrawUtils
import com.flooferland.showbiz.utils.rl
import com.flooferland.showbiz.utils.voxelSnap
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
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

    fun cutoff(level: ClientLevel, neighborPos: BlockPos): Double {
        if (level.getBlockState(neighborPos).block !is CurtainBlock) return Double.MAX_VALUE
        return (level.getBlockEntity(neighborPos) as? CurtainBlockEntity)?.bottom ?: Double.MAX_VALUE
    }

    override fun render(blockEntity: CurtainBlockEntity, partialTick: Float, pose: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int) {
        val level = blockEntity.level as? ClientLevel ?: return
        val minecraft = Minecraft.getInstance() ?: return
        val fastGraphics = minecraft.options?.graphicsMode()?.get() == GraphicsStatus.FAST

        if (minecraft.player?.isCrouching == true) {
            val hitResult = minecraft.hitResult
            val hovering = hitResult is BlockHitResult && hitResult.blockPos == blockEntity.blockPos
            val curtains = blockEntity.findConnectedCurtains(ignoreGroups = true, affectWorld = false)
            if (hovering || curtains.any { hitResult is BlockHitResult && hitResult.blockPos == it }) {
                val text = Component.literal("Group ${blockEntity.group}").withColor(blockEntity.getGroupColor())
                pose.pushPose()
                pose.translate(0.5, 0.8, 0.5)
                pose.mulPose(minecraft.gameRenderer.mainCamera.rotation())
                pose.mulPose(Axis.XP.rotation(Mth.PI))
                pose.scale(0.01f, 0.01f, 0.01f)
                val yOffset = 10f
                for ((i, line) in arrayOf(Component.literal("/\\"), text, Component.literal("\\/")).withIndex()) {
                    minecraft.font.drawInBatch(line, -minecraft.font.width(line) / 2f, (i * yOffset) - (yOffset / 2f), 0xFFFFFFFF.toInt(), false, pose.last().pose(), bufferSource, Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT)
                }
                pose.popPose()
            }
        }

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

        val cuts = doubleArrayOf(
            cutoff(level, blockEntity.blockPos.south()),
            cutoff(level, blockEntity.blockPos.north()),
            cutoff(level, blockEntity.blockPos.west()),
            cutoff(level, blockEntity.blockPos.east())
        )

        val alpha = if (fastGraphics) 255 else 250
        val randomY = abs(sin((blockEntity.blockPos.x + blockEntity.blockPos.y + blockEntity.blockPos.z).toFloat()))
        val randomColor = FastColor.ARGB32.color(255 - (randomY * 30).toInt(), 255 - (randomY * 30).toInt(), 255 - (randomY * 30).toInt())
        val color = FastColor.ARGB32.multiply(blockEntity.color, randomColor).let { FastColor.ARGB32.color(alpha, it) }
        val endColor = randomColor.let { FastColor.ARGB32.color(alpha, it) }

        // Group cube
        run {
            val box = AABB(-0.2, 0.0, -0.2, 0.2, 0.05, 0.2).move(0.5, 1.0, 0.5)
            val worldBox = box.move(blockEntity.blockPos)
            val camera = Minecraft.getInstance().gameRenderer.mainCamera
            if (camera.position.y >= worldBox.maxY) {
                DrawUtils.drawBox(
                    pose, bufferSource.getBuffer(curtainRenderType),
                    box,
                    packedLight = packedLight,
                    packedOverlay = packedOverlay,
                    color = blockEntity.getGroupColor(),
                    drawTop = true,
                    drawBottom = false
                )
            }
        }

        // Open curtain
        if (isOpen) {
            pose.pushPose()
            pose.translate(0f, 0.0f, 0f)

            var lower = 0.0
            if (!fastGraphics) {
                lower += (dist / maxDist).pow(3.0) * 0.4
                lower = lower.coerceIn(0.1, 0.4)
            }
            renderCurtainSection(
                pose, bufferSource,
                box = AABB(0.0, 0.5, 0.0, 1.0, 1.0, 1.0),
                packedLight = packedLight,
                blockEntity = blockEntity,
                packedOverlay = packedOverlay,
                color = color,
                endColor = endColor,
                randomY = randomY,
                cuts = cuts,
                lower = lower
            )
            pose.popPose()
            return
        }

        // Lowered curtain
        val length = blockEntity.findLength() - 1.0
        val box = AABB(0.0, 1.0, 0.0, 1.0, (length * -1f), 1.0)
        pose.pushPose()
        renderCurtainSection(
            pose,
            bufferSource,
            blockEntity = blockEntity,
            packedLight = packedLight,
            packedOverlay = packedOverlay,
            color = color,
            endColor = endColor,
            randomY = randomY,
            cuts = cuts,
            box = box
        )
        pose.popPose()
    }

    fun renderCurtainSection(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        blockEntity: CurtainBlockEntity,
        color: Int,
        endColor: Int,
        randomY: Float,
        box: AABB,
        packedOverlay: Int,
        packedLight: Int,
        cuts: DoubleArray = DoubleArray(4) { Double.MAX_VALUE },
        drawSidesOnly: Boolean = false,
        cullVertical: Boolean = false,
        lower: Double = 0.0
    ) {
        val camera = Minecraft.getInstance().gameRenderer.mainCamera
        val fastGraphics = Minecraft.getInstance()?.options?.graphicsMode()?.get() == GraphicsStatus.FAST

        val lowerOffset = voxelSnap(randomY * 0.2f).toDouble() + lower
        val originalBox = box
        val box = if (fastGraphics) box else box.expandTowards(0.0, -lowerOffset, 0.0)
        val bottomBox = AABB(0.0, originalBox.bottomCenter.y, 0.0, 1.0, (originalBox.bottomCenter.y - lowerOffset), 1.0)
        val devTestColor = CommonColors.RED.let { FastColor.ARGB32.color(150, it) }

        val worldBox = box.move(blockEntity.blockPos)
        val playerInside = worldBox.inflate(0.4).contains(camera.position)
        val color = if (playerInside) FastColor.ARGB32.color(128, color) else color
        val consumer = bufferSource.getBuffer(if (playerInside) curtainRenderType else curtainRenderTypeCull)

        val endHeight = 0.25
        val endBox = AABB(0.0, 0.0, 0.0, 1.0, endHeight, 1.0)
            .deflate(0.01, 0.0, 0.01)
            .move(0.0, -endHeight + box.bottomCenter.y + 0.005, 0.0)
        val endCuts = DoubleArray(4) { Double.MAX_VALUE }
        blockEntity.bottom = box.bottomCenter.y

        fun draw(consumer: VertexConsumer, part: AABB, tint: Int, top: Boolean = false, bottom: Boolean = false, sidesOnly: Boolean = false, sideCuts: DoubleArray = cuts) {
            val ys = (sideCuts.filter { it > part.minY && it < part.maxY } + part.minY + part.maxY).distinct().sorted()
            for (i in 0 until ys.size - 1) {
                DrawUtils.drawBox(
                    poseStack, consumer,
                    AABB(part.minX, ys[i], part.minZ, part.maxX, ys[i + 1], part.maxZ),
                    packedLight = packedLight,
                    packedOverlay = packedOverlay,
                    color = tint,
                    drawSouth = playerInside || ys[i] < sideCuts[0],
                    drawNorth = playerInside || ys[i] < sideCuts[1],
                    drawWest = playerInside || ys[i] < sideCuts[2],
                    drawEast = playerInside || ys[i] < sideCuts[3],
                    drawTop = top && i == ys.size - 2,
                    drawBottom = bottom && i == 0,
                    sidesOnly = sidesOnly
                )
            }
        }

        if (fastGraphics) {
            draw(
                consumer,
                box,
                color,
                top = !drawSidesOnly && if (cullVertical) camera.position.y >= worldBox.maxY else true,
                bottom = !drawSidesOnly && if (cullVertical) camera.position.y <= worldBox.minY else true
            )
        } else {
            draw(
                consumer,
                originalBox,
                color,
                top = !drawSidesOnly && if (cullVertical) camera.position.y >= worldBox.maxY else true,
                bottom = !(bottomBox.maxY >= originalBox.minY - 0.1f)  // This is sooo cooked
            )
            draw(
                consumer,
                bottomBox,
                color,
                bottom = true
            )
        }

        draw(bufferSource.getBuffer(endRenderType), endBox, endColor, sidesOnly = true, sideCuts = endCuts)
    }
}