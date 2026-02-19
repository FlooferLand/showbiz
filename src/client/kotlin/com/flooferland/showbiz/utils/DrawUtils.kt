package com.flooferland.showbiz.utils

import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.util.FastColor
import net.minecraft.world.phys.*
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import org.joml.Matrix4f

object DrawUtils {
    @Suppress("DuplicatedCode")
    fun drawBox(poseStack: PoseStack, consumer: VertexConsumer, box: AABB, color: Int? = null, alpha: Float = 1f, packedOverlay: Int? = null, packedLight: Int? = null, sidesOnly: Boolean = false) {
        val matrix = poseStack.last().pose()
        val color = if (color != null) FastColor.ARGB32.color((alpha * 255).toInt(), color) else null

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
            val color = color ?: FastColor.ARGB32.colorFromFloat(
                shaderColor.getOrNull(3) ?: alpha,
                shaderColor.getOrNull(0) ?: 0f,
                shaderColor.getOrNull(1) ?: 0f,
                shaderColor.getOrNull(2) ?: 0f
            )
            consumer.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setLight(packedLight ?: LightTexture.FULL_BRIGHT)
                .setOverlay(packedOverlay ?: OverlayTexture.NO_OVERLAY)
                .setNormal(poseStack.last(), nx, ny, nz)
        }

        // North (negative z)
        addVertex(matrix, minX, minY, minZ, 0f, height, 0f, 0f, -1f)
        addVertex(matrix, minX, maxY, minZ, 0f, 0f, 0f, 0f, -1f)
        addVertex(matrix, maxX, maxY, minZ, width, 0f, 0f, 0f, -1f)
        addVertex(matrix, maxX, minY, minZ, width, height, 0f, 0f, -1f)

        // South (nositive z)
        addVertex(matrix, minX, minY, maxZ, 0f, height, 0f, 0f, 1f)
        addVertex(matrix, maxX, minY, maxZ, width, height, 0f, 0f, 1f)
        addVertex(matrix, maxX, maxY, maxZ, width, 0f, 0f, 0f, 1f)
        addVertex(matrix, minX, maxY, maxZ, 0f, 0f, 0f, 0f, 1f)

        // West (negative x)
        addVertex(matrix, minX, minY, maxZ, 0f, height, -1f, 0f, 0f)
        addVertex(matrix, minX, maxY, maxZ, 0f, 0f, -1f, 0f, 0f)
        addVertex(matrix, minX, maxY, minZ, depth, 0f, -1f, 0f, 0f)
        addVertex(matrix, minX, minY, minZ, depth, height, -1f, 0f, 0f)

        // East (positive x)
        addVertex(matrix, maxX, minY, minZ, 0f, height, 1f, 0f, 0f)
        addVertex(matrix, maxX, maxY, minZ, 0f, 0f, 1f, 0f, 0f)
        addVertex(matrix, maxX, maxY, maxZ, depth, 0f, 1f, 0f, 0f)
        addVertex(matrix, maxX, minY, maxZ, depth, height, 1f, 0f, 0f)

        if (!sidesOnly) {
            // Up (positive y)
            addVertex(matrix, minX, maxY, minZ, 0f, 0f, 0f, 1f, 0f)
            addVertex(matrix, minX, maxY, maxZ, 0f, depth, 0f, 1f, 0f)
            addVertex(matrix, maxX, maxY, maxZ, width, depth, 0f, 1f, 0f)
            addVertex(matrix, maxX, maxY, minZ, width, 0f, 0f, 1f, 0f)

            // Down (negative y)
            addVertex(matrix, minX, minY, minZ, 0f, 0f, 0f, -1f, 0f)
            addVertex(matrix, maxX, minY, minZ, width, 0f, 0f, -1f, 0f)
            addVertex(matrix, maxX, minY, maxZ, width, depth, 0f, -1f, 0f)
            addVertex(matrix, minX, minY, maxZ, 0f, depth, 0f, -1f, 0f)
        }
    }
}