package com.flooferland.showbiz.screens

import net.minecraft.network.chat.*
import net.minecraft.world.entity.player.*
import com.flooferland.showbiz.menus.CurtainControllerEditMenu
import com.flooferland.showbiz.network.packets.CurtainControllerEditPacket
import com.flooferland.showbiz.screens.base.EditScreen
import com.flooferland.showbiz.screens.widgets.BitSelectButton
import com.flooferland.showbiz.types.MappedBits
import com.flooferland.showbiz.utils.rl

class CurtainControllerEditScreen(editMenu: CurtainControllerEditMenu, inventory: Inventory, title: Component) : EditScreen<CurtainControllerEditMenu, CurtainControllerEditPacket>(editMenu, inventory, title) {
    override val background = rl("textures/gui/curtain_controller.png")

    var bitOpenSelector: BitSelectButton? = null
    var bitCloseSelector: BitSelectButton? = null

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
    }
}