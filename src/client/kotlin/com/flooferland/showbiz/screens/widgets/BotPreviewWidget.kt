package com.flooferland.showbiz.screens.widgets

import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.narration.*
import net.minecraft.client.renderer.*
import net.minecraft.client.resources.sounds.*
import net.minecraft.client.sounds.*
import net.minecraft.network.chat.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.ShowbizClient
import com.flooferland.showbiz.addons.data.AddonBotEntry
import com.flooferland.showbiz.models.BaseBotModel
import com.flooferland.showbiz.registry.ModSounds
import com.flooferland.showbiz.types.BotPreviewAnimatable
import com.flooferland.showbiz.types.ResourceId
import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.renderer.GeoRenderer

class BotPreviewWidget(x: Int, y: Int, width: Int, height: Int) : AbstractWidget(x, y, width, height, Component.empty()) {
    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) = Unit

    var bots = mapOf<ResourceId, AddonBotEntry>()
    var botId: ResourceId? = null
    var scale = 40f
    var rotation = 0f
    val previewBotAnimatable = BotPreviewAnimatable(null)
    val previewBotModel = BaseBotModel<BotPreviewAnimatable>()
    val previewBotRenderer = object : GeoRenderer<BotPreviewAnimatable> {
        override fun getGeoModel() = previewBotModel
        override fun getAnimatable() = previewBotAnimatable
        override fun fireCompileRenderLayersEvent() {}
        override fun firePreRenderEvent(poseStack: PoseStack?, model: BakedGeoModel?, bufferSource: MultiBufferSource?, partialTick: Float, packedLight: Int) = true
        override fun firePostRenderEvent(poseStack: PoseStack?, model: BakedGeoModel?, bufferSource: MultiBufferSource?, partialTick: Float, packedLight: Int) {}
        override fun updateAnimatedTextureFrame(animatable: BotPreviewAnimatable?) {}
    }

    fun renderBot(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        if (botId == null) return
        val delta = ShowbizClient.getDeltaTime()
        val model = previewBotModel
        val renderer = previewBotRenderer
        val animatable = previewBotAnimatable
        animatable.botId = botId

        guiGraphics.enableScissor(x, y, x + width, y + height)
        guiGraphics.fill(x, y, x + width, y + height, 0x88000000.toInt())
        guiGraphics.renderOutline(x, y, width, height, 0xFFFFFFFF.toInt())

        if (isHovered) {
            val rotateDir = if (mouseX < x + (width / 2)) -1f else 1f
            rotation += (rotateDir * 4f) * delta
            if (rotateDir < 0f) {
                guiGraphics.fill(x, y, x + 2, y + height, 0xFFFFFFFF.toInt())
            } else {
                guiGraphics.fill((x + width) - 2, y, x + width, y + height, 0xFFFFFFFF.toInt())
            }
        } else {
            rotation = 0f
        }

        val depth = 100.0
        val heightOffset = 0.3f * height
        val poseStack = guiGraphics.pose()
        poseStack.pushPose()
        poseStack.translate(x.toDouble() + (width / 2), y.toDouble() + (height) - heightOffset + 10, depth)
        poseStack.scale(scale, -scale, scale)
        poseStack.mulPose(Axis.XP.rotationDegrees(15f))
        poseStack.mulPose(Axis.YP.rotationDegrees(180f + 45f + rotation))

        Lighting.setupFor3DItems()
        val texture = model.getTextureResource(animatable)
        val renderType = model.getRenderType(animatable, texture) ?: return
        val buffer = guiGraphics.bufferSource().getBuffer(renderType)
        renderer.defaultRender(poseStack, animatable, guiGraphics.bufferSource(), renderType, buffer, 0f, partialTick, LightTexture.FULL_BRIGHT)
        guiGraphics.bufferSource().endBatch(renderType)
        Lighting.setupForFlatItems()
        poseStack.popPose()

        guiGraphics.disableScissor()
    }

    override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val font = Minecraft.getInstance()?.font ?: return

        val result = runCatching { renderBot(guiGraphics, mouseX, mouseY, partialTick) }
        result.onFailure { Showbiz.log.error("Failed to render bot in preview", it) }

        bots[botId]?.let { botEntry ->
            var yOffset = 1

            val title = Component.literal(botEntry.name)
            font.split(title, width).forEach { line ->
                guiGraphics.drawCenteredString(font, line, x + (width / 2), y + height + (font.lineHeight * yOffset), 0xFFFFFFFF.toInt())
                yOffset += 1
            }

            yOffset += 1
            guiGraphics.drawCenteredString(font, "Authors", x + (width / 2), y + height + (font.lineHeight * yOffset), 0xFFBABABA.toInt())
            yOffset += 1

            botEntry.authors.forEach { author ->
                guiGraphics.drawCenteredString(font, author, x + (width / 2), y + height + (font.lineHeight * yOffset), 0xFFFFFFFF.toInt())
                yOffset += 1
            }
        }
    }

    override fun playDownSound(handler: SoundManager) {
        handler.play(SimpleSoundInstance.forUI(ModSounds.Boop.event, 1.0f, 1.0f))
    }
}