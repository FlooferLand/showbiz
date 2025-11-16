package com.flooferland.showbiz.renderers

import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.renderer.*
import net.minecraft.network.chat.*
import net.minecraft.util.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.registry.ModItems
import com.flooferland.showbiz.types.connection.GlobalConnections
import com.flooferland.showbiz.types.connection.GlobalConnections.entries
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.*
import com.mojang.math.Axis
import org.joml.Matrix4f
import kotlin.math.atan2

// Thank god for https://github.com/MrCrayfish/MrCrayfishFurnitureMod-Refurbished/tree/1.21.1
object ConnectionRenderer {
    val queued = mutableListOf<(pose: PoseStack, consumer: BufferBuilder) -> Unit>()
    val queuedText = mutableListOf<QueuedText>()
    const val POINT_POS_MUL = 0.15

    data class QueuedText(val x: Float, val y: Float, val z: Float, val value: Component)

    fun render(player: Player, source: MultiBufferSource.BufferSource, partialTick: Float) {
        if (!isHoldingWand(player)) return
        for ((pos, points) in entries) {
            for (point in points) {
                val yOffset = getYOffset(point.index, points.size)
                val pointPos = pos.center.add(0.0, yOffset, 0.0)

                // Points
                deferRender { pose, consumer -> renderPoint(pose, consumer, point, pointPos) }

                // Connections
                for (connection in point.connections) {
                    val targetPoint = connection.point
                    val endYOffset = getYOffset(targetPoint.index, entries[connection.pos]?.size ?: 1)
                    val end = connection.pos.center.add(0.0, endYOffset, 0.0)
                    deferRender { pose, consumer -> renderConnection(pose, consumer, pointPos, end) }
                }
            }
        }
    }

    fun renderPoint(pose: PoseStack, consumer: BufferBuilder, point: GlobalConnections.Point, pos: Vec3) {
        val color = when (point.type) {
            GlobalConnections.PointType.Input -> FastColor.ARGB32.color(255, 20, 200, 255)   // Blue
            GlobalConnections.PointType.Output -> FastColor.ARGB32.color(255, 10, 255, 30)   // Green
        }
        val darkColor = FastColor.ARGB32.color(255, 10, 10, 10)
        val bound = getPointBoundingBox()

        // Rendering the box
        run {
            pose.pushPose()
            pose.translate(pos.x, pos.y, pos.z)
            val matrix = pose.last().pose()
            drawColouredBox(matrix, consumer, bound.inflate(-0.032), color, 1.0f)
            drawColouredBox(matrix, consumer, bound, darkColor, 0.6f)
            pose.popPose()
        }

        // Rendering the text
        val textSign = when (point.type) {
            GlobalConnections.PointType.Input -> "in"
            GlobalConnections.PointType.Output -> "out"
        }
        val text = Component.literal(textSign).withColor(color)
            .append(Component.literal(" ").withColor(0xFFFFFFFF.toInt()))
            .append(Component.literal(point.id).withColor(0xFFFFFFFF.toInt()))
        renderText(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat(), text)
    }

    fun renderConnection(pose: PoseStack, consumer: BufferBuilder, startPos: Vec3, endPos: Vec3) {
        val delta = endPos.subtract(startPos)
        val length = delta.length()
        val color = FastColor.ARGB32.color(255, 100, 255, 100)
        val darkColor = FastColor.ARGB32.color(255, 10, 10, 10)
        val yaw = atan2(-delta.z, delta.x) + Mth.PI
        val pitch = atan2(delta.horizontalDistance(), delta.y) + Mth.HALF_PI

        pose.pushPose()
        pose.translate(startPos.x, startPos.y, startPos.z)
        pose.mulPose(Axis.YP.rotation(yaw.toFloat()))
        pose.mulPose(Axis.ZP.rotation(pitch.toFloat()))
        val matrix = pose.last().pose()
        drawColouredBox(matrix, consumer, AABB(0.0, -0.03125, -0.03125, length, 0.03125, 0.03125), color, 0.6f)
        drawColouredBox(matrix, consumer, AABB(0.0, -0.03125, -0.03125, length, 0.03125, 0.03125).inflate(0.032), darkColor, 0.2f)
        pose.popPose()
    }

    fun renderText(x: Float, y: Float, z: Float, text: String) {
        queuedText.add(QueuedText(x, y, z, Component.literal(text)))
    }
    fun renderText(x: Float, y: Float, z: Float, text: Component) {
        queuedText.add(QueuedText(x, y, z, text))
    }

    fun renderDeferred(pose: PoseStack) {
        val mc = Minecraft.getInstance() ?: return
        val target = mc.levelRenderer?.entityTarget() ?: return
        target.bindWrite(false)

        val tesselator = Tesselator.getInstance() ?: return
        RenderSystem.enableDepthTest()
        RenderSystem.enableBlend()
        RenderSystem.blendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
        )
        RenderSystem.depthMask(true)
        RenderSystem.setShader(GameRenderer::getPositionColorShader)
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
        pose.pushPose()

