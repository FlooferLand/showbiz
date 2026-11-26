package com.flooferland.showbiz.utils

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.*
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.util.FastColor
import net.minecraft.util.FormattedCharSequence
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import java.util.*

object DrawUtils {
    class TextStyle {
        var drawOutline: Boolean = false
        var drawBackground: Boolean = true
        var backgroundColor: Int = 0xCC_00_00_00.toInt()
        var maxWidth: Int = 150
        var scale: Float = 1.0f
        var vAlign: Float = 0.5f
        var billboard: Boolean = true
        var rotation: Float = 0f
        var depth: Boolean = false
        var wrapPad: Int = 3
        var linePad: Int = 6
    }
    class BoxStyle {
        var color: Int? = null
        var faces: Faces = Faces()
        var packedOverlay: Int = OverlayTexture.NO_OVERLAY
        var packedLight: Int = LightTexture.FULL_BRIGHT
        var normalShade: Boolean = false

        class Faces() {
            internal var selected: EnumSet<Direction> = EnumSet.allOf(Direction::class.java)
            operator fun contains(direction: Direction): Boolean  = selected.contains(direction)

            constructor(north: Boolean = false, south: Boolean = false, east: Boolean = false, west: Boolean = false, up: Boolean = false, down: Boolean = false) : this() {
                selected.clear()
                if (north) selected += Direction.NORTH
                if (south) selected += Direction.SOUTH
                if (east) selected += Direction.EAST
                if (west) selected += Direction.WEST
                if (up) selected += Direction.UP
                if (down) selected += Direction.DOWN
            }

            /** Enables faces ((all faces are shown by default) */
            fun enable(vararg dirs: Direction) {
                selected = if (dirs.isEmpty()) EnumSet.noneOf(Direction::class.java) else EnumSet.copyOf(dirs.toList())
            }

            /** Disables faces (all faces are shown by default) */
            fun disable(vararg dirs: Direction) {
                dirs.forEach { selected.remove(it) }
            }

            /** Makes specifically these faces visible */
            fun set(vararg dirs: Direction) {
                selected = EnumSet.copyOf(dirs.toList())
            }

            fun setSidesOnly() {
                selected = EnumSet.of(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST)
            }
        }
    }

