package com.flooferland.showbiz.utils

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector4f
import software.bernie.geckolib.animation.AnimationProcessor
import software.bernie.geckolib.cache.`object`.GeoBone

object ClientExtensions {
    /** Safe way to interface with pose stacks, always pops after pushing even on exceptions */
    fun PoseStack.pushAndPop(block: PoseStack.() -> Unit) {
        pushPose()
        try { block() }
        finally { popPose() }
    }

    fun BakedModel?.isMissing(): Boolean =
        this == null || this == Minecraft.getInstance().modelManager.missingModel

    fun AnimationProcessor<*>.getBoneCaseInsensitive(boneName: String): GeoBone? =
        registeredBones.firstOrNull { it.name.equals(boneName, ignoreCase = true) }

    fun GeoBone.calculateBounds(): Vec3 =
        calculateBounds { it.worldSpaceMatrix }

    // overload for the geckolib #841 workaround: caller supplies snapshotted matrices
    fun GeoBone.calculateBounds(matrixFor: (GeoBone) -> Matrix4f?): Vec3 {
        val min = Vector4f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, 1f)
        val max = Vector4f(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, 1f)
        val stack = mutableListOf(this)
        var hasCubes = false
        while (stack.isNotEmpty()) {
            val bone = stack.removeAt(stack.size - 1)
            val mat = matrixFor(bone) ?: bone.worldSpaceMatrix
            for (cube in bone.cubes) {
                hasCubes = true
                val size = cube.size
                val pos1 = Vector4f(0f, 0f, 0f, 1f).mul(mat)
                val pos2 = Vector4f(size.x.toFloat(), size.y.toFloat(), size.z.toFloat(), 1f).mul(mat)
                min.set(minOf(min.x, pos1.x, pos2.x), minOf(min.y, pos1.y, pos2.y), minOf(min.z, pos1.z, pos2.z), 1f)
                max.set(maxOf(max.x, pos1.x, pos2.x), maxOf(max.y, pos1.y, pos2.y), maxOf(max.z, pos1.z, pos2.z), 1f)
            }
            stack.addAll(bone.childBones)
        }
        return if (hasCubes) Vec3(
            (max.x - min.x).toDouble() / 16.0,
            (max.y - min.y).toDouble() / 16.0,
            (max.z - min.z).toDouble() / 16.0
        ) else Vec3.ZERO
    }
}