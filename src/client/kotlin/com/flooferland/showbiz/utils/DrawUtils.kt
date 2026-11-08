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
import net.minecraft.network.chat.Component
import net.minecraft.util.FastColor
import net.minecraft.util.FormattedCharSequence
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector3f

object DrawUtils {
    @Suppress("DuplicatedCode")
    fun drawBox(
        poseStack: PoseStack,
        consumer: VertexConsumer,
        box: AABB,
        color: Int? = null,
        packedOverlay: Int? = null,
        packedLight: Int? = null,
        sidesOnly: Boolean = false,
        drawSouth: Boolean = true,
        drawNorth: Boolean = true,
        drawWest: Boolean = true,
        drawEast: Boolean = true,
        drawTop: Boolean = true,
        drawBottom: Boolean = true,
        normalShade: Boolean = false
    ) {
        val matrix = poseStack.last().pose()
        val alpha = color?.let { FastColor.ARGB32.alpha(it).toFloat() / 255f }

        val minX = box.minX.toFloat()
        val minY = box.minY.toFloat()
        val minZ = box.minZ.toFloat()
        val maxX = box.maxX.toFloat()
        val maxY = box.maxY.toFloat()
        val maxZ = box.maxZ.toFloat()

        val width = maxX - minX
        val height = maxY - minY
        val depth = maxZ - minZ

        fun addVertex(pose: Matrix4f, x: Float, y: Float, z: Float, u: Float, v: Float, nx: Float, ny: Float, nz: Float) {
            val shaderColor = RenderSystem.getShaderColor()
            var color = color ?: FastColor.ARGB32.colorFromFloat(
                alpha ?: shaderColor.getOrNull(3) ?: 1f,
                shaderColor.getOrNull(0) ?: 0f,
                shaderColor.getOrNull(1) ?: 0f,
                shaderColor.getOrNull(2) ?: 0f
            )
            if (normalShade) {
                val normal = poseStack.last().normal().transform(nx, ny, nz, Vector3f())
                val brightness = normal.let { it.x * it.x * 0.6f + it.z * it.z * 0.8f + it.y * it.y * (if (it.y > 0f) 1.0f else 0.5f) }
                color = FastColor.ARGB32.multiply(color, FastColor.ARGB32.colorFromFloat(1f, brightness, brightness, brightness))
            }
            consumer.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setLight(packedLight ?: LightTexture.FULL_BRIGHT)
                .setOverlay(packedOverlay ?: OverlayTexture.NO_OVERLAY)
                .setNormal(poseStack.last(), nx, ny, nz)
        }

        // North (negative z)
        if (drawNorth) {
            addVertex(matrix, minX, minY, minZ, 0f, height, 0f, 0f, -1f)
            addVertex(matrix, minX, maxY, minZ, 0f, 0f, 0f, 0f, -1f)
            addVertex(matrix, maxX, maxY, minZ, width, 0f, 0f, 0f, -1f)
            addVertex(matrix, maxX, minY, minZ, width, height, 0f, 0f, -1f)
        }

        // South (positive z)
        if (drawSouth) {
            addVertex(matrix, minX, minY, maxZ, 0f, height, 0f, 0f, 1f)
            addVertex(matrix, maxX, minY, maxZ, width, height, 0f, 0f, 1f)
            addVertex(matrix, maxX, maxY, maxZ, width, 0f, 0f, 0f, 1f)
            addVertex(matrix, minX, maxY, maxZ, 0f, 0f, 0f, 0f, 1f)
        }

        // West (negative x)
        if (drawWest) {
            addVertex(matrix, minX, minY, maxZ, 0f, height, -1f, 0f, 0f)
            addVertex(matrix, minX, maxY, maxZ, 0f, 0f, -1f, 0f, 0f)
            addVertex(matrix, minX, maxY, minZ, depth, 0f, -1f, 0f, 0f)
            addVertex(matrix, minX, minY, minZ, depth, height, -1f, 0f, 0f)
        }

        if (drawEast) {
            // East (positive x)
            addVertex(matrix, maxX, minY, minZ, 0f, height, 1f, 0f, 0f)
            addVertex(matrix, maxX, maxY, minZ, 0f, 0f, 1f, 0f, 0f)
            addVertex(matrix, maxX, maxY, maxZ, depth, 0f, 1f, 0f, 0f)
            addVertex(matrix, maxX, minY, maxZ, depth, height, 1f, 0f, 0f)
        }

        if (!sidesOnly) {
            if (drawTop) {
                // Up (positive y)
                addVertex(matrix, minX, maxY, minZ, 0f, 0f, 0f, 1f, 0f)
                addVertex(matrix, minX, maxY, maxZ, 0f, depth, 0f, 1f, 0f)
                addVertex(matrix, maxX, maxY, maxZ, width, depth, 0f, 1f, 0f)
                addVertex(matrix, maxX, maxY, minZ, width, 0f, 0f, 1f, 0f)
            }
            if (drawBottom) {
                // Down (negative y)
                addVertex(matrix, minX, minY, minZ, 0f, 0f, 0f, -1f, 0f)
                addVertex(matrix, maxX, minY, minZ, width, 0f, 0f, -1f, 0f)
                addVertex(matrix, maxX, minY, maxZ, width, depth, 0f, -1f, 0f)
                addVertex(matrix, minX, minY, maxZ, 0f, depth, 0f, -1f, 0f)
            }
        }
    }

