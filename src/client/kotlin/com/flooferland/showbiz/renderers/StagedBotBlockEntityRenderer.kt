package com.flooferland.showbiz.renderers

import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.blockentity.*
import net.minecraft.world.level.*
import com.flooferland.showbiz.ShowbizClient
import com.flooferland.showbiz.blocks.StagedBotBlock
import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity
import com.flooferland.showbiz.models.BaseBotModel
import com.flooferland.showbiz.models.StagedBotBlockEntityModel
import com.flooferland.showbiz.types.InteractionRenderHook
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import java.lang.Math.clamp
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.cache.`object`.GeoBone
import software.bernie.geckolib.loading.math.MolangQueries
import software.bernie.geckolib.renderer.GeoBlockRenderer
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer

class StagedBotBlockEntityRenderer(val context: BlockEntityRendererProvider.Context) : GeoBlockRenderer<StagedBotBlockEntity>(StagedBotBlockEntityModel()) {
    val hook = InteractionRenderHook()

    init {
        addRenderLayer(AutoGlowingGeoLayer(this))
    }

    override fun renderCubesOfBone(poseStack: PoseStack, bone: GeoBone, buffer: VertexConsumer?, packedLight: Int, packedOverlay: Int, colour: Int) {
        hook.beforeRenderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, colour)
        super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, colour)
    }

    override fun preRender(poseStack: PoseStack, animatable: StagedBotBlockEntity, model: BakedGeoModel, bufferSource: MultiBufferSource?, buffer: VertexConsumer?, isReRender: Boolean, partialTick: Float, packedLight: Int, packedOverlay: Int, colour: Int) {
        hook.beforePreRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour)
        if (!isReRender) {
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
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour)
    }

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

        if (animatable.botId == null) return
        poseStack.pushPose()
        try {
            val botModel = model as StagedBotBlockEntityModel
            this.animatable = animatable

            val renderColor = getRenderColor(animatable, partialTick, packedLight).argbInt()
            val packedOverlay = getPackedOverlay(animatable, 0f, partialTick)
            val modelLocation = getGeoModel().getModelResource(animatable, this)
            if (modelLocation == null || !ShowbizClient.botModels.containsKey(modelLocation)) {
                error("No model found for bot '${animatable.botId}'")
            }
            val model = getGeoModel().getBakedModel(modelLocation)
            val renderType = getRenderType(animatable, getTextureLocation(animatable), bufferSource, partialTick)
            if (model == null || renderType == null) {
                error("No model or render type found")
            }

            val buffer = bufferSource.getBuffer(renderType)
            preRender(poseStack, animatable, model, bufferSource, buffer, false, partialTick, packedLight, packedOverlay, renderColor)
            if (firePreRenderEvent(poseStack, model, bufferSource, partialTick, packedLight)) {
                val hasGlow = botModel.hasGlowTexture(animatable)
                for (renderLayer in getRenderLayers()) {
                    if (renderLayer is AutoGlowingGeoLayer && !hasGlow) continue
                    renderLayer.preRender(poseStack, animatable, model, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay)
                }
                actuallyRender(
                    poseStack, animatable, model, renderType,
                    bufferSource, buffer, false, partialTick, packedLight, packedOverlay, renderColor
                )
                for (renderLayer in getRenderLayers()) {
                    if (renderLayer is AutoGlowingGeoLayer && !hasGlow) continue
                    renderLayer.render(poseStack, animatable, model, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay)
                }
                postRender(poseStack, animatable, model, bufferSource, buffer, false, partialTick, packedLight, packedOverlay, renderColor)
                firePostRenderEvent(poseStack, model, bufferSource, partialTick, packedLight)
            }
            renderFinal(poseStack, animatable, model, bufferSource, buffer, partialTick, packedLight, packedOverlay, renderColor)
            doPostRenderCleanup()
            MolangQueries.clearActor()
        } catch (throwable: Throwable) {
            val message = throwable.toString()
            if (!BaseBotModel.errorsTriggered.any { it.context == message }) {
                BaseBotModel.errorsTriggered.add(BaseBotModel.Error.RenderException.withBot(animatable).withContext(message))
            }
        }
        poseStack.popPose()
    }

    override fun actuallyRender(poseStack: PoseStack, animatable: StagedBotBlockEntity, model: BakedGeoModel?, renderType: RenderType?, bufferSource: MultiBufferSource?, buffer: VertexConsumer?, isReRender: Boolean, partialTick: Float, packedLight: Int, packedOverlay: Int, colour: Int) {
        // GigaDirection rotation
        if (!isReRender) run {
            val facing = animatable.blockState.getValue(StagedBotBlock.facing) ?: return@run
            val angle = (360f - facing.angle) % 360f
            poseStack.mulPose(Axis.YP.rotationDegrees(angle))
        }
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour)
    }

    override fun postRender(poseStack: PoseStack, animatable: StagedBotBlockEntity, model: BakedGeoModel, bufferSource: MultiBufferSource, buffer: VertexConsumer?, isReRender: Boolean, partialTick: Float, packedLight: Int, packedOverlay: Int, colour: Int) {
        hook.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour)
    }

    companion object {
        var renderExceptionCountdown = 0f
    }
}