package com.flooferland.showbiz.screens.widgets

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.sounds.SoundManager
import net.minecraft.network.chat.CommonComponents
import net.minecraft.resources.ResourceLocation

/** Minecraft's ImageWidget is evil */
abstract class ScaledImageWidget(x: Int, y: Int, width: Int, height: Int) : AbstractWidget(x, y, width, height, CommonComponents.EMPTY) {
    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {}
    override fun playDownSound(handler: SoundManager) {}

    private class Sprite(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        private val sprite: ResourceLocation
    ) : ScaledImageWidget(x, y, width, height) {
        override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
            graphics.blitSprite(sprite, x, y, width, height)
        }
    }

    private class Texture(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        private val texture: ResourceLocation,
        private val textureWidth: Int,
        private val textureHeight: Int
    ) : ScaledImageWidget(x, y, width, height) {
        override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
            graphics.blit(texture, x, y, width, height, 0f, 0f, textureWidth, textureHeight, textureWidth, textureHeight)
        }
    }

    companion object {
        @JvmStatic
        fun texture(width: Int, height: Int, texture: ResourceLocation, textureWidth: Int, textureHeight: Int): ScaledImageWidget =
            Texture(0, 0, width, height, texture, textureWidth, textureHeight)

        @JvmStatic
        fun sprite(width: Int, height: Int, sprite: ResourceLocation): ScaledImageWidget =
            Sprite(0, 0, width, height, sprite)
    }
}