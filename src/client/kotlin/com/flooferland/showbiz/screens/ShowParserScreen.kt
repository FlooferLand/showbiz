package com.flooferland.showbiz.screens

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.MenuAccess
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import com.flooferland.showbiz.menus.ShowParserMenu
import com.flooferland.showbiz.registry.ModCommands
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

class ShowParserScreen(val parserMenu: ShowParserMenu, inventory: Inventory, title: Component) : Screen(title), MenuAccess<ShowParserMenu> {
    override fun getMenu() = parserMenu
    override fun isPauseScreen() = false

    var filterBox: EditBox? = null

    override fun init() {
        super.init()

        val widgets = mutableListOf<Pair<String?, AbstractWidget>>()

        // Filter
        run {
            filterBox = EditBox(font, 200, 20, Component.literal("Filter"))
            filterBox!!.value = menu.data.bitFilter.joinToString(",")
            widgets.add(Pair("Bit filter", filterBox!!))
        }

        // Bitmap help button
        run {
            val help = Button.builder(Component.literal("How to use")) {
                val client = Minecraft.getInstance() ?: return@builder
                val player = client.player ?: return@builder

                val mapping = menu.data.mapping ?: ""
                val command = when {
                    mapping.isNotBlank() -> ModCommands.bitmapCommandView(map = mapping)
                    else -> ModCommands.bitmapCommandView()
                }.let { Component.literal(it).withStyle(ChatFormatting.GRAY) }
                player.displayClientMessage(
                    Component.literal("-- ").withStyle(ChatFormatting.DARK_RED)
                        .append(Component.literal("Show parser").withStyle(ChatFormatting.DARK_GRAY)
                ), false)
                player.displayClientMessage(Component.literal("Use ").append(command).append(" to view all the bits you can filter for"), false)
                client.setScreen(null)
            }.size(100, 20).build()
            widgets.add(Pair(null, help))
        }

        // Placing the UI
        for ((i, entry) in widgets.withIndex()) {
            val (name, widget) = entry
            val x = (width * 0.2f).toInt()
            val y = (height * 0.2f).toInt() + (i * 50)

            val nameHeight = name?.let { (font.lineHeight * 1.5f).toInt() } ?: 0
            if (name != null) {
                val nameWidget = StringWidget(x, y, width - x, 20, Component.literal(name), font).alignLeft()
                addRenderableWidget(nameWidget)
            }

            widget.x = x
            widget.y = y + nameHeight
            addRenderableWidget(widget)
        }
    }

    override fun onClose() {
        super.onClose()

        menu.data.bitFilter.clear()
        filterBox?.let { filterBox ->
            filterBox.value.split(',').forEach { entry ->
                entry.toShortOrNull()?.let {
                    menu.data.bitFilter.add(it)
                }
            }
        }
        ClientPlayNetworking.send(menu.data)
    }

    override fun render(guiGraphics: GuiGraphics?, mouseX: Int, mouseY: Int, partialTick: Float) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick)
        super.render(guiGraphics, mouseX, mouseY, partialTick)
    }
}