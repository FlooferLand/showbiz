package com.flooferland.showbiz.screens

import net.minecraft.ChatFormatting
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.network.chat.*
import net.minecraft.world.entity.player.*
import com.flooferland.showbiz.menus.CurtainControllerEditMenu
import com.flooferland.showbiz.network.packets.CurtainControllerEditPacket
import com.flooferland.showbiz.screens.base.EditScreen
import com.flooferland.showbiz.show.toBitIdOrNull
import com.flooferland.showbiz.utils.rl

class CurtainControllerEditScreen(editMenu: CurtainControllerEditMenu, inventory: Inventory, title: Component) : EditScreen<CurtainControllerEditMenu, CurtainControllerEditPacket>(editMenu, inventory, title) {
    override val background = rl("textures/gui/curtain_controller.png")

    var bitOpenFilterBox: EditBox? = null
    var bitCloseFilterBox: EditBox? = null

    override fun addWidgets(widgets: MutableList<WidgetInfo>) {
        // Curtain open box
        run {
            bitOpenFilterBox = EditBox(font, 200, 20, Component.literal("Open filter"))
            bitOpenFilterBox!!.tooltip =
                Tooltip.create(Component.literal("Enter one or more bit numbers that the curtains will open for (comma-separated)"))
            bitOpenFilterBox!!.value = menu.data.bitFilterOpen.joinToString(",")
            bitOpenFilterBox!!.setFormatter { text, _ ->
                val result = Component.empty()
                if (text.contains(',')) {
                    for (char in text) {
                        if (char == ',') {
                            result.append(Component.literal("$char ").withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD))
                        } else {
                            result.append(char.toString())
                        }
                    }
                } else {
                    result.append(text)
                }
                result.visualOrderText
            }
            widgets.add(WidgetInfo("Curtain open bits", bitOpenFilterBox!!))
        }

        // Curtain close box
        run {
            bitCloseFilterBox = EditBox(font, 200, 20, Component.literal("Close filter"))
            bitCloseFilterBox!!.tooltip =
                Tooltip.create(Component.literal("Enter one or more bit numbers that the curtains will close for (comma-separated)"))
            bitCloseFilterBox!!.value = menu.data.bitFilterClose.joinToString(",")
            bitCloseFilterBox!!.setFormatter { text, _ ->
                val result = Component.empty()
                if (text.contains(',')) {
                    for (char in text) {
                        if (char == ',') {
                            result.append(Component.literal("$char ").withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD))
                        } else {
                            result.append(char.toString())
                        }
                    }
                } else {
                    result.append(text)
                }
                result.visualOrderText
            }
            widgets.add(WidgetInfo("Curtain close bits", bitCloseFilterBox!!))
        }
    }

    override fun saveCustom(data: CurtainControllerEditPacket) {
        menu.data.bitFilterOpen.clear()
        bitOpenFilterBox?.let { filterBox ->
            filterBox.value.split(',').forEach { entry ->
                entry.toBitIdOrNull()?.let { menu.data.bitFilterOpen.add(it) }
            }
        }

        menu.data.bitFilterClose.clear()
        bitCloseFilterBox?.let { filterBox ->
            filterBox.value.split(',').forEach { entry ->
                entry.toBitIdOrNull()?.let { menu.data.bitFilterClose.add(it) }
            }
        }
    }
}