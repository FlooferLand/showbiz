package com.flooferland.showbiz.renderers

import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.util.*
import com.flooferland.showbiz.screens.ProgrammerScreen
import com.flooferland.showbiz.types.entity.PlayerProgrammingData
import com.flooferland.showbiz.types.math.Vec2ic
import com.flooferland.showbiz.utils.rl

object ProgrammerRenderer {
    val slotTextureOn = rl("textures/gui/sprites/hud/programbar_slot_on.png")
    val slotTextureOff = rl("textures/gui/sprites/hud/programbar_slot_off.png")
    val slotTextureSelect = rl("textures/gui/sprites/hud/programbar_slot_select.png")
    val slotTextureSize = Vec2ic(16, 26)

    fun renderBitView(guiGraphics: GuiGraphics, data: PlayerProgrammingData) {
        val minecraft = Minecraft.getInstance() ?: return
        val player = minecraft.player ?: return
        val font = minecraft.font
        val keys = data.heldKeys

        val pad = 3
        val tileWidth = slotTextureSize.x
        val tileHeight = slotTextureSize.y
        val heightOffset = tileHeight + 1
        val totalWidth = keys.size * tileWidth

        guiGraphics.drawCenteredString(font, "Programming mode", guiGraphics.guiWidth() / 2, guiGraphics.guiHeight() - (heightOffset + 5 + pad + font.lineHeight), 0xFFFFFFFF.toInt())

        for (i in 0..keys.size) {
            val isConfigKey = (i == keys.size)
            val isConfigScreen = (minecraft.screen is ProgrammerScreen)
            val pressed = ((keys.getOrNull(i) ?: false) && !isConfigScreen)
                    || (isConfigKey && isConfigScreen)
            val x = (guiGraphics.guiWidth() / 2) - (totalWidth / 2) + (i * tileWidth) + (if (isConfigKey) 5 else 0)
            val y = guiGraphics.guiHeight() - heightOffset + (if (pressed) -1 else 0)

            // Drawing textures
            guiGraphics.blit(if (pressed) slotTextureOn else slotTextureOff, x, y, 0f, 0f, tileWidth, tileHeight, tileWidth, tileHeight)
            if (i == player.inventory.selected)
                 guiGraphics.blit(slotTextureSelect, x, y, 0f, 0f, tileWidth, tileHeight, tileWidth, tileHeight)
        }
    }
}