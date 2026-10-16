package com.flooferland.showbiz.screens.edit

import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.gui.components.*
import net.minecraft.network.chat.*
import net.minecraft.util.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.level.block.*
import com.flooferland.showbiz.blocks.entities.ShowParserBlockEntity
import com.flooferland.showbiz.menus.ShowParserEditMenu
import com.flooferland.showbiz.network.packets.editscreen.ShowParserEditPacket
import com.flooferland.showbiz.screens.base.EditScreen
import com.flooferland.showbiz.screens.widgets.BitSelectButton
import com.flooferland.showbiz.types.OwnerId
import com.flooferland.showbiz.utils.rl

class ShowParserEditScreen(override val editMenu: ShowParserEditMenu, inventory: Inventory, title: Component) : EditScreen<ShowParserEditMenu, ShowParserEditPacket>(editMenu, inventory, title) {
    override val background = rl("textures/gui/show_parser.png")
    val ports = rl("textures/gui/show_parser_ports.png")

    var invert: Checkbox? = null
    var mode: CycleButton<ShowParserBlockEntity.Mode>? = null
    var bitFilter: BitSelectButton? = null

    override fun addWidgets(widgets: MutableList<WidgetInfo>) {
        // Invert
        run {
            invert = Checkbox.builder(Component.empty(), font)
                .selected(menu.data.invert)
                .build()
            widgets.add(WidgetInfo("Invert", invert!!))
        }

        // Mode
        run {
            mode = CycleButton.builder<ShowParserBlockEntity.Mode> { Component.literal(it.name) }
                .withValues(ShowParserBlockEntity.Mode.entries)
                .withInitialValue(menu.data.mode)
                .create(Component.literal("Mode")) { _, mode ->
                    bitFilter?.active = (mode == ShowParserBlockEntity.Mode.Bits)
                }
            widgets.add(WidgetInfo("Mode", mode!!))
        }

        // Bit filter
        run {
            bitFilter = BitSelectButton(200, 20)
            bitFilter!!.values = menu.data.bitFilter.clone()
            bitFilter!!.active = (mode?.value == ShowParserBlockEntity.Mode.Bits)
            widgets.add(WidgetInfo("Bit Filter", bitFilter!!))
        }
    }

    override fun saveCustom(data: ShowParserEditPacket) {
        menu.data.bitFilter.clearCharts()
        bitFilter?.values?.forEach { (chartId, bits) ->
            menu.data.bitFilter.setBits(chartId, bits)
        }

        mode?.value?.let { menu.data.mode = it }
        invert?.selected()?.let { menu.data.invert = it }
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
        guiGraphics.drawCenteredString(font, "Note that some things might change", textureX + (size / 2), textureY + (size * 0.84).toInt(), CommonColors.WHITE)
    }
}