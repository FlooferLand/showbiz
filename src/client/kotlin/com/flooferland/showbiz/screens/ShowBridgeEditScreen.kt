package com.flooferland.showbiz.screens

import net.minecraft.*
import net.minecraft.client.*
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.layouts.*
import net.minecraft.client.gui.screens.*
import net.minecraft.network.chat.*
import net.minecraft.util.*
import net.minecraft.world.entity.player.*
import com.flooferland.showbiz.Permissions
import com.flooferland.showbiz.menus.ShowBridgeEditMenu
import com.flooferland.showbiz.network.packets.editscreen.ShowBridgeEditPacket
import com.flooferland.showbiz.screens.base.EditScreen
import com.flooferland.showbiz.screens.widgets.BitSelectButton
import com.flooferland.showbiz.screens.widgets.TextWidget
import com.flooferland.showbiz.types.showbridge.SafeSerialPort
import com.flooferland.showbiz.types.showbridge.ShowBridgeMethod
import com.flooferland.showbiz.types.showbridge.ShowBridgeProfile
import com.flooferland.showbiz.utils.ShowbizEnv
import com.flooferland.showbiz.utils.rl

class ShowBridgeEditScreen(editMenu: ShowBridgeEditMenu, inventory: Inventory, title: Component) : EditScreen<ShowBridgeEditMenu, ShowBridgeEditPacket>(editMenu, inventory, title) {
    override val background = rl("textures/gui/show_bridge.png")

    var selectedTab = Tab.entries.firstOrNull { getProfile(it).enabled } ?: Tab.Player
    val profile get() = getProfile(selectedTab)

    enum class Tab { Player, Server }

    fun getProfile(tab: Tab): ShowBridgeProfile {
        return if (tab == Tab.Player) menu.data.playerProfile else menu.data.serverProfile
    }
    fun getFreePorts(): List<String> {
        return if (selectedTab == Tab.Server) editMenu.data.serverFreePorts else SafeSerialPort.getUnusedPorts()
    }
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
                port.value = profile.serialPort
                port.tooltip = Tooltip.create(Component.literal("Case sensitive"))
                port.setResponder { text ->
                    profile.serialPort = text
                    val suggestion = getFreePorts().firstOrNull { it.startsWith(text, ignoreCase = true) } ?: ""
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
            ShowBridgeMethod.TextList -> {}
            ShowBridgeMethod.BinaryList -> {
                layout.addChild(TextWidget(font, Component.literal("This method is a huge work in progress, expect breaking changes!"), contentWidth))
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

            // Checkboxes
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
                Tab.Player -> SafeSerialPort.getAllPorts()
                Tab.Server -> menu.data.serverAllPorts
            }

            val lines = mutableListOf<Component>()
            lines += Component.literal("$selectedTab serial ports")
            ports.forEach { portName ->
                val free = getFreePorts().contains(portName)
                lines += Component.literal("- ").append(Component.literal(portName).withStyle(if (free) ChatFormatting.GREEN else ChatFormatting.RED))
            }

            val lineHeight = font.lineHeight + 3
            val x = 20
            val y = y + (lines.size * lineHeight / 2)
            lines.forEachIndexed { i, text ->
                val name = StringWidget(x, y + (i * lineHeight), font.width(text), lineHeight, text, font).alignLeft()
                addRenderableWidget(name)
            }
        }

        // Stay open checkbox
        val stayOpenCheckbox = Checkbox.builder(Component.literal("Stay open"), font)
            .pos(x + profileX, y)
            .selected(profile.stayOpen)
            .tooltip(Tooltip.create(Component.literal("Always keeps the profile engaged even if no show is playing")))
            .onValueChange { _, v ->
                profile.stayOpen = v
                rebuildWidgets()
            }
            .build()
        addRenderableWidget(stayOpenCheckbox)

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

        // Help button
        run {
            val x = x + (texSize - 20)
            val y = height - 30
            val url = "https://github.com/FlooferLand/showbiz/wiki/Controlling-real-bots"
            val client = Minecraft.getInstance()
            val help = Button.builder(Component.literal("?")) {
                    client.setScreen(ConfirmLinkScreen({ confirmed ->
                        if (confirmed) ShowbizEnv.openLink(url)
                        client.setScreen(this)
                    }, url, true))
                }
                .pos(x, y)
                .size(20, 20)
                .tooltip(Tooltip.create(Component.literal("Help")))
                .build()
            addRenderableWidget(help)
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