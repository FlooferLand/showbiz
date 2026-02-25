package com.flooferland.showbiz.renderers

import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity
import com.flooferland.showbiz.models.StagedBotBlockEntityModel
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.*
import net.minecraft.client.*
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.blockentity.*
import net.minecraft.network.chat.*
import net.minecraft.world.level.*
import net.minecraft.world.phys.shapes.CollisionContext
import com.flooferland.showbiz.blocks.StagedBotBlock
import com.flooferland.showbiz.utils.Extensions.secsToTicks
import com.mojang.math.Axis
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.loading.math.MolangQueries
import software.bernie.geckolib.renderer.GeoBlockRenderer
import java.lang.Math.clamp

class StagedBotBlockEntityRenderer(val context: BlockEntityRendererProvider.Context) : GeoBlockRenderer<StagedBotBlockEntity>(StagedBotBlockEntityModel()) {
    override fun render(animatable: StagedBotBlockEntity, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int) {
        val level = animatable.level
        val packedLight = if (level != null) {
            val blockLight = maxOf(
                level.getBrightness(LightLayer.BLOCK, animatable.blockPos.above().above().above()),
                level.getBrightness(LightLayer.BLOCK, animatable.blockPos.above().above()),
                level.getBrightness(LightLayer.BLOCK, animatable.blockPos.above())
            )
            val skyLight = maxOf(
                level.getBrightness(LightLayer.SKY, animatable.blockPos.above().above().above()),
                level.getBrightness(LightLayer.BLOCK, animatable.blockPos.above().above()),
                level.getBrightness(LightLayer.SKY, animatable.blockPos.above())
            )
            LightTexture.pack(blockLight, skyLight)
        } else {
            LightTexture.FULL_BRIGHT
        }

        val render = runCatching {
            if (animatable.botId == null) return@runCatching
            this.animatable = animatable

            poseStack.pushPose()
            try {
                val renderColor = getRenderColor(animatable, partialTick, packedLight).argbInt()
                val packedOverlay = getPackedOverlay(animatable, 0f, partialTick)
                val model = getGeoModel().getBakedModel(getGeoModel().getModelResource(animatable, this))
                val renderType = getRenderType(animatable, getTextureLocation(animatable), bufferSource, partialTick)
                if (model == null || renderType == null) {
                    return@runCatching
                }

                val buffer = bufferSource.getBuffer(renderType)
                preRender(poseStack, animatable, model, bufferSource, buffer, false, partialTick, packedLight, packedOverlay, renderColor)
                if (firePreRenderEvent(poseStack, model, bufferSource, partialTick, packedLight)) {
                    preApplyRenderLayers(poseStack, animatable, model, renderType, bufferSource, buffer, packedLight.toFloat(), packedLight, packedOverlay)
                    actuallyRender(
                        poseStack, animatable, model, renderType,
                        bufferSource, buffer, false, partialTick, packedLight, packedOverlay, renderColor
                    )
                    applyRenderLayers(poseStack, animatable, model, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay)
                    postRender(poseStack, animatable, model, bufferSource, buffer, false, partialTick, packedLight, packedOverlay, renderColor)
                    firePostRenderEvent(poseStack, model, bufferSource, partialTick, packedLight)
                }
                renderFinal(poseStack, animatable, model, bufferSource, buffer, partialTick, packedLight, packedOverlay, renderColor)
                doPostRenderCleanup()
                MolangQueries.clearActor()
            } finally {
                poseStack.popPose()
            }
        }
        render.onFailure { throwable ->
            if (renderExceptionCountdown > 0) {
                renderExceptionCountdown -= Minecraft.getInstance().timer.gameTimeDeltaTicks
                return@onFailure
            }
            Showbiz.log.error("RENDER EXCEPTION FOR '${animatable.botId}': ", throwable)
            Minecraft.getInstance()?.player?.displayClientMessage(
                Component.literal("RENDER EXCEPTION FOR '${animatable.botId}': $throwable\n").withStyle(ChatFormatting.RED)
                    .append("This is usually caused by other errors, please check the game logs."),
                false
            )
            renderExceptionCountdown = 20.secsToTicks().toFloat()
        }
    }

    override fun preRender(poseStack: PoseStack, animatable: StagedBotBlockEntity, model: BakedGeoModel, bufferSource: MultiBufferSource?, buffer: VertexConsumer?, isReRender: Boolean, partialTick: Float, packedLight: Int, packedOverlay: Int, colour: Int) {
        poseStack.translate(0.0, 1.0, 0.0)

        // Handling carpets
        run {
            val level = animatable.level ?: return@run
            val above = level.getBlockState(animatable.blockPos.above()) ?: return@run
            if (above.isAir) return@run
            val shape = above.getShape(level, animatable.blockPos.above())
            if (!shape.isEmpty) {
                val top = clamp(shape.bounds().maxY, 0.0, 1.0)
                poseStack.translate(0.0, top, 0.0)
            }
        }

        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour)
    }

    override fun actuallyRender(poseStack: PoseStack, animatable: StagedBotBlockEntity, model: BakedGeoModel?, renderType: RenderType?, bufferSource: MultiBufferSource?, buffer: VertexConsumer?, isReRender: Boolean, partialTick: Float, packedLight: Int, packedOverlay: Int, colour: Int
    ) {
        // GigaDirection rotation
        run {
            val facing = animatable.blockState.getValue(StagedBotBlock.facing) ?: return@run
            // (((context.rotation + 180.0) * 8) / 360).roundToInt() % 8
            val angle = (360f - facing.angle) % 360f
            poseStack.mulPose(Axis.YP.rotationDegrees(angle))
        }
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour)
    }

    companion object {
        var renderExceptionCountdown = 0f
    }
}