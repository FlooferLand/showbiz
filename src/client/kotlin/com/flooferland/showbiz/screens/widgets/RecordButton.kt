package com.flooferland.showbiz.screens.widgets

import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.narration.*
import net.minecraft.network.chat.*
import com.flooferland.showbiz.types.math.Vec2ic
import com.flooferland.showbiz.utils.rl
import com.mojang.blaze3d.systems.RenderSystem

class RecordButton(x: Int, y: Int) : AbstractButton(x, y, 70, 15, Component.literal("Record")) {
    val recordOnSprite = rl("textures/gui/sprites/recording_online.png")
    val recordOffSprite = rl("textures/gui/sprites/recording_offline.png")
    val recordSpriteSize = Vec2ic(8, 8)

    public var recording: Boolean = false

    init { update() }

    fun update() {
        message = if (recording) Component.literal("Recording") else Component.literal("Record")
    }

    override fun onPress() {
        recording = !recording
        update()
    }

    override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val minecraft = Minecraft.getInstance()
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, this.alpha)
        RenderSystem.enableBlend()
        RenderSystem.enableDepthTest()

        guiGraphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF000000.toInt())
        guiGraphics.fill(x, y, x + width, y + height, 0xAA222222.toInt())
        guiGraphics.blit(
            if (recording) recordOnSprite else recordOffSprite,
            x + 5,
            y + 4,
            0f, 0f,
            recordSpriteSize.x, recordSpriteSize.y,
            recordSpriteSize.x, recordSpriteSize.y
        )

        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f)
        guiGraphics.drawString(minecraft.font, message, x + (recordSpriteSize.x * 2), y + ((height - 6) / 2), if (recording) 0xFFFFFFFF.toInt() else 0xFFAAAAAA.toInt())
    }

    override fun updateWidgetNarration(narration: NarrationElementOutput) {
        narration.add(NarratedElementType.HINT, "Record button")
    }
}