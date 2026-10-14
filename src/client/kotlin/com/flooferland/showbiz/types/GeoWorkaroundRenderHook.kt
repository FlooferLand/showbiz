@file:Suppress("unused")

package com.flooferland.showbiz.types

import net.minecraft.client.*
import net.minecraft.client.renderer.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.ShowbizClient
import com.flooferland.showbiz.blocks.entities.SpotlightBlockEntity
import com.flooferland.showbiz.entities.DecorEntity
import com.flooferland.showbiz.entities.FloodlightEntity
import com.flooferland.showbiz.types.collidepart.ICollidePartInteractable
import com.flooferland.showbiz.utils.ClientExtensions.calculateBounds
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import org.joml.Matrix4f
import org.joml.Vector4f
import java.util.WeakHashMap
import software.bernie.geckolib.animatable.GeoAnimatable
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.cache.`object`.GeoBone
import kotlin.jvm.optionals.getOrNull

/**
 * Workaround for GeckoLib #841: worldSpaceMatrix is broken inside GeoBlockRenderer
 * Credit to https://duzo.is-a.dev
 */
class GeoWorkaroundRenderHook() {
    // One reused matrix per bone, stamped with the frame that filled it; a bone this frame skipped reads as missing
    private class Captured {
        val matrix = Matrix4f()
        var frame = -1
    }
    private val capturedBoneMatrices = mutableMapOf<String, Captured>()
    private var frame = 0
    // Physics: the bones a physics system reads a drawn pose from
    private var physicsPoses: MutableMap<String, Matrix4f>? = null
    private var physicsBones: Set<String> = emptySet()
    private var physicsOffsetX: Float = 0f
    private var physicsOffsetY: Float = 0f
    private var physicsOffsetZ: Float = 0f

    fun beforeRenderCubesOfBone(poseStack: PoseStack, bone: GeoBone, buffer: VertexConsumer?, packedLight: Int, packedOverlay: Int, colour: Int) {
        val captured = capturedBoneMatrices.getOrPut(bone.name) { Captured() }
        captured.frame = frame
        val pose = captured.matrix.set(poseStack.last().pose())
        // undo prepMatrixForBone's translateAwayFromPivotPoint so the matrix matches worldSpaceMatrix
        pose.translate(bone.pivotX / 16f, bone.pivotY / 16f, bone.pivotZ / 16f)

        val poses = physicsPoses ?: return
        if (bone.name !in physicsBones) return
        poses.getOrPut(bone.name) { Matrix4f() }.set(pose).setTranslation(pose.m30() + physicsOffsetX, pose.m31() + physicsOffsetY, pose.m32() + physicsOffsetZ)
    }
    fun beforePreRender(poseStack: PoseStack, animatable: GeoAnimatable, model: BakedGeoModel, bufferSource: MultiBufferSource?, buffer: VertexConsumer?, isReRender: Boolean, partialTick: Float, packedLight: Int, packedOverlay: Int, colour: Int) {
        if (isReRender) return
        frame++

        // Physics :)
        physicsPoses = null
        val bot = animatable as? IBot ?: return
        val origin = bot.botPos ?: return
        val layout = ShowbizClient.bots[bot.botId]?.model?.let { ShowbizClient.botModels[it] }?.chainLayout ?: return
        if (layout.size == 0) return
        val cam = Minecraft.getInstance().gameRenderer.mainCamera.position
        physicsOffsetX = (cam.x - origin.x).toFloat()
        physicsOffsetY = (cam.y - origin.y).toFloat()
        physicsOffsetZ = (cam.z - origin.z).toFloat()
        physicsBones = layout.bones
        physicsPoses = lastPoses.getOrPut(animatable) { mutableMapOf() }
    }

    fun postRender(poseStack: PoseStack, animatable: GeoAnimatable, model: BakedGeoModel, bufferSource: MultiBufferSource, buffer: VertexConsumer?, isReRender: Boolean, partialTick: Float, packedLight: Int, packedOverlay: Int, colour: Int) {
        if (isReRender) return
        if (animatable is IBot) run {
            val entities = DecorEntity.decorEntities[animatable] ?: return@run
            entities.forEach { entity ->
                val bone = entity.boneName?.let { model.getBone(it).getOrNull() } ?: return@forEach
                val bonePos = bonePosFromCapture(bone) ?: return@forEach
                entity.moveDecor(bonePos)
            }
        }
        when (animatable) {
            is SpotlightBlockEntity -> {
                val startBone = model.getBone("start").getOrNull() ?: return
                val startPos = bonePosFromCapture(startBone) ?: return
                animatable.startPos = startPos
                val endBone = model.getBone("end").getOrNull() ?: return
                val endPos = bonePosFromCapture(endBone) ?: return
                animatable.endPos = endPos
            }
            is FloodlightEntity -> {
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
                        entity.targetSize = bone.calculateBounds { captured(it.name) }
                    }
                }
            }
        }
    }

   companion object {
        val lastPoses: WeakHashMap<GeoAnimatable, MutableMap<String, Matrix4f>> = WeakHashMap()
    }

    private fun captured(name: String): Matrix4f? = capturedBoneMatrices[name]?.takeIf { it.frame == frame }?.matrix

    fun bonePosFromCapture(bone: GeoBone): Vec3? {
        val mat = captured(bone.name) ?: return null
        val v = Vector4f(0f, 0f, 0f, 1f).mul(mat)
        val cam = Minecraft.getInstance().gameRenderer.mainCamera.position
        return Vec3(v.x.toDouble() + cam.x, v.y.toDouble() + cam.y, v.z.toDouble() + cam.z)
    }
}