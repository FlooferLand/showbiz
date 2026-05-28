package com.flooferland.showbiz.renderers

import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.blockentity.*
import net.minecraft.client.renderer.texture.*
import net.minecraft.resources.*
import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.blocks.entities.MonitorBlockEntity
import com.flooferland.showbiz.types.FFmpeg
import com.flooferland.showbiz.utils.rl
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import java.util.WeakHashMap

class MonitorBlockEntityRenderer(ctx: BlockEntityRendererProvider.Context) : BlockEntityRenderer<MonitorBlockEntity> {
    private val textures = WeakHashMap<MonitorBlockEntity, Pair<DynamicTexture, ResourceLocation>>()

    private fun getOrCreateTexture(entity: MonitorBlockEntity, width: Int, height: Int): Pair<DynamicTexture, ResourceLocation> {
        val id = rl("showbiz_video_${entity.blockPos.x}_${entity.blockPos.y}")
        val existing = textures[entity]
        if (existing != null) {
            val texture = existing.first
            if (texture.pixels?.width != width || texture.pixels?.height != height) {
                textures.remove(entity)
                Minecraft.getInstance().textureManager.release(id)
                return getOrCreateTexture(entity, width, height)
            }
            return existing
        }

        val texture = DynamicTexture(NativeImage(width, height, false))
        texture.setFilter(true, false)
        Minecraft.getInstance().textureManager.register(id, texture)
        val out = Pair(texture, id)
        textures[entity] = out
        return out
    }

    override fun render(entity: MonitorBlockEntity, partialTick: Float, poseStack: PoseStack, buffer: MultiBufferSource, packedLight: Int, packedOverlay: Int) {
        val facing = entity.blockState.getValue(FacingEntityBlock.FACING)
        if (!FFmpeg.serverAvailable) {
            val font = Minecraft.getInstance().font ?: return
            poseStack.pushPose()
            poseStack.translate(0.5, 0.5, 0.5)
            poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()))
            poseStack.translate(-0.18, -0.5, 0.35)
            poseStack.scale(0.01f, -0.01f, 0.01f)
            val matrix = poseStack.last().pose()
            var yOffset = 0f
            for (text in arrayOf("Install", "FFmpeg")) {
                font.drawInBatch(text, 0f, yOffset - 50f, 0xFF_FF_FF_FF.toInt(), false, matrix, buffer, Font.DisplayMode.NORMAL, 0,  LightTexture.FULL_BRIGHT)
                yOffset += font.lineHeight + 2
            }
            poseStack.popPose()
            return
        }

        val bytes = entity.video.data.bytes
        val width = entity.video.data.width
        val height = entity.video.data.height
        val channels = entity.video.data.channels
        if (width == 0 || height == 0 || bytes.isEmpty()) return

        val (texture, id) = getOrCreateTexture(entity, width, height)
        val image = texture.pixels ?: return
        for (i in 0 until width * height) {
            val r = bytes[i * channels].toInt() and 0xFF
            val g = bytes[i * channels + 1].toInt() and 0xFF
            val b = bytes[i * channels + 2].toInt() and 0xFF
            val a = if (channels == 4) bytes[i * channels + 3].toInt() and 0xFF else 0xFF
            image.setPixelRGBA(i % width, i / width, (a shl 24) or (b shl 16) or (g shl 8) or r)
        }
        texture.upload()

        poseStack.pushPose()
        poseStack.translate(0.5, 0.5, 0.5)
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 180f))
        poseStack.translate(-0.5, -0.5, -0.5)

        // What i gotta do just to get this aligned :sob:
        // Reverse winding order + flipping the pose backward
        val consumer = buffer.getBuffer(RenderType.entityCutout(id))
        val matrix = poseStack.last()
        fun vert(x: Float, y: Float, z: Float, u: Float, v: Float) =
            consumer.addVertex(matrix, x, y, z).setColor(-1).setUv(u, v).setOverlay(packedOverlay).setLight(LightTexture.FULL_BRIGHT).setNormal(matrix, 0f, 0f, -1f)
        poseStack.scale(-1f, 1f, 1f)
        poseStack.translate(-1f, 0f, 0f)
        run {
            // Manually positioning the quads to fit in the model of the monitor
            poseStack.translate(0.2f, 0.16f, 0.099f)
            poseStack.scale(0.62f, 0.62f, 1f)
        }
        vert(0f, 0f, 0f, 0f, 1f)  // 4
        vert(1f, 0f, 0f, 1f, 1f)  // 3
        vert(1f, 1f, 0f, 1f, 0f)  // 2
        vert(0f, 1f, 0f, 0f, 0f)  // 1
        poseStack.popPose()
    }
}