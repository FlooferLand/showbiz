package com.flooferland.showbiz.screens

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.MenuAccess
import net.minecraft.network.chat.Component
import net.minecraft.util.CommonColors
import net.minecraft.world.entity.player.Inventory
import com.flooferland.showbiz.blocks.ShowParserBlock
import com.flooferland.showbiz.menus.ShowParserMenu
import com.flooferland.showbiz.registry.ModCommands
import com.flooferland.showbiz.utils.rl
import com.mojang.blaze3d.systems.RenderSystem
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import kotlin.math.roundToInt

class ShowParserScreen(val parserMenu: ShowParserMenu, inventory: Inventory, title: Component) : Screen(title), MenuAccess<ShowParserMenu> {
    enum class WidgetSide { Playing, Signal }
    data class WidgetInfo(val name: String?, val widget: AbstractWidget, val side: WidgetSide = WidgetSide.Signal)
    val showParserBackground = rl("textures/gui/show_parser.png")
    val showParserPorts = rl("textures/gui/show_parser_ports.png")

    val texSize = 256
    val size get() = (texSize * 1.5).roundToInt()
    val textureX get() = (width / 2) - (size / 2)
    val textureY get() = (height / 2) - (size / 2)

    override fun getMenu() = parserMenu
    override fun isPauseScreen() = false

    var filterBox: EditBox? = null

    override fun init() {
        super.init()
        autoUi()

        // Bitmap help button
        run {
            val help = Button.builder(Component.literal("?")) {
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
            }.pos(textureX + (size * 0.77).toInt(), textureY + (size * 0.82).toInt()).size(20, 20).build()
            addRenderableWidget(help)
        }
    }

    override fun resize(minecraft: Minecraft?, width: Int, height: Int) {
        super.resize(minecraft, width, height)
        filterBox = null
        clearWidgets()
        init()
    }

    fun autoUi() {
        val widgets = mutableListOf<WidgetInfo>()

        // Filter box
        run {
            filterBox = EditBox(font, 200, 20, Component.literal("Filter"))
            filterBox!!.tooltip = Tooltip.create(Component.literal("Enter one or more bit numbers that the 'signal' end of the show parser will trigger for (comma-separated)"))
            filterBox!!.value = menu.data.bitFilter.joinToString(",")
            filterBox!!.setFormatter { text, i ->
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
            widgets.add(WidgetInfo("Bit filter", filterBox!!))
        }

        // Placing the UI
        widgets.firstOrNull()?.widget?.isFocused = true
        for ((i, entry) in widgets.withIndex()) {
            val (name, widget) = entry
            val spacing = 40
            val x = textureX + (size / 2) - (widget.width / 2)
            val y = when (entry.side) {
                WidgetSide.Playing -> textureY + (size * 0.2f).toInt() + (i * spacing)
                WidgetSide.Signal -> textureY + (size * 0.6f).toInt() - (i * spacing)
            }

            val nameHeight = name?.let { (font.lineHeight * 1.65f).toInt() } ?: 0
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

    override fun renderBackground(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick)

        val menuOwner = Minecraft.getInstance()?.level?.getBlockState(parserMenu.data.blockPos)
        RenderSystem.enableBlend()
        guiGraphics.setColor(0.5f, 0.5f, 0.5f, 0.8f)
        guiGraphics.blit(showParserBackground, textureX, textureY, 0f, 0f, size, size, size, size)
        fun drawPort(top: Boolean) {
            val active = when (top) {
                true -> menuOwner?.getValue(ShowParserBlock.PLAYING_POWERED)
                false -> menuOwner?.getValue(ShowParserBlock.SIGNAL_POWERED)
            } ?: false
            val lightness = if (active) 0.8f else 0.2f
            guiGraphics.setColor(lightness, lightness, lightness, 0.8f)
            when (top) {
                true ->
                    guiGraphics.blit(showParserPorts, textureX, textureY, 0f, 0f, size, size / 2, size, size)
                false ->
                    guiGraphics.blit(showParserPorts, textureX, textureY + (size / 2), 0f, size / 2f, size, size / 2, size, size)
            }
        }
        drawPort(true)
        drawPort(false)
        guiGraphics.setColor(1f, 1f, 1f, 1f)
        guiGraphics.drawCenteredString(font, "Signal", textureX + (size / 2), textureY + (size * 0.84).toInt(), CommonColors.WHITE)
        guiGraphics.drawCenteredString(font, "Playing", textureX + (size / 2), textureY + (size * 0.15).toInt(), CommonColors.WHITE)
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick)
        super.render(guiGraphics, mouseX, mouseY, partialTick)
    }
}