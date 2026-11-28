package com.flooferland.showbiz.screens.edit

import com.flooferland.showbiz.menus.CurtainControllerEditMenu
import com.flooferland.showbiz.network.packets.editscreen.CurtainControllerEditPacket
import com.flooferland.showbiz.screens.base.EditScreen
import com.flooferland.showbiz.screens.widgets.BitSelectButton
import com.flooferland.showbiz.types.CurtainState
import com.flooferland.showbiz.types.MappedBits
import com.flooferland.showbiz.utils.rl
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.gui.components.EditBox
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class CurtainControllerEditScreen(editMenu: CurtainControllerEditMenu, inventory: Inventory, title: Component) : EditScreen<CurtainControllerEditMenu, CurtainControllerEditPacket>(editMenu, inventory, title) {
    override val background = rl("textures/gui/curtain_controller.png")

    var bitOpenSelector: BitSelectButton? = null
    var bitCloseSelector: BitSelectButton? = null
    var defaultState: CycleButton<CurtainState>? = null
    var speed: EditBox? = null

    override fun addWidgets(widgets: MutableList<WidgetInfo>) {
        // Curtain open box
        run {
            bitOpenSelector = BitSelectButton(0, 0, 200, 20)
            val copy = MappedBits()
            menu.data.bitFilterOpen.forEach { (chartId, bits) -> copy.setBits(chartId, bits) }
            bitOpenSelector!!.values = copy
            widgets.add(WidgetInfo("Curtain open bits", bitOpenSelector!!))
        }

        // Curtain close box
        run {
            bitCloseSelector = BitSelectButton(0, 0, 200, 20)
            val copy = MappedBits()
            menu.data.bitFilterClose.forEach { (chartId, bits) -> copy.setBits(chartId, bits) }
            bitCloseSelector!!.values = copy
            widgets.add(WidgetInfo("Curtain close bits", bitCloseSelector!!))
        }

        // Curtain default
        run {
            defaultState = CycleButton.builder<CurtainState> { Component.literal(it.name) }
                .withValues(CurtainState.entries)
                .withInitialValue(menu.data.defaultState)
                .displayOnlyValue()
                .create(0, 0, 200, 20, Component.empty())
            widgets.add(WidgetInfo("Default state", defaultState!!))
        }

        // Curtain speed
        run {
            speed = EditBox(font, 200, 20, Component.empty())
            speed!!.value = editMenu.data.speed.toString().removeSuffix(".0")
            speed!!.setFilter { it.toFloatOrNull() != null || it.endsWith('.') || it.isEmpty() }
            widgets.add(WidgetInfo("Block speed", speed!!))
        }
    }

    override fun saveCustom(data: CurtainControllerEditPacket) {
        menu.data.bitFilterOpen.clearCharts()
        bitOpenSelector?.values?.forEach { (chartId, bits) ->
            menu.data.bitFilterOpen.setBits(chartId, bits)
        }

        menu.data.bitFilterClose.clearCharts()
        bitCloseSelector?.values?.forEach { (chartId, bits) ->
            menu.data.bitFilterClose.setBits(chartId, bits)
        }

        defaultState?.value?.let { menu.data.defaultState = it }
        speed?.value?.toFloatOrNull()?.let { menu.data.speed = it}
    }
}