package com.flooferland.showbiz.screens.base

import net.minecraft.*
import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.screens.*
import net.minecraft.client.gui.screens.inventory.*
import net.minecraft.network.chat.*
import net.minecraft.resources.*
import net.minecraft.world.entity.player.*
import com.flooferland.showbiz.registry.ModCommands
import com.flooferland.showbiz.screens.widgets.BitSelectButton
import com.flooferland.showbiz.types.EditScreenMenu
import com.flooferland.showbiz.types.MappedBits
import com.mojang.blaze3d.systems.RenderSystem
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import kotlin.math.roundToInt

abstract class EditScreen<M, P: EditScreenMenu.EditScreenPacketPayload>(open val editMenu: M, inventory: Inventory, title: Component)
    : Screen(title), MenuAccess<M>
where M: EditScreenMenu<P> {
    enum class WidgetSide { Top, Bottom }
    data class WidgetInfo(val name: String?, val widget: AbstractWidget, var side: WidgetSide = WidgetSide.Top)
    abstract val background: ResourceLocation

    /** Adds custom widgets to the autoui widget list */
    open fun addCustomWidgets(widgets: MutableList<WidgetInfo>) = Unit

    /** Saves data to the menu's close packet. Called right before closing the screen */
    open fun saveCustom(data: P) = Unit

    val texSize = 256
    val size get() = (texSize * 1.5).roundToInt()
    val textureX get() = (width / 2) - (size / 2)
    val textureY get() = (height / 2) - (size / 2)

    override fun getMenu() = editMenu
    override fun isPauseScreen() = false

    var bitSelector: BitSelectButton? = null

    override fun init() {
        super.init()
        autoUi()

        // Bitmap help button
        run {
            val help = Button.builder(Component.literal("?")) {
                val client = Minecraft.getInstance() ?: return@builder
                val player = client.player ?: return@builder

                val mapping = menu.data.base.mapping ?: ""
                val command = when {
                    mapping.isNotBlank() -> ModCommands.bitmapCommandView(map = mapping)
                    else -> ModCommands.bitmapCommandView()
                }.let { Component.literal(it).withStyle(ChatFormatting.GRAY) }
                player.displayClientMessage(
                    Component.literal("-- ").withStyle(ChatFormatting.DARK_RED)
                        .append(title.copy().withStyle(ChatFormatting.DARK_GRAY)
                        ), false)
                player.displayClientMessage(Component.literal("Use ").append(command).append(" to view all the bits you can filter for"), false)
                client.setScreen(null)
            }.pos(textureX + (size * 0.77).toInt(), textureY + (size * 0.82).toInt()).size(20, 20).build()
            addRenderableWidget(help)
        }
    }

    override fun resize(minecraft: Minecraft, width: Int, height: Int) {
        super.resize(minecraft, width, height)
        bitSelector = null
        clearWidgets()
        init()
    }

    open fun addWidgets(widgets: MutableList<WidgetInfo>) {
        // Filter
        run {
            bitSelector = BitSelectButton(0, 0, 200, 20)
            val copy = MappedBits()
            menu.data.base.bitFilter.forEach { (chartId, bits) -> copy.setBits(chartId, bits) }
            bitSelector!!.values = copy
            widgets.add(WidgetInfo("Bit filter", bitSelector!!))
        }
    }

    fun autoUi() {
        val widgets = mutableListOf<WidgetInfo>()
        addWidgets(widgets)
        addCustomWidgets(widgets)

        // Placing the UI
        widgets.firstOrNull()?.widget?.isFocused = true
        for ((i, entry) in widgets.withIndex()) {
            val (name, widget) = entry
            val spacing = 40
            val x = textureX + (size / 2) - (widget.width / 2)
            val y = when (entry.side) {
                WidgetSide.Top -> textureY + (size * 0.2f).toInt() + (i * spacing)
                WidgetSide.Bottom -> textureY + (size * 0.6f).toInt() - (i * spacing)
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

        menu.data.base.bitFilter.clearCharts()
        bitSelector?.values?.forEach { (chartId, bits) ->
            menu.data.base.bitFilter.setBits(chartId, bits)
        }

        saveCustom(menu.data)
        ClientPlayNetworking.send(menu.data)
    }

    override fun renderBackground(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        RenderSystem.enableBlend()
        guiGraphics.setColor(1f, 1f, 1f, 1f)
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick)

        RenderSystem.enableBlend()
        guiGraphics.setColor(0.5f, 0.5f, 0.5f, 0.8f)
        guiGraphics.blit(background, textureX, textureY, 0f, 0f, size, size, size, size)

        RenderSystem.defaultBlendFunc()
        guiGraphics.setColor(1f, 1f, 1f, 1f)
    }
}