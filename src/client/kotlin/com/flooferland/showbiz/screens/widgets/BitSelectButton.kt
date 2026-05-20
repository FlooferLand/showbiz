package com.flooferland.showbiz.screens.widgets

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.narration.NarratedElementType
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component
import net.minecraft.util.FastColor
import com.flooferland.bizlib.bits.BitUtils
import com.flooferland.bizlib.bits.MappedBit
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.show.Drawer
import com.flooferland.showbiz.types.MappedBits
import kotlin.math.min

class BitSelectButton(x: Int, y: Int, width: Int, height: Int) : AbstractWidget(x, y, width, height, Component.literal("Bit select")) {
    val popupY get() = y + height
    val popupHeight get() = min(200, (Minecraft.getInstance().screen!!.height - popupY) - 5)
    val popupWidth get() = width

    var pickingChart: String? = null
    var expanded = false
    var values = MappedBits()

    var scrollOffset = 0.0
    var bitWidgets = mutableListOf<AbstractWidget>()
    val listStartY get() = popupY + Minecraft.getInstance().font.lineHeight
    val listHeight get() = popupHeight - (listStartY - popupY)
    val maxScroll get() = maxOf(0.0, (bitWidgets.size * 20).toDouble() - listHeight)

    val buttons: Map<String, Button> = Showbiz.charts.ids.associateWith { chartId ->
        Button.builder(Component.empty()) {
           popupSetOpen(!expanded || pickingChart != chartId, chartId)
        }.size(0, height).tooltip(Tooltip.create(Component.literal("$chartId bit"))).build()
    }

    init {
        updatePositions()
    }

    fun popupSetOpen(open: Boolean, chartId: String? = null) {
        if (activeButton != this) activeButton?.popupSetOpen(false)
        if (open) {
            activeButton = this;

            val widgets = mutableListOf<AbstractWidget>()
            val chart = chartId?.let { BitUtils.readBitmap(it) } ?: return
            chart.forEach { (fixtureName, movements) ->
                val font = Minecraft.getInstance().font
                widgets.add(StringWidget(x + 5, 0, popupWidth - 10, 20, Component.literal(fixtureName), font))
                movements.forEach { (movementName, bitId) ->
                    val tooltip = Tooltip.create(Component.literal(movementName))
                    val text = "$bitId: $movementName"
                    val button = Button.builder(Component.literal(text)) {
                        values[chartId] = bitId
                        popupSetOpen(false)
                    }.pos(x + 5, 0).size(popupWidth - 10, 20).tooltip(tooltip).build()
                    button.message = Component.literal(
                        if (font.width(text) > button.width) {
                            font.plainSubstrByWidth(text, button.width - font.width("..")) + ".."
                        } else {
                            text
                        }
                    )
                    widgets.add(button)
                }
            }
            bitWidgets = widgets
            updateScroll(0.0)
        } else if (activeButton == this) {
            activeButton = null;
            bitWidgets = mutableListOf()
        }

        expanded = open
        pickingChart = chartId
        buttons.forEach { (id, b) -> b.active = !open || id != chartId }
    }

    override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val font = Minecraft.getInstance()?.font ?: return

        if (expanded) {
            val outline = 1
            guiGraphics.fill(x - outline, y - outline, x + width + outline, y + height + outline, 0xAAFFFFFF.toInt())
        }

        // Chart buttons
        buttons.forEach { (chartId, button) ->
            button.render(guiGraphics, mouseX, mouseY, partialTick)
            val outlineColor = Showbiz.charts.idsToInfo[pickingChart?: chartId]?.color ?: 0xFFFFFFFF.toInt()
            guiGraphics.renderOutline(button.x, button.y, button.width, button.height, FastColor.ARGB32.color(100, outlineColor))

            // Bit chart ID
            val color = Showbiz.charts.idsToInfo[chartId]?.color ?: 0xFFFFFFFF.toInt()
            guiGraphics.drawString(font, chartId.first().toString(), button.x, button.y - 3, color)

            // Bit ID
            val text = values[chartId]?.let { Drawer.formatBitAsComp(it) } ?: Component.literal("None")
            guiGraphics.drawCenteredString(font, text, button.x + (button.width / 2), button.y + (button.height / 2) - (font.lineHeight / 2), 0xFFFFFFFF.toInt())
        }

        // Popup
        if (expanded) {
            guiGraphics.pose().pushPose()
            guiGraphics.pose().translate(0f, 0f, 5f)
            guiGraphics.fill(x, popupY, x + popupWidth, popupY + popupHeight, 0x99000000.toInt())
            guiGraphics.drawString(font, "Pick a bit ($pickingChart)", x, popupY, 0xFFFFFFFF.toInt())

            // Content
            guiGraphics.enableScissor(x, listStartY, x + popupWidth, listStartY + listHeight)
            bitWidgets.forEach { it.render(guiGraphics, mouseX, mouseY, partialTick) }
            guiGraphics.disableScissor()
            guiGraphics.pose().popPose()
        }
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (isMouseOverPopup(mouseX, mouseY)) {
            updateScroll(scrollOffset - (scrollY * 20))
            return true
        }
        return false
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!active || !visible) return false
        if (isMouseOverPopup(mouseX, mouseY)) {
            if (mouseY >= listStartY) {
                bitWidgets.forEach { it.mouseClicked(mouseX, mouseY, button) }
            }
            return true
        } else if (isMouseOver(mouseX, mouseY)) {
            // Player probably clicked on a button
        } else if (expanded) {
            // Player clicked off
            popupSetOpen(false)
            return true
        }

        return buttons.values.any { it.mouseClicked(mouseX, mouseY, button) }
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return super.isMouseOver(mouseX, mouseY) || isMouseOverPopup(mouseX, mouseY)
    }

    fun isMouseOverPopup(mouseX: Double, mouseY: Double) =
        expanded && mouseX > x && mouseX < (x + popupWidth)
                && mouseY > popupY && mouseY < (popupY + popupHeight)

    fun updateScroll(scroll: Double) {
        scrollOffset = scroll.coerceIn(0.0, maxScroll)
        bitWidgets.forEachIndexed { i, widget ->
            widget.y = listStartY + (i * 20) - scrollOffset.toInt()
        }
    }

    fun updatePositions() {
        val chartSize = buttons.size
        if (chartSize == 0) return

        val buttonWidth = width / chartSize
        buttons.values.forEachIndexed { i, button ->
            button.x = x + (i * buttonWidth)
            button.y = y
            button.width = buttonWidth
        }
    }

    override fun setPosition(x: Int, y: Int) {
        super.setPosition(x, y)
        updatePositions()
    }

    override fun updateWidgetNarration(narration: NarrationElementOutput) {
        narration.add(NarratedElementType.HINT, "Bit selection button")
    }

    companion object {
        var activeButton: BitSelectButton? = null
    }
}