package com.flooferland.showbiz.screens.base

import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.layouts.*
import net.minecraft.client.gui.screens.*
import net.minecraft.client.gui.screens.inventory.*
import net.minecraft.network.chat.*
import net.minecraft.resources.*
import net.minecraft.world.entity.player.*
import com.flooferland.showbiz.types.EditScreenMenu
import com.mojang.blaze3d.systems.RenderSystem
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import kotlin.math.roundToInt

abstract class EditScreen<M, P: EditScreenMenu.EditScreenPacketPayload>(open val editMenu: M, inventory: Inventory, title: Component) : Screen(title), MenuAccess<M> where M: EditScreenMenu<P> {
    data class WidgetInfo(val name: String?, val widget: AbstractWidget, val settings: (LayoutSettings) -> LayoutSettings = { it }) {
        constructor(widget: AbstractWidget) : this(widget.message.string, widget)
    }

    abstract val background: ResourceLocation

    /** Adds custom widgets to the autoui widget list */
    open fun addWidgets(widgets: MutableList<WidgetInfo>) = Unit

    /** Saves data to the menu's close packet. Called right before closing the screen */
    abstract fun saveCustom(data: P)

    val texSize = 256
    val contentWidth get() = size - 220
    val size get() = (texSize * 1.5).roundToInt()
    val textureX get() = (width / 2) - (size / 2)
    val textureY get() = (height / 2) - (size / 2)

    override fun getMenu() = editMenu
    override fun isPauseScreen() = false

    override fun init() {
        super.init()
        buildUi()
    }

    override fun resize(minecraft: Minecraft, width: Int, height: Int) {
        super.resize(minecraft, width, height)
        clearWidgets()
        init()
    }

    open fun buildUi() {
        val widgets = mutableListOf<WidgetInfo>()
        addWidgets(widgets)

        val layout = LinearLayout.vertical().spacing(0)
         layout.defaultCellSetting().alignHorizontallyLeft()

        // Placing the UI
        widgets.firstOrNull()?.widget?.isFocused = true
        for (entry in widgets) {
            val (name, widget) = entry
            if (name != null) {
                val nameWidget = StringWidget(contentWidth, font.lineHeight, Component.literal(name), font).alignLeft()
                layout.addChild(nameWidget)
            }
            widget.width = widget.width.coerceAtMost(contentWidth)
            layout.addChild(widget, LayoutSettings.defaults().paddingBottom(10).let { entry.settings.invoke(it) })
        }

        layout.arrangeElements()
        val x = textureX + (size / 2)
        val y = textureY + (size * 0.2f).toInt() + font.lineHeight * 2
        layout.setPosition(x - (layout.width / 2), y)
        layout.visitWidgets(this::addRenderableWidget)
    }

    override fun onClose() {
        super.onClose()

        saveCustom(menu.data)
        ClientPlayNetworking.send(menu.data)
    }

    override fun renderBackground(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        RenderSystem.defaultBlendFunc()
        guiGraphics.setColor(1f, 1f, 1f, 1f)
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick)

        RenderSystem.enableBlend()
        guiGraphics.setColor(0.5f, 0.5f, 0.5f, 0.9f)
        guiGraphics.blit(background, textureX, textureY, 0f, 0f, size, size, size, size)

        RenderSystem.defaultBlendFunc()
        guiGraphics.setColor(1f, 1f, 1f, 1f)
    }
}