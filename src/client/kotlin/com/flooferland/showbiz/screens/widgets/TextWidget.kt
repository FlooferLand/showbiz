package com.flooferland.showbiz.screens.widgets

import net.minecraft.client.gui.*
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.narration.*
import net.minecraft.network.chat.*
import net.minecraft.util.*

class TextWidget(
    private val font: Font,
    message: Component,
    maxWidth: Int,
    private val color: Int = CommonColors.BLACK,
    private val shadow: Boolean = false
) : AbstractWidget(0, 0, maxWidth, 0, message) {
    private val lines = font.split(message, maxWidth)

    init {
        height = lines.size * font.lineHeight
    }

    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        var y = y
        for (line in lines) {
            val x = x + (width - font.width(line)) / 2
            graphics.drawString(font, line, x, y, color, shadow)
            y += font.lineHeight
        }
    }

    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {}
}