package com.flooferland.showbiz.screens

import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.network.chat.*
import net.minecraft.util.*
import net.minecraft.world.entity.player.*
import com.flooferland.showbiz.blocks.ShowParserBlock
import com.flooferland.showbiz.menus.ShowParserEditMenu
import com.flooferland.showbiz.network.packets.ShowParserEditPacket
import com.flooferland.showbiz.screens.base.EditScreen
import com.flooferland.showbiz.utils.rl

class ShowParserEditScreen(override val editMenu: ShowParserEditMenu, inventory: Inventory, title: Component) : EditScreen<ShowParserEditMenu, ShowParserEditPacket>(editMenu, inventory, title) {
    override val background = rl("textures/gui/show_parser.png")
    val ports = rl("textures/gui/show_parser_ports.png")

    override fun addCustomWidgets(widgets: MutableList<WidgetInfo>) {
        val bitFilter = widgets.first { it.widget == bitFilterBox }
        bitFilter.side = WidgetSide.Bottom
    }

    override fun renderBackground(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick)

        val menuOwner = Minecraft.getInstance()?.level?.getBlockState(editMenu.data.base.blockPos)
        fun drawPort(top: Boolean) {
            val active = when (top) {
                true -> menuOwner?.getValue(ShowParserBlock.PLAYING_POWERED)
                false -> menuOwner?.getValue(ShowParserBlock.SIGNAL_POWERED)
            } ?: false
            val lightness = if (active) 0.8f else 0.2f
            guiGraphics.setColor(lightness, lightness, lightness, 0.8f)
            when (top) {
                true ->
                    guiGraphics.blit(ports, textureX, textureY, 0f, 0f, size, size / 2, size, size)
                false ->
                    guiGraphics.blit(ports, textureX, textureY + (size / 2), 0f, size / 2f, size, size / 2, size, size)
            }
        }
        drawPort(true)
        drawPort(false)
        guiGraphics.setColor(1f, 1f, 1f, 1f)
        guiGraphics.drawCenteredString(font, "Signal", textureX + (size / 2), textureY + (size * 0.84).toInt(), CommonColors.WHITE)
        guiGraphics.drawCenteredString(font, "Playing", textureX + (size / 2), textureY + (size * 0.15).toInt(), CommonColors.WHITE)
    }
}