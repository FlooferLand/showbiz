package com.flooferland.showbiz.renderers

import net.minecraft.client.*
import net.minecraft.client.gui.*
import com.flooferland.showbiz.screens.ProgrammerScreen
import com.flooferland.showbiz.types.entity.PlayerProgrammingData
import com.flooferland.showbiz.types.math.Vec2ic
import com.flooferland.showbiz.utils.rl
import com.mojang.blaze3d.systems.RenderSystem

object ProgrammerRenderer {
    val settingsTextureOn = rl("textures/gui/sprites/hud/programbar_settings_on.png")
    val settingsTextureOff = rl("textures/gui/sprites/hud/programbar_settings_off.png")
    val settingsTextureSize = Vec2ic(16, 24)

    val slotTextureOn = rl("textures/gui/sprites/hud/programbar_slot_on.png")
    val slotTextureOff = rl("textures/gui/sprites/hud/programbar_slot_off.png")
    val slotTextureSelect = rl("textures/gui/sprites/hud/programbar_slot_select.png")
    val slotTextureSize = Vec2ic(15, 26)

    fun renderBitView(guiGraphics: GuiGraphics, data: PlayerProgrammingData) {
        val minecraft = Minecraft.getInstance() ?: return
        val player = minecraft.player ?: return
        val font = minecraft.font
        val keys = data.heldKeys

        val pad = 2
        guiGraphics.drawCenteredString(font, "Programming mode", guiGraphics.guiWidth() / 2, guiGraphics.guiHeight() - (24 + 5 + pad + font.lineHeight), 0xFFFFFFFF.toInt())

        for (i in 0..keys.size) {
            val isConfigKey = (i == keys.size)
            val isConfigScreen = (minecraft.screen is ProgrammerScreen)
            val pressed = ((keys.getOrNull(i) ?: false) && !isConfigScreen)
                    || (isConfigKey && isConfigScreen)
            val size = if (isConfigKey)
                Vec2ic(settingsTextureSize.x, settingsTextureSize.y)
                else Vec2ic(slotTextureSize.x, slotTextureSize.y)
            val dist = slotTextureSize.x
            val totalWidth = keys.size * dist
            val heightOffset = size.y + 1 + (if (isConfigKey) 1 else 0)
            val x = ((guiGraphics.guiWidth() / 2) - (totalWidth / 2)) + (i * dist) + (if (isConfigKey) 1 else 0)
            val y = guiGraphics.guiHeight() - heightOffset + (if (pressed) -1 else 0)

            val sprite = if (isConfigKey) (if (pressed) settingsTextureOn else settingsTextureOff)
                else (if (pressed) slotTextureOn else slotTextureOff)

            // Drawing textures
            if (isConfigKey && data.recording) {
                guiGraphics.fill(x - 1, y - 1, x + size.x + 1, y + size.y + 1, 0xFFFF0000.toInt())
                RenderSystem.setShaderColor(1f, 0.8f, 0.8f, 1f)
            }
            guiGraphics.blit(sprite, x, y, 0f, 0f, size.x, size.y, size.x, size.y)
            if (i == player.inventory.selected)
                 guiGraphics.blit(slotTextureSelect, x, y, 0f, 0f, size.x, size.y, size.x, size.y)
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        }
    }
}