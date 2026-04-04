package com.flooferland.showbiz.screens

import net.minecraft.client.gui.components.*
import net.minecraft.network.chat.*
import net.minecraft.world.entity.player.*
import com.flooferland.showbiz.menus.SpotlightEditMenu
import com.flooferland.showbiz.network.packets.SpotlightEditPacket
import com.flooferland.showbiz.screens.base.EditScreen
import com.flooferland.showbiz.utils.rl

class SpotlightEditScreen(editMenu: SpotlightEditMenu, inventory: Inventory, title: Component) : EditScreen<SpotlightEditMenu, SpotlightEditPacket>(editMenu, inventory, title) {
    override val background = rl("textures/gui/spotlight.png")

    var turnX: EditBox? = null
    var turnY: EditBox? = null
    var angle: EditBox? = null

    override fun addCustomWidgets(widgets: MutableList<WidgetInfo>) {
        // Turn X
        run {
            turnX = EditBox(font, 40, 20, Component.literal("Turn X"))
            turnX!!.value = editMenu.data.turn.x.toString().replace(".0", "")
            turnX!!.setFilter { it.toFloatOrNull() != null || it.isEmpty() || it.startsWith('-') }
            widgets.add(WidgetInfo("Turn X", turnX!!))
        }

        // Turn Y
        run {
            turnY = EditBox(font, 40, 20, Component.literal("Turn Y"))
            turnY!!.value = editMenu.data.turn.y.toString().replace(".0", "")
            turnY!!.setFilter { it.toFloatOrNull() != null || it.isEmpty() || it.startsWith('-') }
            widgets.add(WidgetInfo("Turn Y", turnY!!))
        }

        // Angle (radius)
        run {
            angle = EditBox(font, 40, 20, Component.literal("Angle"))
            angle!!.tooltip = Tooltip.create(Component.literal("Angular radius"))
            angle!!.value = editMenu.data.angle.toString().replace(".0", "")
            angle!!.setFilter { it.toFloatOrNull() != null || it.isEmpty() }
            widgets.add(WidgetInfo("Angle", angle!!))
        }
    }

    override fun saveCustom(data: SpotlightEditPacket) {
        turnX?.value?.toFloatOrNull()?.let { data.turn.x = it }
        turnY?.value?.toFloatOrNull()?.let { data.turn.y = it }
        angle?.value?.toFloatOrNull()?.let { data.angle = it }
    }
}