package com.flooferland.showbiz.screens

import net.minecraft.client.gui.components.*
import net.minecraft.network.chat.*
import net.minecraft.world.entity.player.*
import com.flooferland.showbiz.menus.SpotlightEditMenu
import com.flooferland.showbiz.network.packets.SpotlightEditPacket
import com.flooferland.showbiz.screens.base.EditScreen
import com.flooferland.showbiz.screens.widgets.ColorPicker
import com.flooferland.showbiz.utils.rl

// TODO: Should turn this into a general light edit screen? Floodlights could use it

class SpotlightEditScreen(editMenu: SpotlightEditMenu, inventory: Inventory, title: Component) : EditScreen<SpotlightEditMenu, SpotlightEditPacket>(editMenu, inventory, title) {
    override val background = rl("textures/gui/spotlight.png")

    var turn: EditBox? = null
    var angle: EditBox? = null
    var color: ColorPicker? = null
    var shadows: Checkbox? = null

    override fun addCustomWidgets(widgets: MutableList<WidgetInfo>) {
        // Turn X
        run {
            turn = EditBox(font, 80, 20, Component.literal("Turn"))
            turn!!.value =
                editMenu.data.turn.x.toString().replace(".0", "") + ", " + editMenu.data.turn.y.toString().replace(".0", "")
            turn!!.setFilter { turn -> turn.split(',').map { it.trim() }.all { it.toFloatOrNull() != null || it.isEmpty() || it.startsWith('-') } }
            turn!!.tooltip = Tooltip.create(Component.literal("Turn X and Y separared by a comma"))
            widgets.add(WidgetInfo("Turn X/Y", turn!!))
        }

        // Angle (radius)
        run {
            angle = EditBox(font, 40, 20, Component.literal("Angle"))
            angle!!.tooltip = Tooltip.create(Component.literal("Angular radius"))
            angle!!.value = editMenu.data.angle.toString().replace(".0", "")
            angle!!.setFilter { it.toFloatOrNull() != null || it.isEmpty() }
            widgets.add(WidgetInfo("Angle", angle!!))
        }

        // Color
        run {
            color = ColorPicker(0, 0, 160, 40, defaultKelvin = editMenu.data.kelvin)
            color!!.allowedModes = mutableSetOf(ColorPicker.Mode.Kelvin)
            widgets.add(WidgetInfo("Color", color!!))
        }

        // Shadows
        run {
            shadows = Checkbox.builder(Component.literal("Shadows"), font)
                .selected(editMenu.data.shadows)
                .build()
            widgets.add(WidgetInfo(null, shadows!!))
        }
    }

    override fun saveCustom(data: SpotlightEditPacket) {
        turn?.value?.split(',')?.let { values ->
            val values = values.map { it.trim() }.mapNotNull { it.toFloatOrNull() }
            if (values.size >= 2) {
                data.turn.x = values[0]
                data.turn.y = values[1]
            }
        }
        angle?.value?.toFloatOrNull()?.let { data.angle = it.coerceIn(1f, 180f) }
        color?.valueKelvin?.let { data.kelvin = it }
        shadows?.selected()?.let { data.shadows = it }
    }
}