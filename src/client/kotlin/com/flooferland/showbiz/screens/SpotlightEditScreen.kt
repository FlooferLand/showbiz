package com.flooferland.showbiz.screens

import net.minecraft.*
import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.screens.*
import net.minecraft.client.gui.screens.inventory.*
import net.minecraft.network.chat.*
import net.minecraft.util.*
import net.minecraft.world.entity.player.*
import com.flooferland.showbiz.menus.SpotlightEditMenu
import com.flooferland.showbiz.registry.ModCommands
import com.flooferland.showbiz.show.toBitIdOrNull
import com.flooferland.showbiz.utils.rl
import com.mojang.blaze3d.systems.RenderSystem
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import kotlin.math.roundToInt

// TODO: Merge ShowParserScreen and SpotlightEditScreen
class SpotlightEditScreen(val editMenu: SpotlightEditMenu, inventory: Inventory, title: Component) : Screen(title), MenuAccess<SpotlightEditMenu> {
    data class WidgetInfo(val name: String?, val widget: AbstractWidget)
    val background = rl("textures/gui/spotlight.png")

    val texSize = 256
    val size get() = (texSize * 1.5).roundToInt()
    val textureX get() = (width / 2) - (size / 2)
    val textureY get() = (height / 2) - (size / 2)

    override fun getMenu() = editMenu
    override fun isPauseScreen() = false

    var selectBox: EditBox? = null
    var turnX: EditBox? = null
    var turnY: EditBox? = null

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
                        .append(Component.literal("Spotlight").withStyle(ChatFormatting.DARK_GRAY)
                        ), false)
                player.displayClientMessage(Component.literal("Use ").append(command).append(" to view all the bits you can filter for"), false)
                client.setScreen(null)
            }.pos(textureX + (size * 0.77).toInt(), textureY + (size * 0.82).toInt()).size(20, 20).build()
            addRenderableWidget(help)
        }
    }

    override fun resize(minecraft: Minecraft?, width: Int, height: Int) {
        super.resize(minecraft, width, height)
        selectBox = null
        clearWidgets()
        init()
    }

    fun autoUi() {
        val widgets = mutableListOf<WidgetInfo>()

        // Filter box
        run {
            selectBox = EditBox(font, 200, 20, Component.literal("Filter"))
            selectBox!!.tooltip = Tooltip.create(Component.literal("Enter one or more bit numbers (comma-separated)"))
            selectBox!!.value = menu.data.bitFilter.joinToString(",")
            selectBox!!.setFormatter { text, i ->
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
            widgets.add(WidgetInfo("Bit selector", selectBox!!))
        }

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

        // Placing the UI
        for ((i, entry) in widgets.withIndex()) {
            val (name, widget) = entry
            val spacing = 40
            val x = textureX + (size / 2) - (widget.width / 2)
            val y = textureY + (size * 0.5f).toInt() + (i * spacing)

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
        selectBox?.let { filterBox ->
            filterBox.value.split(',').forEach { entry ->
                entry.toBitIdOrNull()?.let {
                    menu.data.bitFilter.add(it)
                }
            }
        }
        turnX?.value?.toFloatOrNull()?.let { menu.data.turn.x = it }
        turnY?.value?.toFloatOrNull()?.let { menu.data.turn.y = it }
        ClientPlayNetworking.send(menu.data)
    }

    override fun renderBackground(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick)

        RenderSystem.enableBlend()
        guiGraphics.setColor(0.5f, 0.5f, 0.5f, 0.8f)
        guiGraphics.blit(background, textureX, textureY, 0f, 0f, size, size, size, size)
        guiGraphics.setColor(1f, 1f, 1f, 1f)
        guiGraphics.drawCenteredString(font, title, textureX + (size / 2), textureY + (size * 0.15).toInt(), CommonColors.WHITE)
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick)
        super.render(guiGraphics, mouseX, mouseY, partialTick)
    }
}