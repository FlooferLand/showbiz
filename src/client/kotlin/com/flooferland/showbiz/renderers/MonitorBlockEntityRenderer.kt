package com.flooferland.showbiz.renderers

import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.blocks.entities.MonitorBlockEntity
import com.flooferland.showbiz.types.FFmpeg
import com.flooferland.showbiz.types.math.Color4
import com.flooferland.showbiz.utils.rl
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.ResourceLocation
import java.awt.image.DataBufferByte
import java.util.*
import java.util.concurrent.Executors
import javax.imageio.ImageIO

data class MonitorState(
    val texture: DynamicTexture,
    val id: ResourceLocation,
    var lastFrame: Long = 0L,
    @Volatile var decoding: Boolean = false,
    @Volatile var pending: NativeImage? = null
)

class MonitorBlockEntityRenderer(ctx: BlockEntityRendererProvider.Context) : BlockEntityRenderer<MonitorBlockEntity> {
    private val states = WeakHashMap<MonitorBlockEntity, MonitorState>()
    private val decoder = Executors.newSingleThreadExecutor { Thread(it, "showbiz-video").apply { isDaemon = true } }
    init { ImageIO.setUseCache(false) }

    private fun getOrCreateState(entity: MonitorBlockEntity, width: Int, height: Int): MonitorState {
        val id = rl("showbiz_video_${entity.blockPos.x}_${entity.blockPos.y}_${entity.blockPos.z}")
        val existing = states[entity]
        if (existing != null) {
            if (existing.texture.pixels?.width != width || existing.texture.pixels?.height != height) {
                states.remove(entity)
                Minecraft.getInstance().textureManager.release(id)
                return getOrCreateState(entity, width, height)
            }
            return existing
        }

        val texture = DynamicTexture(NativeImage(width, height, false))
        texture.setFilter(false, false)
        Minecraft.getInstance().textureManager.register(id, texture)
        return MonitorState(texture, id).also { states[entity] = it }
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

        // Returning means we just render black rn since the interior of the TV is black
        val data = entity.video.data
        val width = data.width
        val height = data.height
        if (width == 0 || height == 0 || data.bytes.isEmpty() || !Showbiz.config.video.playVideo) {
            entity.colorAverage = Color4.ZERO
            return
        }

        // Decompressing and uploading a frame
        val state = getOrCreateState(entity, width, height)
        if (data.timestamp != state.lastFrame && !state.decoding) {
            state.lastFrame = data.timestamp
            state.decoding = true
            val bytes = data.bytes
            decoder.execute {
                try {
                    val raw = bytes.size == width * height * 3
                    val src = if (raw) bytes else bytes.inputStream().use {
                        val jpg = ImageIO.read(it) ?: return@execute
                        (jpg.raster.dataBuffer as DataBufferByte).data  // BGR bytes
                    }
                    val ro = if (raw) 0 else 2
                    val image = NativeImage(width, height, false)
                    if (!Showbiz.config.video.devFastVideoRender) {
                        var sumR = 0
                        var sumG = 0
                        var sumB = 0
                        for (i in 0 until width * height) {
                            val r = src[i * 3 + ro].toInt() and 0xFF
                            val g = src[i * 3 + 1].toInt() and 0xFF
                            val b = src[i * 3 + 2 - ro].toInt() and 0xFF
                            image.setPixelRGBA(i % width, i / width, (0xFF shl 24) or (b shl 16) or (g shl 8) or r)
                            if (i % 8 == 0) {
                                sumR += r.coerceAtLeast(10)
                                sumG += g.coerceAtLeast(10)
                                sumB += b.coerceAtLeast(10)
                            }
                        }
                        val count = (width * height + 7) / 8
                        entity.colorAverage = Color4(sumR / count, sumG / count, sumB / count)
                    }
                    state.pending = image
                } finally {
                    state.decoding = false
                }
            }
        }
        state.pending?.let {
            state.pending = null
            state.texture.setPixels(it)
            state.texture.upload()
        }

        poseStack.pushPose()
        poseStack.translate(0.5, 0.5, 0.5)
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 180f))
        poseStack.translate(-0.5, -0.5, -0.5)

        // What i gotta do just to get this aligned :sob:
        // Reverse winding order + flipping the pose backward
        val consumer = buffer.getBuffer(RenderType.beaconBeam(state.id, false))
        val matrix = poseStack.last()
        fun vert(x: Float, y: Float, z: Float, u: Float, v: Float) =
            consumer.addVertex(matrix, x, y, z).setColor(-1).setUv(u, v).setOverlay(packedOverlay).setLight(LightTexture.FULL_BRIGHT).setNormal(matrix, 0f, 0f, -1f)
        poseStack.scale(-1f, 1f, 1f)
        poseStack.translate(-1f, 0f, 0f)
        run {
            // Manually positioning the quads to fit in the model of the monitor
            poseStack.translate(0.192f, 0.2f, 0.122f)
            poseStack.scale(0.61f, 0.53f, 1f)
        }
        vert(0f, 0f, 0f, 0f, 1f)  // 4
        vert(1f, 0f, 0f, 1f, 1f)  // 3
        vert(1f, 1f, 0f, 1f, 0f)  // 2
        vert(0f, 1f, 0f, 0f, 0f)  // 1
        poseStack.popPose()
    }
}