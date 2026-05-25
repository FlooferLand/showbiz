package com.flooferland.showbiz.screens.widgets

import net.minecraft.*
import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.narration.*
import net.minecraft.client.gui.screens.*
import net.minecraft.network.chat.*
import net.minecraft.resources.*
import net.minecraft.util.*
import com.flooferland.bizlib.bits.BitUtils
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.show.BitId
import com.flooferland.showbiz.show.toBitId
import com.flooferland.showbiz.types.MappedBits
import com.mojang.blaze3d.systems.RenderSystem
import kotlin.math.max

// Welcome to hell
class BitSelectButton(x: Int, y: Int, width: Int, height: Int = 15) : AbstractWidget(x, y, width, height, Component.literal("Bit select")) {
    val popupY get() = y + height
    val popupHeight get() = (Minecraft.getInstance().screen!!.height - popupY) - 5
    val popupWidth get() = max(180, width+1)
    val popupEntryHeight get() = 13

    var pickingChart: String? = null
    var usedBits = mutableSetOf<BitId>()
    var expanded = false
    var values = MappedBits()
        set(value) {
            field = value
            updateChartButtons()
        }

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
        bitWidgets.clear()
        if (open) {
            val font = Minecraft.getInstance().font
            activeButton = this

            searchBar = EditBox(font, popupWidth, popupEntryHeight, Component.literal("Search box"))
            searchBar?.setPosition(x, popupY)
            searchBar?.tooltip = Tooltip.create(Component.literal("Click to search"))
            searchBar?.isFocused = true
            searchBar?.setResponder { filterList(it) }

            chartId?.let { rebuildList(it) }
        } else if (activeButton == this) {
            activeButton = null;
            searchBar = null
            usedBits.clear()
            bitWidgets = mutableListOf()
        }

