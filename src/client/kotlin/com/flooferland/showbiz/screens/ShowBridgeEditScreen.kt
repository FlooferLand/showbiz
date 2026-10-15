package com.flooferland.showbiz.screens

import net.minecraft.*
import net.minecraft.client.*
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.layouts.*
import net.minecraft.network.chat.*
import net.minecraft.util.*
import net.minecraft.world.entity.player.*
import com.fazecast.jSerialComm.SerialPort
import com.flooferland.showbiz.Permissions
import com.flooferland.showbiz.menus.ShowBridgeEditMenu
import com.flooferland.showbiz.network.packets.editscreen.ShowBridgeEditPacket
import com.flooferland.showbiz.screens.base.EditScreen
import com.flooferland.showbiz.screens.widgets.BitSelectButton
import com.flooferland.showbiz.screens.widgets.TextWidget
import com.flooferland.showbiz.types.showbridge.ShowBridgeMethod
import com.flooferland.showbiz.types.showbridge.ShowBridgeProfile
import com.flooferland.showbiz.utils.rl

class ShowBridgeEditScreen(editMenu: ShowBridgeEditMenu, inventory: Inventory, title: Component) : EditScreen<ShowBridgeEditMenu, ShowBridgeEditPacket>(editMenu, inventory, title) {
    override val background = rl("textures/gui/show_bridge.png")

    var selectedTab = Tab.entries.firstOrNull { getProfile(it).enabled } ?: Tab.Player
    val profile get() = getProfile(selectedTab)

    enum class Tab { Player, Server }

    fun getProfile(tab: Tab): ShowBridgeProfile =
        if (tab == Tab.Player) menu.data.playerProfile else menu.data.serverProfile

    fun canEditServer(): Boolean {
        return Minecraft.getInstance().player?.let { Permissions.EditServerShowBridge.test(it) } ?: false
    }

    fun buildProfile(layout: LinearLayout) {
        if (selectedTab == Tab.Server && !canEditServer()) {
            val text = Component.literal("You don't have the permission to edit this page")
            layout.addChild(TextWidget(font, text, contentWidth))
            return
        }

        fun <T> field(label: String, description: String? = null, block: () -> T): T where T: AbstractWidget {
            val widget = block()
            val text = Component.literal(label)
            description?.let { text.append(Component.literal(" ($it)").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)) }
            layout.addChild(StringWidget(text, font))
            layout.addChild(widget)
            layout.addChild(SpacerElement.height(5))
            return widget
        }

        field("Bit filter", "off when empty") {
            BitSelectButton(0, 0, 200, 20).also { button ->
                button.values = profile.bitFilter.clone()
                button.onChanged = { profile.bitFilter = it.clone() }
            }
        }

        field("Serial port") {
            EditBox(font, 60, 20, Component.literal("Serial port")).also { port ->
                val ports = editMenu.data.serverPorts
                port.value = profile.serialPort
                port.tooltip = Tooltip.create(Component.literal("Case sensitive"))
                port.setResponder { text ->
                    profile.serialPort = text
                    val suggestion = ports.firstOrNull { it.startsWith(text, ignoreCase = true) } ?: ""
                    port.setSuggestion(if (suggestion.length > port.cursorPosition) suggestion.substring(port.cursorPosition) else "")
                }
            }
        }

        field("Serial baud rate") {
            EditBox(font, 60, 20, Component.literal("Serial baud rate")).also { baud ->
                baud.value = profile.serialBaudRate.toString()
                baud.setFilter { text -> text.all { it.isDigit() } && text.length < 8 }
                baud.setResponder { profile.serialBaudRate = it.toIntOrNull() ?: profile.serialBaudRate }
            }
        }

        layout.addChild(SpacerElement.height(20))
        field("Method") {
            CycleButton.builder<ShowBridgeMethod> { Component.literal(it.name) }
                .withValues(ShowBridgeMethod.entries)
                .withInitialValue(profile.method)
                .displayOnlyValue()
                .create(Component.empty()) { _, v ->
                    profile.method = v
                    rebuildWidgets()
                }.also { it.width = 80 }
        }

        // Method specific options
        when (profile.method) {
            ShowBridgeMethod.TextList -> {
                val list = profile.textList
                field("Separator") {
                    fun tooltip() = Component.literal("Length ${list.separator.length}\n").append("Separator character between the bits")
                    val edit = EditBox(font, 40, 20, Component.literal("Separator"))
                    edit.value = list.separator
                    edit.tooltip = Tooltip.create(tooltip())
                    edit.setResponder { v ->
                        list.separator = v
                        edit.tooltip = Tooltip.create(tooltip())
                    }
                    edit
                }
            }
            ShowBridgeMethod.BinaryList -> {
                // TODO
            }
        }

        // Toggling on/off if its enabled
        layout.visitWidgets { it.active = profile.enabled }
    }

    override fun buildUi() {
        val x = textureX + 75
        val y = textureY.coerceAtLeast(0) + 20

        // Profile tabs
        val text = Component.literal("Profiles")
        val name = StringWidget(x, y, font.width(text), 20, text, font).alignLeft()
        addRenderableWidget(name)

        var profileX = name.width + 20
        for (tab in Tab.entries) {
            val profile = getProfile(tab)

            // Enable checkbox
            val enableCheckbox = Checkbox.builder(Component.empty(), font)
                .pos(x + profileX, y)
                .selected(profile.enabled)
                .onValueChange { _, v ->
                    profile.enabled = v
                    rebuildWidgets()
                }
                .build()
            addRenderableWidget(enableCheckbox)
            profileX += 18

            // Tab button
            val tabButton = Button.builder(Component.literal(tab.name)) { selectedTab = tab; rebuildWidgets() }
                .pos(x + profileX, y)
                .size(60, enableCheckbox.height)
                .build()
            tabButton.active = selectedTab != tab
            addRenderableWidget(tabButton)
            profileX += tabButton.width + 10
        }

        // Serial port info
        run {
            val ports = when (selectedTab) {
                Tab.Player -> SerialPort.getCommPorts().map { it.systemPortName }
                Tab.Server -> menu.data.serverPorts
            }

            val lines = mutableListOf<Component>()
            lines += Component.literal("$selectedTab serial ports")
            ports.forEach { lines += Component.literal("- $it") }

            val x = 20
            val y = y + (lines.size * font.lineHeight / 2)
            lines.forEachIndexed { i, text ->
                val name = StringWidget(x, y + (i * font.lineHeight), font.width(text), font.lineHeight, text, font).alignLeft()
                addRenderableWidget(name)
            }
        }

        // Bottom output
        run {
            val x = x
            val y = height - 30
            val output = EditBox(font, x, y, texSize - 20, 20, Component.literal("Output"))
            output.setMaxLength(512)
            output.value = profile.output.ifEmpty { "<empty>" }
            output.tooltip = Tooltip.create(Component.literal("Output"))
            output.setEditable(false)
            output.setTextColor(CommonColors.GRAY)
            addRenderableWidget(output)
        }

        // Layout
        val layout = LinearLayout.vertical().spacing(1)
        layout.defaultCellSetting().alignHorizontallyLeft()
        buildProfile(layout)
        layout.arrangeElements()
        val layoutX = textureX + (size / 2) - 10
        val layoutY = textureY + (size * 0.2f).toInt()
        layout.setPosition(layoutX - (layout.width / 2), layoutY)
        layout.visitWidgets(this::addRenderableWidget)
    }

    override fun saveCustom(data: ShowBridgeEditPacket) {
        data.serverProfile = menu.data.serverProfile
        data.playerProfile = menu.data.playerProfile
    }
}