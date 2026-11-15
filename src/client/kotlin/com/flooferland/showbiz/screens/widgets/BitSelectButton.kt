package com.flooferland.showbiz.screens.widgets

import com.flooferland.bizlib.bits.BitUtils
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.show.BitId
import com.flooferland.showbiz.show.toBitId
import com.flooferland.showbiz.types.MappedBits
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.narration.NarratedElementType
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import kotlin.math.max

class BitSelectButton(x: Int, y: Int, width: Int, height: Int = 15) :
    AbstractButton(x, y, width, height, Component.empty()) {

    constructor(width: Int, height: Int) : this(0, 0, width, height)

    private val chartIds = (Showbiz.charts.ids + "any").distinct()
    private val rowHeight = 13

    val popupY get() = y + height
    val popupHeight get() = max(0, Minecraft.getInstance().screen!!.height - popupY - 5)
    val popupWidth get() = max(180, width + 1)

    private val contentY get() = popupY + rowHeight * 2 + 4
    private val contentHeight get() = max(0, popupHeight - (contentY - popupY))

    var values = MappedBits()
    var onChanged: ((MappedBits) -> Unit)? = null

    var expanded = false
        private set
    var pickingChart: String? = null
        private set

    private var choosingChart = true
    private var scrollOffset = 0.0
    private var chartOptions = mutableListOf<Pair<String, Button>>()
    private var bitEntries = mutableListOf<AbstractWidget>()
    private var searchBar: EditBox? = null
    private var labelScrollStartMs = 0L

    private val chartPickerButton = Button.builder(Component.literal("Choose a chart")) {
        if (!choosingChart) showChartPicker()
    }.size(0, rowHeight).build()

    fun popupSetOpen(open: Boolean) {
        if (!open) {
            expanded = false
            pickingChart = null
            searchBar = null
            chartOptions.clear()
            bitEntries.clear()
            if (activeButton == this) activeButton = null
            return
        }

        if (activeButton != this) activeButton?.popupSetOpen(false)

        activeButton = this
        expanded = true
        choosingChart = true
        pickingChart = null
        scrollOffset = 0.0
        labelScrollStartMs = System.currentTimeMillis()

        searchBar = EditBox(
            Minecraft.getInstance().font,
            popupWidth - 10,
            rowHeight,
            Component.literal("Search")
        ).apply {
            setPosition(x + 5, popupY + rowHeight + 2)
            tooltip = Tooltip.create(Component.literal("Search"))
            isFocused = true
            setResponder(::filterEntries)
        }

        rebuildChartOptions()
        positionPopupControls()
    }

    private fun showChartPicker() {
        choosingChart = true
        pickingChart = null
        scrollOffset = 0.0
        searchBar?.value = ""
        rebuildChartOptions()
        positionPopupControls()
    }

    private fun openChart(chartId: String) {
        choosingChart = false
        pickingChart = chartId
        scrollOffset = 0.0
        searchBar?.value = ""
        rebuildBitEntries(chartId)
        positionPopupControls()
    }

    private fun rebuildChartOptions() {
        chartOptions = chartIds.map { chartId ->
            val selectedCount = values.getOrPutDefault(chartId).size
            val button = Button.builder(Component.literal("$chartId ($selectedCount)")) {
                openChart(chartId)
            }.size(popupWidth - 10, rowHeight).build()

            chartId to button
        }.toMutableList()

        filterEntries(searchBar?.value.orEmpty())
    }

    private fun rebuildBitEntries(chartId: String) {
        val font = Minecraft.getInstance().font
        val chart = BitUtils.readBitmap(chartId)
        val selection = values.getOrPutDefault(chartId)
        val usedBits = chart?.values?.flatMap { it.values }?.toSet().orEmpty()
        val entries = mutableListOf<AbstractWidget>()

        fun addHeader(text: String) {
            entries.add(StringWidget(x + 5, 0, popupWidth - 10, rowHeight, Component.literal(text), font))
        }

        fun addBitEntry(fixture: String, movement: String, bitId: BitId, selected: Boolean) {
            entries.add(
                BitEntryButton(x + 5, 0, popupWidth - 10, rowHeight, fixture, movement, bitId, selected) {
                    when {
                        selected -> {
                            values.removeBit(chartId, bitId)
                            onChanged?.invoke(values)
                            rebuildBitEntries(chartId)
                        }

                        Screen.hasShiftDown() -> {
                            if (bitId in selection) values.removeBit(chartId, bitId)
                            else values.addBit(chartId, bitId)

                            onChanged?.invoke(values)
                            rebuildBitEntries(chartId)
                        }

                        else -> {
                            values.clearBits(chartId)
                            values.addBit(chartId, bitId)
                            onChanged?.invoke(values)
                            popupSetOpen(false)
                        }
                    }
                }
            )
        }

        if (selection.isNotEmpty()) {
            addHeader("Selected")

            selection.forEach { bitId ->
                val fixture = chart?.entries
                    ?.find { (_, movements) -> movements.containsValue(bitId) }
                    ?.key
                    ?: "Unknown fixture"
                val movement = chart?.values
                    ?.flatMap { it.entries }
                    ?.find { it.value == bitId }
                    ?.key
                    ?: "Bit $bitId"

                addBitEntry(fixture, movement, bitId, selected = true)
            }
        }

        chart?.forEach { (fixture, movements) ->
            addHeader(fixture)

            movements.entries
                .filter { it.value !in selection }
                .sortedBy { it.value }
                .forEach { (movement, bitId) ->
                    addBitEntry(fixture, movement, bitId, selected = false)
                }
        }

        addHeader("Unused")

        (1..BitUtils.DRAWER_MAX.toInt())
            .map { it.toBitId() }
            .filter { it !in usedBits }
            .forEach { bitId ->
                addBitEntry("unused", "Bit $bitId", bitId, selected = false)
            }

        bitEntries = entries
        filterEntries(searchBar?.value.orEmpty())
    }

    private fun filterEntries(text: String) {
        val query = text.lowercase().trim()

        if (choosingChart) {
            chartOptions.forEach { (chartId, button) ->
                button.visible = query.isEmpty() || query in chartId.lowercase()
            }
        } else {
            var currentHeader: StringWidget? = null
            var headerMatches = false

            bitEntries.forEach { widget ->
                when (widget) {
                    is StringWidget -> {
                        currentHeader = widget
                        val header = widget.message.string.lowercase()
                            .replace('_', ' ')
                            .replace("duke", "dook")

                        headerMatches = query.isEmpty() || query in header
                        widget.visible = headerMatches
                    }

                    is BitEntryButton -> {
                        val entryMatches = query.isEmpty() || query in widget.searchKey
                        widget.visible = headerMatches || entryMatches
                        if (entryMatches) currentHeader?.visible = true
                    }

                    else -> widget.visible = query.isEmpty()
                }
            }
        }

        updateListScroll(0.0)
    }

    private fun mappingLabel(chartId: String, bitId: BitId, showName: Boolean = false): Component {
        val movement = BitUtils.readBitmap(chartId)?.entries?.asSequence()
            ?.flatMap { (fixture, movements) -> movements.entries.asSequence().map { fixture to it } }
            ?.firstOrNull { (_, entry) -> entry.value == bitId }

        val label = Component.literal("$chartId:$bitId")
        if (showName && movement != null) {
            label.append(Component.literal(" (${movement.first}.${movement.second.key})").withStyle(ChatFormatting.GRAY))
        }
        return label
    }

    private fun selectedMappingLabels(showName: Boolean = false): List<Component> {
        val all = (chartIds + values.entries.map { it.key }).distinct()
        return all.flatMap { chartId ->
            values.getOrPutDefault(chartId).map { bitId -> mappingLabel(chartId, bitId, showName) }
        }
    }

    private fun renderHoveredItemTooltip(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val font = Minecraft.getInstance().font

        if (choosingChart) {
            val hoveredOption = chartOptions.firstOrNull { (_, button) ->
                button.visible && button.isMouseOver(mouseX.toDouble(), mouseY.toDouble())
            } ?: return

            val chartId = hoveredOption.first
            val selected = values.getOrPutDefault(chartId)
            val tooltipLines = listOf(Component.literal("$chartId mappings")) + selected.map { bitId -> mappingLabel(chartId, bitId, showName = true) }
            guiGraphics.renderTooltip(font, tooltipLines.map { it.visualOrderText }, mouseX, mouseY)
            return
        }

        val hoveredEntry = bitEntries.filterIsInstance<BitEntryButton>().firstOrNull {
            it.visible && it.isMouseOver(mouseX.toDouble(), mouseY.toDouble())
        } ?: return

        guiGraphics.renderTooltip(font, hoveredEntry.tooltipLines.map { it.visualOrderText }, mouseX, mouseY)
    }

    override fun onPress() {
        popupSetOpen(!expanded)
    }

    override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick)

        val labels = selectedMappingLabels()
        val text = if (labels.isEmpty()) "No mappings" else labels.joinToString(", ") { it.string }
        val font = Minecraft.getInstance().font
        val textX = x + 5
        val textY = y + (height - font.lineHeight) / 2
        val availableWidth = width - 10
        val textWidth = font.width(text)

        guiGraphics.enableScissor(textX, y, x + width - 5, y + height)

        if (textWidth <= availableWidth) {
            guiGraphics.drawString(font, text, textX, textY, 0xFFFFFFFF.toInt())
        } else if (availableWidth > 0) {
            val cycleWidth = textWidth + 16
            val offset = ((System.currentTimeMillis() - labelScrollStartMs) / 35 % cycleWidth).toInt()
            guiGraphics.drawString(font, text, textX - offset, textY, 0xFFFFFFFF.toInt())
            guiGraphics.drawString(font, text, textX - offset + cycleWidth, textY, 0xFFFFFFFF.toInt())
        }

        guiGraphics.disableScissor()

        if (!expanded) {
            if (isHovered) {
                val tooltipLines = selectedMappingLabels(showName = true)
                if (tooltipLines.isNotEmpty()) {
                    guiGraphics.renderTooltip(font, tooltipLines.map { it.visualOrderText }, mouseX, mouseY)
                }
            }
            return
        }

        guiGraphics.pose().pushPose()
        guiGraphics.pose().translate(0f, 0f, 5f)
        guiGraphics.fill(x, popupY, x + popupWidth, popupY + popupHeight, 0x99000000.toInt())

        chartPickerButton.x = x + 5
        chartPickerButton.y = popupY + 1
        chartPickerButton.width = popupWidth - 10
        chartPickerButton.message = Component.literal(if (choosingChart) "Choose a chart" else "< Choose a chart")
        chartPickerButton.active = !choosingChart
        chartPickerButton.render(guiGraphics, mouseX, mouseY, partialTick)

        searchBar?.render(guiGraphics, mouseX, mouseY, partialTick)

        guiGraphics.enableScissor(x, contentY, x + popupWidth, contentY + contentHeight)

        val visibleWidgets = if (choosingChart) chartOptions.map { it.second } else bitEntries
        val firstVisibleY = contentY - rowHeight
        val lastVisibleY = contentY + contentHeight

        visibleWidgets.forEach { widget ->
            if (widget.visible && widget.y in firstVisibleY..lastVisibleY) {
                widget.render(guiGraphics, mouseX, mouseY, partialTick)
            }
        }

        if (visibleWidgets.none { it.visible }) {
            guiGraphics.drawString(font, "No matches", x + 5, contentY + 2, 0xFFFFFFFF.toInt())
        }

        guiGraphics.disableScissor()
        guiGraphics.renderOutline(x - 1, popupY - 1, popupWidth + 1, popupHeight + 1, 0xFFFFFFFF.toInt())
        renderHoveredItemTooltip(guiGraphics, mouseX, mouseY)
        guiGraphics.pose().popPose()
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (expanded && searchBar?.isFocused == true) {
            return searchBar?.keyPressed(keyCode, scanCode, modifiers) == true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun charTyped(codePoint: Char, modifiers: Int): Boolean {
        if (expanded && searchBar?.isFocused == true) {
            return searchBar?.charTyped(codePoint, modifiers) == true
        }
        return super.charTyped(codePoint, modifiers)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (!isMouseOverPopup(mouseX, mouseY)) return false
        updateListScroll(scrollOffset - scrollY * rowHeight)
        return true
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!active || !visible) return false

        if (isMouseOverPopup(mouseX, mouseY)) {
            when {
                mouseY < popupY + rowHeight + 2 ->
                    chartPickerButton.mouseClicked(mouseX, mouseY, button)

                mouseY < contentY -> {
                    searchBar?.isFocused = true
                    searchBar?.mouseClicked(mouseX, mouseY, button)
                    searchBar?.onClick(mouseX, mouseY)
                }

                else -> {
                    val widgets = if (choosingChart) chartOptions.map { it.second } else bitEntries
                    widgets.toList()
                        .filter { it.visible }
                        .forEach { it.mouseClicked(mouseX, mouseY, button) }
                }
            }
            return true
        }

        if (expanded && !super.isMouseOver(mouseX, mouseY)) {
            popupSetOpen(false)
            return true
        }

        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean =
        super.isMouseOver(mouseX, mouseY) || isMouseOverPopup(mouseX, mouseY)

    private fun isMouseOverPopup(mouseX: Double, mouseY: Double): Boolean =
        expanded &&
                mouseX > x && mouseX < x + popupWidth &&
                mouseY > popupY && mouseY < popupY + popupHeight

    private fun updateListScroll(scroll: Double = scrollOffset) {
        val widgets = if (choosingChart) chartOptions.map { it.second } else bitEntries
        val maxScroll = max(0.0, widgets.count { it.visible } * rowHeight.toDouble() - contentHeight)
        scrollOffset = scroll.coerceIn(0.0, maxScroll)

        var visibleIndex = 0
        widgets.forEach { widget ->
            if (widget.visible) {
                widget.x = x + 5
                widget.width = popupWidth - 10
                widget.y = contentY + visibleIndex * rowHeight - scrollOffset.toInt()
                visibleIndex++
            }
        }
    }

    private fun positionPopupControls() {
        chartPickerButton.x = x + 5
        chartPickerButton.y = popupY + 1
        chartPickerButton.width = popupWidth - 10
        searchBar?.setPosition(x + 5, popupY + rowHeight + 2)
        updateListScroll(0.0)
    }

    override fun setX(x: Int) {
        super.setX(x)
        positionPopupControls()
    }

    override fun setY(y: Int) {
        super.setY(y)
        positionPopupControls()
    }

    override fun updateWidgetNarration(narration: NarrationElementOutput) {
        narration.add(NarratedElementType.HINT, "Bit mappings")
    }

    inner class BitEntryButton(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        fixtureName: String,
        movementName: String,
        bitId: BitId,
        private val selectionButton: Boolean,
        private val pressed: () -> Unit
    ) : AbstractButton(x, y, width, height, Component.empty()) {

        val searchKey = "$bitId $movementName $fixtureName".lowercase()
            .replace('_', ' ')
            .replace("duke", "dook")

        val tooltipLines = listOf(
            Component.literal("$fixtureName.$movementName").withStyle(ChatFormatting.WHITE),
            Component.literal("Bit $bitId").withStyle(ChatFormatting.GRAY),
            Component.literal(if (selectionButton) "Click to deselect" else "Hold shift to multi-select")
                .withStyle(ChatFormatting.GRAY)
        )

        private val sprites = WidgetSprites(
            ResourceLocation.withDefaultNamespace("widget/button"),
            ResourceLocation.withDefaultNamespace("widget/button_disabled"),
            ResourceLocation.withDefaultNamespace("widget/button_highlighted")
        )

        init {
            message = Component.literal(bitId.toString())
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(" "))
                .append(Component.literal(movementName).withStyle(ChatFormatting.WHITE))
        }

        override fun onPress() = pressed()

        override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
            val font = Minecraft.getInstance().font

            guiGraphics.setColor(0.4f, 0.4f, 0.4f, alpha)
            RenderSystem.enableBlend()
            RenderSystem.enableDepthTest()
            guiGraphics.blitSprite(sprites.get(active, isHoveredOrFocused()), x, y, width, height)
            guiGraphics.setColor(1f, 1f, 1f, 1f)
            guiGraphics.drawString(font, message, x + 4, y + 1 + (height - font.lineHeight) / 2, 0xFFFFFFFF.toInt())

            if (selectionButton) {
                guiGraphics.drawString(
                    font,
                    "x",
                    x + width - font.width("x") - 5,
                    y + (height - font.lineHeight) / 2,
                    0xFFFF2222.toInt()
                )
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