    @Suppress("DuplicatedCode")
    fun drawBox(poseStack: PoseStack, consumer: VertexConsumer, box: AABB, configure: (style: BoxStyle) -> Unit) {
        val style = BoxStyle().also(configure)
        val matrix = poseStack.last().pose()
        val alpha = style.color?.let { FastColor.ARGB32.alpha(it).toFloat() / 255f }

        val minX = box.minX.toFloat()
        val minY = box.minY.toFloat()
        val minZ = box.minZ.toFloat()
        val maxX = box.maxX.toFloat()
        val maxY = box.maxY.toFloat()
        val maxZ = box.maxZ.toFloat()

        val width = maxX - minX
        val height = maxY - minY
        val depth = maxZ - minZ

        val shaderColor = RenderSystem.getShaderColor()
        val fallbackColor = FastColor.ARGB32.colorFromFloat(
            alpha ?: shaderColor.getOrNull(3) ?: 1f,
            shaderColor.getOrNull(0) ?: 0f,
            shaderColor.getOrNull(1) ?: 0f,
            shaderColor.getOrNull(2) ?: 0f
        )
        fun vertex(x: Float, y: Float, z: Float, u: Float, v: Float, nx: Float, ny: Float, nz: Float) {
            var color = style.color ?: fallbackColor
            if (style.normalShade) {
                val normal = poseStack.last().normal().transform(nx, ny, nz, Vector3f())
                val brightness = normal.let { it.x * it.x * 0.6f + it.z * it.z * 0.8f + it.y * it.y * (if (it.y > 0f) 1.0f else 0.5f) }
                color = FastColor.ARGB32.multiply(color, FastColor.ARGB32.colorFromFloat(1f, brightness, brightness, brightness))
            }
            consumer.addVertex(matrix, x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setLight(style.packedLight)
                .setOverlay(style.packedOverlay)
                .setNormal(poseStack.last(), nx, ny, nz)
        }

        // North (negative z)
        if (Direction.NORTH in style.faces) {
            vertex(minX, minY, minZ, 0f, height, 0f, 0f, -1f)
            vertex(minX, maxY, minZ, 0f, 0f, 0f, 0f, -1f)
            vertex(maxX, maxY, minZ, width, 0f, 0f, 0f, -1f)
            vertex(maxX, minY, minZ, width, height, 0f, 0f, -1f)
        }

        // South (positive z)
        if (Direction.SOUTH in style.faces) {
            vertex(minX, minY, maxZ, 0f, height, 0f, 0f, 1f)
            vertex(maxX, minY, maxZ, width, height, 0f, 0f, 1f)
            vertex(maxX, maxY, maxZ, width, 0f, 0f, 0f, 1f)
            vertex(minX, maxY, maxZ, 0f, 0f, 0f, 0f, 1f)
        }

        // West (negative x)
        if (Direction.WEST in style.faces) {
            vertex(minX, minY, maxZ, 0f, height, -1f, 0f, 0f)
            vertex(minX, maxY, maxZ, 0f, 0f, -1f, 0f, 0f)
            vertex(minX, maxY, minZ, depth, 0f, -1f, 0f, 0f)
            vertex(minX, minY, minZ, depth, height, -1f, 0f, 0f)
        }

        // East (positive x)
        if (Direction.EAST in style.faces) {
            vertex(maxX, minY, minZ, 0f, height, 1f, 0f, 0f)
            vertex(maxX, maxY, minZ, 0f, 0f, 1f, 0f, 0f)
            vertex(maxX, maxY, maxZ, depth, 0f, 1f, 0f, 0f)
            vertex(maxX, minY, maxZ, depth, height, 1f, 0f, 0f)
        }

        // Up (positive y)
        if (Direction.UP in style.faces) {
            vertex(minX, maxY, minZ, 0f, 0f, 0f, 1f, 0f)
            vertex(minX, maxY, maxZ, 0f, depth, 0f, 1f, 0f)
            vertex(maxX, maxY, maxZ, width, depth, 0f, 1f, 0f)
            vertex(maxX, maxY, minZ, width, 0f, 0f, 1f, 0f)
        }
        // Down (negative y)
        if (Direction.DOWN in style.faces) {
            vertex(minX, minY, minZ, 0f, 0f, 0f, -1f, 0f)
            vertex(maxX, minY, minZ, width, 0f, 0f, -1f, 0f)
            vertex(maxX, minY, maxZ, width, depth, 0f, -1f, 0f)
            vertex(minX, minY, maxZ, 0f, depth, 0f, -1f, 0f)
        }
    }

    fun drawText(poseStack: PoseStack, worldPos: Vec3, text: Component, buffer: MultiBufferSource.BufferSource? = null, configure: (TextStyle) -> Unit = { }) {
        val minecraft = Minecraft.getInstance() ?: return
        val font = minecraft.font ?: return
        val camera = minecraft.gameRenderer?.mainCamera ?: return
        val buffer = buffer ?: minecraft.renderBuffers().bufferSource()
        val style = TextStyle().also(configure)
        if (text.string.isNullOrEmpty()) return

        poseStack.pushPose()
        poseStack.translate(worldPos.x, worldPos.y, worldPos.z)
        if (style.billboard) {
            poseStack.mulPose(camera.rotation())
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(style.rotation))
        }
        poseStack.mulPose(Axis.XP.rotation(Mth.PI))
        poseStack.scale(style.scale * 0.01f, style.scale * 0.01f, style.scale * 0.01f)
        val matrix = poseStack.last().pose()

        var height = 0f
        val lines = mutableListOf<Pair<FormattedCharSequence, Float>>()
        for ((blockIndex, block) in font.splitter.splitLines(text, 9999, text.style).withIndex()) {
            // Separating via maxWidth
            for ((lineIndex, line) in font.split(block, style.maxWidth).withIndex()) {
                if (blockIndex > 0 || lineIndex > 0)
                    height += if (lineIndex == 0) style.linePad else style.wrapPad
                lines.add(line to height)
                height += font.lineHeight
            }
        }
        val yOffset = -height * style.vAlign

        if (style.drawBackground) {
            if (!style.depth) {
                RenderSystem.disableDepthTest()
            } else {
                RenderSystem.enableDepthTest()
            }
            RenderSystem.disableCull()
            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.setShader(GameRenderer::getPositionColorShader)
            val builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR)
            for ((line, ly) in lines) {
                val x0 = -(font.width(line) / 2f) - 2f
                val x1 = -x0
                val y0 = yOffset + ly - 2f
                val y1 = yOffset + ly + font.lineHeight + 2f
                builder.addVertex(matrix, x0, y0, 0f).setColor(style.backgroundColor)
                builder.addVertex(matrix, x0, y1, 0f).setColor(style.backgroundColor)
                builder.addVertex(matrix, x1, y1, 0f).setColor(style.backgroundColor)
                builder.addVertex(matrix, x1, y0, 0f).setColor(style.backgroundColor)
            }
            BufferUploader.drawWithShader(builder.buildOrThrow())
            RenderSystem.enableCull()
            RenderSystem.enableDepthTest()
        }

        for ((line, ly) in lines) {
            val displayMode = if (!style.depth) Font.DisplayMode.SEE_THROUGH else Font.DisplayMode.NORMAL
            val x = -font.width(line) / 2f
            val y = yOffset + ly
            val color = 0xFF_FF_FF_FF.toInt()
            val backgroundColor = 0
            val packedLightCoords = LightTexture.FULL_BRIGHT
            if (!style.drawOutline) {
                font.drawInBatch(line, x, y, color, false, matrix, buffer, displayMode, backgroundColor, packedLightCoords)
            } else {
                font.drawInBatch8xOutline(line, x, y, color, backgroundColor, matrix, buffer, packedLightCoords)
            }
        }
        if (!style.depth) {
            RenderSystem.disableDepthTest()
            RenderSystem.depthMask(false)
        }
        buffer.endBatch()

        RenderSystem.depthMask(true)
        RenderSystem.enableDepthTest()
        poseStack.popPose()
    }
}