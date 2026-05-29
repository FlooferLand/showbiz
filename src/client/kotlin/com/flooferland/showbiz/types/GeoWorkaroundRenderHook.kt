@file:Suppress("unused")

package com.flooferland.showbiz.types

import net.minecraft.client.*
import net.minecraft.client.renderer.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.blocks.entities.SpotlightBlockEntity
import com.flooferland.showbiz.types.collidepart.ICollidePartInteractable
import com.flooferland.showbiz.utils.ClientExtensions.calculateBounds
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import org.joml.Matrix4f
import org.joml.Vector4f
import software.bernie.geckolib.animatable.GeoAnimatable
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.cache.`object`.GeoBone
import kotlin.jvm.optionals.getOrNull

/**
 * Workaround for GeckoLib #841: worldSpaceMatrix is broken inside GeoBlockRenderer
 * Credit to https://duzo.is-a.dev
 */
class GeoWorkaroundRenderHook() {
    private val capturedBoneMatrices = mutableMapOf<String, Matrix4f>()

    fun beforeRenderCubesOfBone(poseStack: PoseStack, bone: GeoBone, buffer: VertexConsumer?, packedLight: Int, packedOverlay: Int, colour: Int) {
        val pose = Matrix4f(poseStack.last().pose())
        // undo prepMatrixForBone's translateAwayFromPivotPoint so the matrix matches worldSpaceMatrix
        pose.translate(bone.pivotX / 16f, bone.pivotY / 16f, bone.pivotZ / 16f)
        capturedBoneMatrices[bone.name] = pose
    }
    fun beforePreRender(poseStack: PoseStack, animatable: GeoAnimatable, model: BakedGeoModel, bufferSource: MultiBufferSource?, buffer: VertexConsumer?, isReRender: Boolean, partialTick: Float, packedLight: Int, packedOverlay: Int, colour: Int) {
        if (isReRender) return
        capturedBoneMatrices.clear()
    }

    fun postRender(poseStack: PoseStack, animatable: GeoAnimatable, model: BakedGeoModel, bufferSource: MultiBufferSource, buffer: VertexConsumer?, isReRender: Boolean, partialTick: Float, packedLight: Int, packedOverlay: Int, colour: Int) {
        when (animatable) {
            is SpotlightBlockEntity -> {
                val startBone = model.getBone("start").getOrNull() ?: return
                val startPos = bonePosFromCapture(startBone) ?: return
                animatable.startPos = startPos
                val endBone = model.getBone("end").getOrNull() ?: return
                val endPos = bonePosFromCapture(endBone) ?: return
                animatable.endPos = endPos
            }
            is ICollidePartInteractable -> {
                val instance = animatable.collidePartInstance
                val clientInstance = instance.clientInstance as? ClientCollidePartInstance ?: return
                for ((bone, id) in instance.bonesToIds) {
                    model.getBone(bone).getOrNull()?.let { bone ->
                        val entity = clientInstance.spawned[id] ?: return@let
                        entity.targetPos = bonePosFromCapture(bone) ?: return@let
                        entity.targetSize = bone.calculateBounds { capturedBoneMatrices[it.name] }
                    }
                }
            }
        }
    }

    fun bonePosFromCapture(bone: GeoBone): Vec3? {
        val mat = capturedBoneMatrices[bone.name] ?: return null
        val v = Vector4f(0f, 0f, 0f, 1f).mul(mat)
        val cam = Minecraft.getInstance().gameRenderer.mainCamera.position
        return Vec3(v.x.toDouble() + cam.x, v.y.toDouble() + cam.y, v.z.toDouble() + cam.z)
    }
}