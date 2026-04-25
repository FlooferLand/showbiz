package com.flooferland.showbiz.renderers

import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.util.*
import com.flooferland.showbiz.screens.ProgrammerScreen
import com.flooferland.showbiz.types.entity.PlayerProgrammingData

object ProgrammerRenderer {
    val onColour get() = FastColor.ARGB32.color(150, 150, 150)
    val offColour get() = FastColor.ARGB32.color(50, 50, 50)
    val bgColor get() = FastColor.ARGB32.color(30, 30, 30)

    fun renderBitView(guiGraphics: GuiGraphics, data: PlayerProgrammingData) {
        val minecraft = Minecraft.getInstance() ?: return
        val player = minecraft.player ?: return
        val font = minecraft.font
        val pad = 3
        val tileHeight = 18
        val heightOffset = tileHeight + 3
        val tileWidth = 15 - 2

        guiGraphics.drawCenteredString(font, "Programming mode", guiGraphics.guiWidth() / 2, guiGraphics.guiHeight() - (heightOffset + 5 + pad + font.lineHeight), 0xFFFFFFFF.toInt())

        val keys = data.heldKeys
        for (i in 0..keys.size) {
            val isConfigKey = (i == keys.size)
            val isConfigScreen = (minecraft.screen is ProgrammerScreen)
            val pressed = ((keys.getOrNull(i) ?: false) && !isConfigScreen)
                    || (isConfigKey && isConfigScreen)
            val x = (guiGraphics.guiWidth() / 2) - 90 + i * 20 + 2 + (if (isConfigKey) 5 else 0)
            val y = guiGraphics.guiHeight() - heightOffset + (if (pressed) -1 else 0)
            val color = if (pressed) onColour else offColour

            // Drawing rects
            if (i == player.inventory.selected)
                guiGraphics.fill(x - pad - 1, y - pad - 1, x + pad + tileWidth + 1, y + tileHeight + pad + 1, onColour)
            guiGraphics.fill(x - pad, y - pad, x + pad + tileWidth, y + tileHeight + pad, bgColor)
            guiGraphics.fill(x, y, x + tileWidth, y + tileHeight, color)

            // Drawing text
            val textColor = if (pressed) 0xFFFFFFFF.toInt() else 0xFF777777.toInt()
            val text = if (isConfigKey) "0" else "${i+1}"
            guiGraphics.drawCenteredString(font, text, x + 1 + (tileWidth / 2), y + (tileHeight / 2) - 3, textColor)
        }
    }
}