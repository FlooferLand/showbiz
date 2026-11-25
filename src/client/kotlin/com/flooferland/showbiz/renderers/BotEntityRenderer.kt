package com.flooferland.showbiz.renderers

import com.flooferland.showbiz.ShowbizClient
import com.flooferland.showbiz.entities.BotEntity
import com.flooferland.showbiz.models.BotModel
import com.flooferland.showbiz.renderers.base.GeoFixedEntityRenderer
import com.flooferland.showbiz.renderers.layers.BetterAutoGlowingGeoLayer
import com.flooferland.showbiz.types.BotError
import com.flooferland.showbiz.types.BotErrors
import com.flooferland.showbiz.utils.DrawUtils
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.network.chat.Component

class BotEntityRenderer(ctx: EntityRendererProvider.Context) : GeoFixedEntityRenderer<BotEntity>(ctx, BotModel()) {
    val glow = BetterAutoGlowingGeoLayer(this)
    init { addRenderLayer(glow) }

    override fun shouldShowName(entity: BotEntity) = false
    override fun shouldRender(livingEntity: BotEntity, camera: Frustum, camX: Double, camY: Double, camZ: Double): Boolean {
        // TODO: Figure out a better fix to solve Sodium cutting off entities early. Bad for performance.
        //       Not sure how to dynamically scale the entity dimensions based on the bot size cause of addons, or if that's even a good idea
        val texture =
        return true;
    }

    @Suppress("UnstableApiUsage")
    override fun render(entity: BotEntity, entityYaw: Float, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int) {
        glow.shouldRender = (geoModel as? BotModel)?.hasGlowTexture(entity) ?: false

        try {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight)
        } catch (throwable: Throwable) {
            BotErrors.add(BotError.RenderException().withBot(animatable).withContext(throwable))
        }
    }

    companion object {
        fun render(mc: Minecraft, buffer: MultiBufferSource.BufferSource, poseStack: PoseStack) {
            // Debug screen
            val entity = mc.crosshairPickEntity
            if (mc.debugOverlay?.showDebugScreen() == true && entity is BotEntity) {
                val bot = ShowbizClient.bots[entity.botId] ?: return
                val text = Component.empty()
                listOf(
                    Component.literal("${entity.botId}:${entity.botVariant}"),
                    Component.literal("Show playing=${entity.show.data.playing} map=${entity.show.data.mapping}"),
                    Component.literal("Bot mappings={${bot.bitmap.bits.map { "${it.key}=${it.value.size}" }.joinToString(" ")}}")
                ).forEach { text.append(it.append("\n")) }
                DrawUtils.drawText(poseStack, entity.eyePosition.add(0.0, 0.5, 0.0), text, buffer) {
                    maxWidth = 300
                    vAlign = 1f
                }
            }

            // Errors
            // Gets called for staged bots as well
            for (err in BotErrors.getAll()) {
                val entity = err.bot ?: continue
                val pos = entity.botPos?.add(0.0, 0.5, 0.0) ?: continue
                val text = Component.literal(err.context ?: "X").withStyle(ChatFormatting.RED)

                DrawUtils.drawText(poseStack, pos, text, buffer) {
                    maxWidth = 150
                    depth = false
                    vAlign = 1f
                }

                DrawUtils.drawText(poseStack, pos, Component.empty(), buffer) {}
            }
        }
    }
}