    fun drawText(
        poseStack: PoseStack,
        worldPos: Vec3,
        text: Component,
        scale: Float = 1.0f,
        buffer: MultiBufferSource.BufferSource? = null,
        backgroundColor: Int = 0xCC_00_00_00.toInt(),
        maxWidth: Int = 150,
        wrapPad: Int = 3,
        linePad: Int = 6,
        vAlign: Float = 0.5f,
        drawBackground: Boolean = true
    ) {
        val minecraft = Minecraft.getInstance() ?: return
        val font = minecraft.font ?: return
        val camera = minecraft.gameRenderer?.mainCamera ?: return
        val buffer = buffer ?: minecraft.renderBuffers().bufferSource()

        poseStack.pushPose()
        poseStack.translate(worldPos.x, worldPos.y, worldPos.z)
        poseStack.mulPose(camera.rotation())
        poseStack.mulPose(Axis.XP.rotation(Mth.PI))
        poseStack.scale(scale * 0.01f, scale * 0.01f, scale * 0.01f)
        val matrix = poseStack.last().pose()

        var height = 0f
        val lines = mutableListOf<Pair<FormattedCharSequence, Float>>()
        for ((blockIndex, block) in text.string.split("\n").withIndex()) {
            // Separating via maxWidth
            for ((lineIndex, line) in font.split(Component.literal(block).withStyle(text.style), maxWidth).withIndex()) {
                if (blockIndex > 0 || lineIndex > 0)
                    height += if (lineIndex == 0) linePad else wrapPad
                lines.add(line to height)
                height += font.lineHeight
            }
        }
        val yOffset = -height * vAlign

        if (drawBackground) {
            RenderSystem.disableDepthTest()
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
                builder.addVertex(matrix, x0, y0, 0f).setColor(backgroundColor)
                builder.addVertex(matrix, x0, y1, 0f).setColor(backgroundColor)
                builder.addVertex(matrix, x1, y1, 0f).setColor(backgroundColor)
                builder.addVertex(matrix, x1, y0, 0f).setColor(backgroundColor)
            }
            BufferUploader.drawWithShader(builder.buildOrThrow())
            RenderSystem.enableCull()
            RenderSystem.enableDepthTest()
        }

        for ((line, ly) in lines) {
            font.drawInBatch(line, -font.width(line) / 2f, yOffset + ly, 0xFF_FF_FF_FF.toInt(), false, matrix, buffer, Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT)
        }
        poseStack.popPose()
    }
}