        expanded = open
        pickingChart = chartId
        buttons.forEach { (id, b) -> b.active = !open || id != chartId }
        updateChartButtons()
        updateScroll(0.0)
    }

    private fun rebuildList(chartId: String) {
        val font = Minecraft.getInstance().font
        val widgets = mutableListOf<AbstractWidget>()
        val chart = chartId.let { BitUtils.readBitmap(it) }
        fun addBitButton(fixtureName: String, movementName: String, bitId: BitId, selectionButton: Boolean, pressed: () -> Unit): BitEntryButton {
            val button = BitEntryButton(
                x + 5, 0, popupWidth - 10, popupEntryHeight,
                fixtureName, movementName, bitId,
                selectionButton, pressed
            )
            widgets.add(button)
            return button
        }

        // Selected bits
        val selection = values.getOrPutDefault(chartId)
        if (selection.isNotEmpty()) {
            widgets.add(StringWidget(x + 5, 0, popupWidth - 10, popupEntryHeight, Component.literal("selected"), font))
            selection.forEach { bitId ->
                val fixtureName = chart?.entries?.find { (_, movements) -> movements.containsValue(bitId) }?.key ?: "Unknown fixture"
                val info = chart?.values?.flatMap { it.entries }?.find { it.value == bitId }
                val movementName = info?.key ?: "Bit $bitId"
                addBitButton(fixtureName, movementName, bitId, true) {
                    values.removeBit(chartId, bitId)
                    rebuildList(chartId)
                }
            }
        }

        // Main named bits
        val bitButtonClick = fun(bitId: BitId) {
            if (Screen.hasShiftDown()) {
                if (bitId in selection) values.removeBit(chartId, bitId)
                else values.addBit(chartId, bitId)
                rebuildList(chartId)
            } else {
                values.clearBits(chartId)
                values.addBit(chartId, bitId)
                popupSetOpen(false)
            }
        }
        chart?.forEach { (fixtureName, movements) ->
            val movements = movements.entries
                .filter { it.value !in selection }
                .sortedBy { it.value }
            widgets.add(StringWidget(x + 5, 0, popupWidth - 10, popupEntryHeight, Component.literal(fixtureName), font))
            movements.forEach { (movementName, bitId) ->
                usedBits.add(bitId)
                addBitButton(fixtureName, movementName, bitId, false) { bitButtonClick(bitId) }
            }
        }

        // Unused bits
        widgets.add(StringWidget(x + 5, 0, popupWidth - 10, popupEntryHeight, Component.literal("Unused"), font))
        val unusedBits = (1..(BitUtils.NEXT_DRAWER * 2u).toInt()).filter { it.toBitId() !in usedBits }
        unusedBits.forEach { id ->
            val bitId = id.toBitId()
            addBitButton(fixtureName = "unused", movementName = "Bit $bitId", bitId = bitId, false, { bitButtonClick(bitId) },)
        }
        bitWidgets = widgets
        searchBar?.let { filterList(it.value) }
        updateChartButtons()
    }

    private fun filterList(text: String) {
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
        updateScroll()
    }

    private fun updateChartButtons() {
        buttons.forEach { (chartId, button) ->
            val bits = values.getOrPutDefault(chartId)
            val text = if (bits.isNotEmpty())
                Component.literal(bits.joinToString(",")).withStyle(ChatFormatting.GREEN)
            else
                Component.literal("..").withStyle(ChatFormatting.GRAY)
            button.message = text
        }
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

            // Tooltip
            if ((!expanded && activeButton?.expanded != true) && button.isHovered) {
                val info = run {
                    val bitChart = BitUtils.readBitmap(chartId)
                    val bits = values.getOrPutDefault(chartId)
                    val list = mutableListOf<String>()
                    for ((fixture, movements) in bitChart?.entries ?: emptyList()) {
                        for ((name, moveBit) in movements)
                            if (moveBit in bits) list += "$fixture.$name"
                    }
                    list
                }
                val comps = mutableListOf<MutableComponent>(Component.literal("$chartId bit"))
                info.forEach { comps += Component.literal(it).withStyle(ChatFormatting.GRAY) }
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
                val bit = pickingChart?.let { values.getOrPutDefault(it) }
                val text = if (bit == null) "Pick a bit" else "Replacing bit $bit"
                searchBar.setHint(Component.literal("$text ($pickingChart)").withStyle(ChatFormatting.DARK_GRAY))
                searchBar.render(guiGraphics, mouseX, mouseY, partialTick)
            }

            // Content
            guiGraphics.enableScissor(x, listStartY, x + popupWidth, listStartY + listHeight)
            val minRenderY = listStartY - popupEntryHeight
            val maxRenderY = listStartY + listHeight
            bitWidgets.forEach { widget ->
                if (!widget.visible) return@forEach
                if (widget.y in minRenderY..maxRenderY) {
                    widget.render(guiGraphics, mouseX, mouseY, partialTick)
                }
            }
            if (bitWidgets.isEmpty()) {
                guiGraphics.drawString(font, "No chart yet. Type bit IDs in", x + 2, popupY + (searchBar?.height ?: 0) + 2, 0xFFFFFFFF.toInt())
            }
            guiGraphics.disableScissor()
            guiGraphics.renderOutline(x - 1, popupY - 1, popupWidth + 1, popupHeight + 1, 0xFFFFFFFF.toInt())
            guiGraphics.pose().popPose()
        }
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val searchBar = searchBar
        val chartId = pickingChart
        if (expanded && searchBar?.isFocused == true) {
            /*if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_COMMA) {
                val value = searchBar.value.trim().toBitIdOrNull()
                if (value != null && chartId != null) {
                    values.addBit(chartId, value)
                    rebuildList(chartId)
                    searchBar.value = ""
                    return true
                }
            }*/
            return searchBar.keyPressed(keyCode, scanCode, modifiers)
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
                bitWidgets.toList().forEach { if (it.visible) it.mouseClicked(mouseX, mouseY, button) }
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

    fun updateScroll(scroll: Double = scrollOffset) {
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

    override fun setX(x: Int) {
        super.setX(x)
        updatePositions()
    }

    override fun setY(y: Int) {
        super.setY(y)
        updatePositions()
    }

    override fun updateWidgetNarration(narration: NarrationElementOutput) {
        narration.add(NarratedElementType.HINT, "Bit selection button")
    }

    inner class BitEntryButton(x: Int, y: Int, width: Int, height: Int, fixtureName: String, movementName: String, bitId: BitId, val selectionButton: Boolean = false, val pressed: () -> Unit) : AbstractButton(x, y, width, height, message) {
        val searchKey: String = "$bitId $movementName $fixtureName"
            .lowercase()
            .replace('_', ' ')
            .replace("duke", "dook")

        init {
            message = Component.literal(bitId.toString()).withStyle(ChatFormatting.GREEN)
                .append(Component.literal(" "))
                .append(Component.literal(movementName).withStyle(ChatFormatting.WHITE))

            val tooltipComp = Component.empty()
                .append(Component.literal(fixtureName).withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("\n"))
                .append(Component.literal(movementName).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("\n"))

            tooltip = (
                if (!selectionButton) {
                    Component.literal("Hold shift to multi-select").withStyle(ChatFormatting.GRAY)
                } else {
                    Component.literal("Click to deselect").withStyle(ChatFormatting.GRAY)
                }
            ).let { Tooltip.create(tooltipComp.append(it)) }
        }

        val sprites: WidgetSprites = WidgetSprites(
            ResourceLocation.withDefaultNamespace("widget/button"),
            ResourceLocation.withDefaultNamespace("widget/button_disabled"),
            ResourceLocation.withDefaultNamespace("widget/button_highlighted")
        )

        override fun onPress() {
            pressed()
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
            if (selectionButton) {
                guiGraphics.drawString(font, "x", x + (width - font.width("x")) - 5, y + (height - font.lineHeight) / 2, 0xffff2222.toInt())
            }
        }

        override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {
            narrationElementOutput.add(NarratedElementType.HINT, message)
        }
    }

    companion object {
        var activeButton: BitSelectButton? = null
    }
}