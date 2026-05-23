package com.flooferland.showbiz.screens

import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.screens.*
import net.minecraft.network.chat.*
import com.flooferland.showbiz.network.packets.ProgrammerPlayerUpdatePacket
import com.flooferland.showbiz.screens.widgets.BitSelectButton
import com.flooferland.showbiz.types.entity.PlayerProgrammingData
import com.flooferland.showbiz.utils.rl
import com.mojang.blaze3d.systems.RenderSystem
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import kotlin.math.roundToInt

class ProgrammerScreen : Screen(Component.literal("Programmer")) {
    override fun isPauseScreen() = false

    val texSize = 256
    val background = rl("textures/gui/programmer.png")
    val size get() = (texSize * 1.5).roundToInt()
    val textureX get() = (width / 2) - (size / 2)
    val textureY get() = (height / 2) - (size / 2)

    val inputs = mutableListOf<BitSelectButton>()

    override fun init() {
        val player = Minecraft.getInstance().player ?: return
        val data = PlayerProgrammingData.getFromPlayer(player)

        inputs.clear()
        for ((i, mappedBits) in data.keysToBits.withIndex()) {
            val spacing = 20
            val height = 15
            val name = "Key ${i+1}"
            val leftSpace = font.width(name)
            val input = BitSelectButton(0, 0, 80, height)
            input.values = mappedBits
            val x = textureX + (input.width / 2) + leftSpace + (spacing * 2)
            val y = textureY + 10 + (size * 0.2f).toInt() + (i * spacing)
            run {
                val nameWidget = StringWidget(x, y, leftSpace, height, Component.literal(name), font).alignLeft()
                nameWidget.tooltip = Tooltip.create(Component.literal("$name in your number row"))
                addRenderableWidget(nameWidget)
            }
            input.setPosition(x + leftSpace + 5, y)
            addRenderableWidget(input)
            inputs.add(input)
        }
    }

    override fun onClose() {
        val player = Minecraft.getInstance().player ?: return
        val data = PlayerProgrammingData.getFromPlayer(player)
        inputs.forEachIndexed { i, input ->
            data.keysToBits[i] = input.values
        }
        data.saveToPlayer(player)
        ClientPlayNetworking.send(ProgrammerPlayerUpdatePacket(keysToBits = data.keysToBits))
        super.onClose()
    }

    override fun renderBackground(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        RenderSystem.enableBlend()
        guiGraphics.setColor(1f, 1f, 1f, 1f)
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick)

        RenderSystem.enableBlend()
        guiGraphics.setColor(0.5f, 0.5f, 0.5f, 0.9f)
        guiGraphics.blit(background, textureX, textureY, 0f, 0f, size, size, size, size)

        RenderSystem.defaultBlendFunc()
        guiGraphics.setColor(1f, 1f, 1f, 1f)
    }
}