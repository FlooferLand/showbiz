package com.flooferland.showbiz.renderers

import com.flooferland.showbiz.blocks.CurtainBlock
import com.flooferland.showbiz.blocks.entities.CurtainBlockEntity
import com.flooferland.showbiz.utils.DrawUtils
import com.flooferland.showbiz.utils.rl
import com.flooferland.showbiz.utils.voxelSnap
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.GraphicsStatus
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.util.CommonColors
import net.minecraft.util.FastColor
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
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

    override fun render(blockEntity: CurtainBlockEntity, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int) {
        val level = blockEntity.level as? ClientLevel ?: return
        val minecraft = Minecraft.getInstance() ?: return
        val fastGraphics = minecraft.options?.graphicsMode()?.get() == GraphicsStatus.FAST

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
            val box = AABB(-0.2, 0.0, -0.2, 0.2, 0.001, 0.2).move(0.5, 1.0, 0.5)
            val worldBox = box.move(blockEntity.blockPos)
            val camera = Minecraft.getInstance().gameRenderer.mainCamera
            if (camera.position.y >= worldBox.maxY) {
                DrawUtils.drawBox(poseStack, bufferSource.getBuffer(curtainRenderType), box) {
                    it.packedLight = packedLight
                    it.packedOverlay = packedOverlay
                    it.color = blockEntity.getGroupColor()
                    it.faces.set(Direction.UP)
                }
            }
        }

        // Open curtain
        if (isOpen) {
            poseStack.pushPose()
            poseStack.translate(0f, 0.0f, 0f)

            var lower = 0.0
            if (!fastGraphics) {
                lower += (dist / maxDist).pow(3.0) * 0.4
                lower = lower.coerceIn(0.1, 0.4)
            }
            renderCurtainSection(
                poseStack, bufferSource,
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
            poseStack.popPose()
            return
        }

        // Lowered curtain
        val length = blockEntity.findLength() - 1.0
        val box = AABB(0.0, 1.0, 0.0, 1.0, (length * -1f), 1.0)
        poseStack.pushPose()
        renderCurtainSection(
            poseStack,
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
        poseStack.popPose()
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

        val lowerOffset = voxelSnap(randomY * 0.2) + lower
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

        fun draw(consumer: VertexConsumer, part: AABB, tint: Int, up: Boolean = false, down: Boolean = false, sidesOnly: Boolean = false, sideCuts: DoubleArray = cuts) {
            val ys = (sideCuts.filter { it > part.minY && it < part.maxY } + part.minY + part.maxY).distinct().sorted()
            for (i in 0 until ys.size - 1) {
                DrawUtils.drawBox(
                    poseStack, consumer,
                    AABB(part.minX, ys[i], part.minZ, part.maxX, ys[i + 1], part.maxZ)
                ) {
                    it.packedLight = packedLight
                    it.packedOverlay = packedOverlay
                    it.color = tint
                    it.faces = DrawUtils.BoxStyle.Faces(
                        south = playerInside || ys[i] < sideCuts[0],
                        north = playerInside || ys[i] < sideCuts[1],
                        west = playerInside || ys[i] < sideCuts[2],
                        east = playerInside || ys[i] < sideCuts[3],
                        up = !sidesOnly && up && i == ys.size - 2,
                        down = !sidesOnly && down && i == 0
                    )
                }
            }
        }

        if (fastGraphics) {
            draw(
                consumer,
                box,
                color,
                up = !drawSidesOnly && if (cullVertical) camera.position.y >= worldBox.maxY else true,
                down = !drawSidesOnly && if (cullVertical) camera.position.y <= worldBox.minY else true
            )
        } else {
            draw(
                consumer,
                originalBox,
                color,
                up = !drawSidesOnly && if (cullVertical) camera.position.y >= worldBox.maxY else true,
                down = !(bottomBox.maxY >= originalBox.minY - 0.1f)  // This is sooo cooked
            )
            draw(
                consumer,
                bottomBox,
                color,
                down = true
            )
        }

        draw(bufferSource.getBuffer(endRenderType), endBox, endColor, sidesOnly = true, sideCuts = endCuts)
    }

    companion object {
        var zoom = 0f

        // Drawing group text
        // Rendered as in the world last render event otherwise curtains draw over it
        fun render(mc: Minecraft, buffer: MultiBufferSource.BufferSource, poseStack: PoseStack) {
            val level = mc.level ?: return
            val blockEntity = run {
                if (mc.player?.isCrouching != true) return@run null
                val hitResult = mc.hitResult ?: return@run null
                val blockPos = (hitResult as? BlockHitResult)?.blockPos ?: return@run null
                level.getBlockEntity(blockPos) as? CurtainBlockEntity ?: return@run null
            }
            if (blockEntity == null) {
                zoom = 0.15f
                return
            }
            if (zoom > 0f) {
                zoom = (zoom - (mc.timer.realtimeDeltaTicks * 0.08f)).coerceAtLeast(0f)
            }

            val curtains = blockEntity.findConnectedCurtains(ignoreGroups = true, affectWorld = false)
            for (curtainPos in curtains) {
                val curtain = level.getBlockEntity(curtainPos) as? CurtainBlockEntity ?: continue
                val text = Component.literal("+").append("\n")
                    .append(Component.literal("Group ${curtain.group}\n").withColor(curtain.getGroupColor()))
                    .append(Component.literal("-"))

                DrawUtils.drawText(poseStack, curtainPos.bottomCenter, text, buffer) { it.scale = 0.9f - zoom }
            }
        }
    }
}