package com.flooferland.showbiz.screens

import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.network.chat.*
import net.minecraft.util.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.level.block.*
import com.flooferland.showbiz.menus.ShowParserEditMenu
import com.flooferland.showbiz.network.packets.ShowParserEditPacket
import com.flooferland.showbiz.screens.base.EditScreen
import com.flooferland.showbiz.types.OwnerId
import com.flooferland.showbiz.utils.rl

class ShowParserEditScreen(override val editMenu: ShowParserEditMenu, inventory: Inventory, title: Component) : EditScreen<ShowParserEditMenu, ShowParserEditPacket>(editMenu, inventory, title) {
    override val background = rl("textures/gui/show_parser.png")
    val ports = rl("textures/gui/show_parser_ports.png")

    override fun addCustomWidgets(widgets: MutableList<WidgetInfo>) {
        val bitFilter = widgets.first { it.widget == bitSelector }
        bitFilter.side = WidgetSide.Bottom
    }

    override fun renderBackground(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick)

        val id = editMenu.data.base.id as? OwnerId.BlockId ?: return
        val level = Minecraft.getInstance().level ?: return
        val menuOwner = id.grabBlockState(level)
        val active = menuOwner.getValue(DiodeBlock.POWERED)
        val lightness = if (active) 0.8f else 0.2f
        guiGraphics.setColor(lightness, lightness, lightness, 0.8f)
        guiGraphics.blit(ports, textureX, textureY, 0f, 0f, size, size / 2, size, size)
        guiGraphics.setColor(1f, 1f, 1f, 1f)
        guiGraphics.drawCenteredString(font, "Signal", textureX + (size / 2), textureY + (size * 0.84).toInt(), CommonColors.WHITE)
    }
}