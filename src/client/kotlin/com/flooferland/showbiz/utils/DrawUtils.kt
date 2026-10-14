package com.flooferland.showbiz.utils

import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.texture.*
import net.minecraft.util.*
import net.minecraft.world.phys.*
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
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
}