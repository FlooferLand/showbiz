package com.flooferland.showbiz.screens.widgets

import net.minecraft.*
import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.narration.*
import net.minecraft.network.chat.*
import net.minecraft.resources.*
import net.minecraft.util.*
import com.flooferland.bizlib.bits.BitUtils
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.show.BitId
import com.flooferland.showbiz.show.Drawer
import com.flooferland.showbiz.show.toBitId
import com.flooferland.showbiz.types.MappedBits
import com.mojang.blaze3d.systems.RenderSystem

// Welcome to hell
class BitSelectButton(x: Int, y: Int, width: Int, height: Int) : AbstractWidget(x, y, width, height, Component.literal("Bit select")) {
    val popupY get() = y + height
    val popupHeight get() = (Minecraft.getInstance().screen!!.height - popupY) - 5
    val popupWidth get() = 180
    val popupEntryHeight get() = 13

    var pickingChart: String? = null
    var expanded = false
    var values = MappedBits()
    var usedBits = mutableSetOf<BitId>()

    var scrollOffset = 0.0
    var bitWidgets = mutableListOf<AbstractWidget>()
    val listStartY get() = popupY + (Minecraft.getInstance().font.lineHeight * 1.5).toInt()
    val listHeight get() = popupHeight - (listStartY - popupY)

    var searchBar: EditBox? = null
    val buttons: Map<String, Button> = Showbiz.charts.ids.associateWith { chartId ->
        Button.builder(Component.empty()) {
           popupSetOpen(!expanded || pickingChart != chartId, chartId)
        }.size(0, height).build()
    }

    init {
        updatePositions()
    }