        val builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR)
        for (consumer in queued) {
            consumer(pose, builder)
        }
        builder.build()?.let { mesh -> BufferUploader.drawWithShader(mesh) }
        run { // Render text
            val mc = Minecraft.getInstance() ?: return@run
            val camera = mc.gameRenderer?.mainCamera ?: return@run
            val font = mc.font ?: return@run
            val bufferSource = mc.renderBuffers()?.bufferSource() ?: return@run

            RenderSystem.disableDepthTest()
            RenderSystem.depthMask(false)
            for (text in queuedText) {
                pose.pushPose()
                pose.translate(text.x, text.y, text.z)
                pose.mulPose(camera.rotation())
                pose.mulPose(Axis.XP.rotation(Mth.PI))
                pose.scale(0.01f, 0.01f, 0.01f)
                val matrix = pose.last().pose()
                val shadowText = Component.literal(text.value.string).withColor(0xFF_00_00_00.toInt())
                font.drawInBatch(shadowText, -(font.width(shadowText) / 2).toFloat() + 1, 1f, 0xFF_00_00_00.toInt(), false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0x00_00_00_00,  LightTexture.FULL_BRIGHT)
                font.drawInBatch(text.value, -(font.width(text.value) / 2).toFloat(), 0f, 0xFF_FF_FF_FF.toInt(), false, matrix, bufferSource, Font.DisplayMode.SEE_THROUGH, 0x00_00_00_00,  LightTexture.FULL_BRIGHT)
                pose.popPose()
            }
            bufferSource.endBatch()
            RenderSystem.enableDepthTest()
            RenderSystem.depthMask(true)
            queuedText.clear()
        }

        pose.popPose()
        RenderSystem.disableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
        RenderSystem.depthMask(true)

        mc.mainRenderTarget.bindWrite(false)
        queued.clear()
    }

    private fun deferRender(callback: (pose: PoseStack, consumer: BufferBuilder) -> Unit) {
        queued.add(callback)
    }

    fun getPointBoundingBox(): AABB {
        val size = 0.105
        return AABB.ofSize(Vec3.ZERO, size, size, size)
    }

    fun getYOffset(index: Int, groupSize: Int): Double {
        return (index * POINT_POS_MUL) - ((groupSize - 1) * POINT_POS_MUL / 2.0)
    }

    fun isHoldingWand(player: Player) =
        player.isHolding(ModItems.Wand.item)

    @Suppress("DuplicatedCode")
    fun drawColouredBox(matrix: Matrix4f, consumer: VertexConsumer, box: AABB, colour: Int, alpha: Float) {
        val red = FastColor.ARGB32.red(colour) / 255f
        val green = FastColor.ARGB32.green(colour) / 255f
        val blue = FastColor.ARGB32.blue(colour) / 255f
        val minU = 0.0f
        val minV = 0.25f
        val maxU = minU + 0.25f
        val maxV = minV + 0.25f

        // North
        consumer.addVertex(matrix, box.minX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat()).setUv(maxU, minV).setColor(red, green, blue, alpha)
        consumer.addVertex(matrix, box.maxX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat()).setUv(minU, minV).setColor(red, green, blue, alpha)
        consumer.addVertex(matrix, box.maxX.toFloat(), box.minY.toFloat(), box.minZ.toFloat()).setUv(minU, maxV).setColor(red, green, blue, alpha)
        consumer.addVertex(matrix, box.minX.toFloat(), box.minY.toFloat(), box.minZ.toFloat()).setUv(maxU, maxV).setColor(red, green, blue, alpha)

        // South
        consumer.addVertex(matrix, box.maxX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat()).setUv(maxU, minV).setColor(red, green, blue, alpha)
        consumer.addVertex(matrix, box.minX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat()).setUv(minU, minV).setColor(red, green, blue, alpha)
        consumer.addVertex(matrix, box.minX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat()).setUv(minU, maxV).setColor(red, green, blue, alpha)
        consumer.addVertex(matrix, box.maxX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat()).setUv(maxU, maxV).setColor(red, green, blue, alpha)

        // West
        consumer.addVertex(matrix, box.minX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat()).setUv(maxU, minV).setColor(red, green, blue, alpha)
        consumer.addVertex(matrix, box.minX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat()).setUv(minU, minV).setColor(red, green, blue, alpha)
        consumer.addVertex(matrix, box.minX.toFloat(), box.minY.toFloat(), box.minZ.toFloat()).setUv(minU, maxV).setColor(red, green, blue, alpha)
        consumer.addVertex(matrix, box.minX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat()).setUv(maxU, maxV).setColor(red, green, blue, alpha)

        // East
        consumer.addVertex(matrix, box.maxX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat()).setUv(maxU, minV).setColor(red, green, blue, alpha)
        consumer.addVertex(matrix, box.maxX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat()).setUv(minU, minV).setColor(red, green, blue, alpha)
        consumer.addVertex(matrix, box.maxX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat()).setUv(minU, maxV).setColor(red, green, blue, alpha)
        consumer.addVertex(matrix, box.maxX.toFloat(), box.minY.toFloat(), box.minZ.toFloat()).setUv(maxU, maxV).setColor(red, green, blue, alpha)

        // Up
        consumer.addVertex(matrix, box.minX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat()).setUv(maxU, minV).setColor(red, green, blue, alpha)
        consumer.addVertex(matrix, box.minX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat()).setUv(minU, minV).setColor(red, green, blue, alpha)
        consumer.addVertex(matrix, box.maxX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat()).setUv(minU, maxV).setColor(red, green, blue, alpha)
        consumer.addVertex(matrix, box.maxX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat()).setUv(maxU, maxV).setColor(red, green, blue, alpha)

        // Down
        consumer.addVertex(matrix, box.minX.toFloat(), box.minY.toFloat(), box.minZ.toFloat()).setUv(maxU, minV).setColor(red, green, blue, alpha)
        consumer.addVertex(matrix, box.maxX.toFloat(), box.minY.toFloat(), box.minZ.toFloat()).setUv(minU, minV).setColor(red, green, blue, alpha)
        consumer.addVertex(matrix, box.maxX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat()).setUv(minU, maxV).setColor(red, green, blue, alpha)
        consumer.addVertex(matrix, box.minX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat()).setUv(maxU, maxV).setColor(red, green, blue, alpha)
    }

    @Suppress("DuplicatedCode")
    private fun drawBox(pose: PoseStack, consumer: VertexConsumer, box: AABB) {
        val matrix = pose.last().pose()

        var width = (box.maxX - box.minX).toFloat()
        var height = (box.maxY - box.minY).toFloat()
        if (width > 0.01) {
            // North
            consumer.addVertex(matrix, box.minX.toFloat(), box.minY.toFloat(), box.minZ.toFloat()).setUv(0f, height)
            consumer.addVertex(matrix, box.maxX.toFloat(), box.minY.toFloat(), box.minZ.toFloat()).setUv(width, height)
            consumer.addVertex(matrix, box.maxX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat()).setUv(width, 0f)
            consumer.addVertex(matrix, box.minX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat()).setUv(0f, 0f)

            // South
            consumer.addVertex(matrix, box.maxX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat()).setUv(0f, height)
            consumer.addVertex(matrix, box.minX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat()).setUv(width, height)
            consumer.addVertex(matrix, box.minX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat()).setUv(width, 0f)
            consumer.addVertex(matrix, box.maxX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat()).setUv(0f, 0f)
        }

        width = (box.maxZ - box.minZ).toFloat()
        if (width > 0.01) {
            // West
            consumer.addVertex(matrix, box.minX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat()).setUv(0f, height)
            consumer.addVertex(matrix, box.minX.toFloat(), box.minY.toFloat(), box.minZ.toFloat()).setUv(width, height)
            consumer.addVertex(matrix, box.minX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat()).setUv(width, 0f)
            consumer.addVertex(matrix, box.minX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat()).setUv(0f, 0f)

            // East
            consumer.addVertex(matrix, box.maxX.toFloat(), box.minY.toFloat(), box.minZ.toFloat()).setUv(0f, height)
            consumer.addVertex(matrix, box.maxX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat()).setUv(width, height)
            consumer.addVertex(matrix, box.maxX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat()).setUv(width, 0f)
            consumer.addVertex(matrix, box.maxX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat()).setUv(0f, 0f)
        }

        width = (box.maxX - box.minX).toFloat()
        height = (box.maxZ - box.minZ).toFloat()
        if (width > 0.01) {
            // Up
            consumer.addVertex(matrix, box.minX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat()).setUv(0f, width)
            consumer.addVertex(matrix, box.minX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat()).setUv(height, width)
            consumer.addVertex(matrix, box.maxX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat()).setUv(height, 0f)
            consumer.addVertex(matrix, box.maxX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat()).setUv(0f, 0f)

            // Down
            consumer.addVertex(matrix, box.minX.toFloat(), box.minY.toFloat(), box.minZ.toFloat()).setUv(0f, height)
            consumer.addVertex(matrix, box.maxX.toFloat(), box.minY.toFloat(), box.minZ.toFloat()).setUv(width, height)
            consumer.addVertex(matrix, box.maxX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat()).setUv(width, 0f)
            consumer.addVertex(matrix, box.minX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat()).setUv(0f, 0f)
        }
    }
}