    fun popupSetOpen(open: Boolean, chartId: String? = null) {
        if (activeButton != this) activeButton?.popupSetOpen(false)
        usedBits.clear()
        if (open) {
            val font = Minecraft.getInstance().font
            activeButton = this

            searchBar = EditBox(font, popupWidth, popupEntryHeight, Component.literal("Search box"))
            searchBar?.setPosition(x, popupY)
            searchBar?.tooltip = Tooltip.create(Component.literal("Click to search"))
            searchBar?.isFocused = true
            searchBar?.setResponder { text ->
                val query = text.lowercase().trim()
                if (query.isEmpty()) {
                    bitWidgets.forEach { it.visible = true }
                } else {
                    var currentHeader: StringWidget? = null
                    var matchesHeader = false
                    bitWidgets.forEach { widget ->
                        if (widget is StringWidget) {
                            currentHeader = widget
                            val target = widget.message.string.lowercase()
                                .replace('_', ' ')
                                .replace("duke", "dook")
                            matchesHeader = query in target
                            widget.visible = matchesHeader
                        } else if (widget is BitEntryButton) {
                            val buttonMatched = query in widget.searchKey
                            widget.visible = matchesHeader || buttonMatched
                            if (buttonMatched && currentHeader != null) {
                                currentHeader.visible = true
                            }
                        }
                    }
                }
                updateScroll(0.0)
            }

            val widgets = mutableListOf<AbstractWidget>()
            val chart = chartId?.let { BitUtils.readBitmap(it) } ?: return

            // Named bits
            chart.forEach { (fixtureName, movements) ->
                widgets.add(StringWidget(x + 5, 0, popupWidth - 10, popupEntryHeight, Component.literal(fixtureName), font))
                val movements = movements.entries.sortedBy { it.value }
                movements.forEach { (movementName, bitId) ->
                    usedBits.add(bitId)
                    val button = BitEntryButton(
                        x + 5, 0, popupWidth - 10, popupEntryHeight,
                        fixtureName = fixtureName,
                        movementName = movementName,
                        bitId = bitId
                    ) {
                        values[chartId] = bitId
                        popupSetOpen(false)
                    }
                    widgets.add(button)
                }
            }

            // Unused
            widgets.add(StringWidget(x + 5, 0, popupWidth - 10, popupEntryHeight, Component.literal("Unused"), font))
            val unusedBits = (1..(BitUtils.NEXT_DRAWER * 2u).toInt()).filter { it.toBitId() !in usedBits }
            unusedBits.forEach { id ->
                val bitId = id.toBitId()
                val button = BitEntryButton(
                    x + 5, 0, popupWidth - 10, popupEntryHeight,
                    fixtureName = "unused",
                    movementName = "Bit $bitId",
                    bitId = bitId
                ) {
                    values[chartId] = bitId
                    popupSetOpen(false)
                }
                widgets.add(button)
            }
            bitWidgets = widgets
            updateScroll(0.0)
        } else if (activeButton == this) {
            activeButton = null;
            searchBar = null
            usedBits.clear()
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
            val text = values[chartId]?.let { Drawer.formatBitAsComp(it).withStyle(ChatFormatting.GREEN) } ?: Component.literal("..").withStyle(ChatFormatting.GRAY)
            guiGraphics.drawCenteredString(font, text, button.x + (button.width / 2), button.y + (button.height / 2) - (font.lineHeight / 2), 0xFFFFFFFF.toInt())

            // Tooltip
            if ((!expanded && activeButton?.expanded != true) && button.isHovered) {
                val info: String = run {
                    val bitChart = BitUtils.readBitmap(chartId)
                    val bit = values[chartId] ?: return@run null
                    val bitName = bitChart?.let {
                        for ((fixture, movements) in bitChart.entries) {
                            for ((name, moveBit) in movements)
                                if (moveBit == bit) return@let "$fixture.$name"
                        }
                        return@let null
                    } ?: "Unused"
                    return@run bitName
                } ?: "Unset"
                val comps = listOf<MutableComponent>(
                    Component.literal("$chartId bit"),
                    Component.literal(info).withStyle(ChatFormatting.GRAY)
                )
                guiGraphics.renderTooltip(font, comps.map { it.visualOrderText }, mouseX, mouseY)
            }
        }

        // Popup
        if (expanded) {
            guiGraphics.pose().pushPose()
            guiGraphics.pose().translate(0f, 0f, 5f)
            guiGraphics.fill(x, popupY, x + popupWidth, popupY + popupHeight, 0x99000000.toInt())

            // Search
            searchBar?.let { searchBar ->
                val bit = pickingChart?.let { values[it] }
                val text = if (bit == null) "Pick a bit" else "Replacing bit $bit"
                searchBar.setHint(Component.literal("$text ($pickingChart)").withStyle(ChatFormatting.DARK_GRAY))
                searchBar.render(guiGraphics, mouseX, mouseY, partialTick)
            }

            // Content
            guiGraphics.enableScissor(x, listStartY, x + popupWidth, listStartY + listHeight)
            bitWidgets.forEach { it.render(guiGraphics, mouseX, mouseY, partialTick) }
            guiGraphics.disableScissor()
            guiGraphics.renderOutline(x - 1, popupY - 1, popupWidth + 1, popupHeight + 1, 0xFFFFFFFF.toInt())
            guiGraphics.pose().popPose()
        }
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (expanded && searchBar?.isFocused == true) {
            return searchBar!!.keyPressed(keyCode, scanCode, modifiers)
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun charTyped(codePoint: Char, modifiers: Int): Boolean {
        if (expanded && searchBar?.isFocused == true) {
            return searchBar!!.charTyped(codePoint, modifiers)
        }
        return super.charTyped(codePoint, modifiers)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (isMouseOverPopup(mouseX, mouseY)) {
            updateScroll(scrollOffset - (scrollY * popupEntryHeight))
            return true
        }
        return false
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!active || !visible) return false
        if (isMouseOverPopup(mouseX, mouseY)) {
            if (mouseY >= listStartY) {
                bitWidgets.forEach { if (it.visible) it.mouseClicked(mouseX, mouseY, button) }
            } else if (mouseY > y) {
                searchBar?.isFocused = true
                searchBar?.mouseClicked(mouseX, mouseY, button)
                searchBar?.onClick(mouseX, mouseY)
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
        // Dynamically calculate maxScroll inline based on currently visible items
        val currentMaxScroll = maxOf(0.0, (bitWidgets.count { it.visible } * popupEntryHeight).toDouble() - listHeight)
        scrollOffset = scroll.coerceIn(0.0, currentMaxScroll)

        var visibleIndex = 0
        bitWidgets.forEach { widget ->
            if (widget.visible) {
                widget.y = listStartY + (visibleIndex * popupEntryHeight) - scrollOffset.toInt()
                visibleIndex++
            }
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

    inner class BitEntryButton(x: Int, y: Int, width: Int, height: Int, fixtureName: String, movementName: String, bitId: BitId, val action: () -> Unit) : AbstractButton(x, y, width, height, message) {
        val searchKey: String = "$bitId $movementName $fixtureName"
            .lowercase()
            .replace('_', ' ')
            .replace("duke", "dook")

        init {
            message = Component.literal(bitId.toString()).withStyle(ChatFormatting.GREEN)
                .append(Component.literal(" "))
                .append(Component.literal(movementName).withStyle(ChatFormatting.WHITE))
            tooltip = Component.empty()
                .append(Component.literal(fixtureName).withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("\n"))
                .append(Component.literal(movementName).withStyle(ChatFormatting.WHITE))
                .let { Tooltip.create(it) }
        }

        val sprites: WidgetSprites = WidgetSprites(
            ResourceLocation.withDefaultNamespace("widget/button"),
            ResourceLocation.withDefaultNamespace("widget/button_disabled"),
            ResourceLocation.withDefaultNamespace("widget/button_highlighted")
        )

        override fun onPress() {
            action()
        }

        override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
            val font = Minecraft.getInstance().font
            guiGraphics.setColor(0.4f, 0.4f, 0.4f, alpha)
            RenderSystem.enableBlend()
            RenderSystem.enableDepthTest()
            guiGraphics.blitSprite(
                sprites.get(active, isHoveredOrFocused()),
                x, y, width, height
            )
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f)
            guiGraphics.drawString(font, message, x + 4, y + 1 + (height - font.lineHeight) / 2, 0xffffffff.toInt())
        }

        override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {
            narrationElementOutput.add(NarratedElementType.HINT, message)
        }
    }

    companion object {
        var activeButton: BitSelectButton? = null
    }
}