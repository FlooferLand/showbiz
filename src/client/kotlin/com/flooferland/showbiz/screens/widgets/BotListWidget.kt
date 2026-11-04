package com.flooferland.showbiz.screens.widgets

import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.Showbiz.MOD_ID
import com.flooferland.showbiz.ShowbizClient
import com.flooferland.showbiz.addons.data.AddonBotEntry
import com.flooferland.showbiz.types.BitChartStore
import com.flooferland.showbiz.types.IBot
import com.flooferland.showbiz.types.ResourceId
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.ContainerObjectSelectionList
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.renderer.Rect2i
import net.minecraft.network.chat.Component
import kotlin.math.roundToInt

private typealias OnChange = (botId: ResourceId?, variant: String) -> Unit

/** Lists bots mainly for [com.flooferland.showbiz.screens.BotSelectScreen] (or anything that requires a bot selection list) */
class BotListWidget(x: Int, y: Int, width: Int, height: Int) : ContainerObjectSelectionList<BotListWidget.BotEntry>(Minecraft.getInstance(), width, height, y, 18) {
    var visibleCategories = mutableSetOf<String>()
    var onHover: (botId: ResourceId?) -> Unit = {}
    var onChange: OnChange = { _, _ -> }

    var selectedBot: ResourceId? = null
    var selectedVariant: String = IBot.DEFAULT_VARIANT

    val excluded = listOf(ResourceId(Showbiz.MOD_ID, "conner"))
    var showExcluded = false

    init {
        setPosition(x, y)
    }

    override fun renderListBackground(guiGraphics: GuiGraphics) {
        super.renderListBackground(guiGraphics)
    }

    override fun renderListSeparators(guiGraphics: GuiGraphics) {

    }

    override fun getRowWidth(): Int {
        return (width - 50) - SCROLLBAR_WIDTH
    }

    override fun getScrollbarPosition(): Int {
        return x + width - 5
    }

    public fun setBots(bots: Map<ResourceId, AddonBotEntry>) {
        clearEntries()

        val grouped = bots.entries.sortedBy { it.key.namespace }.groupBy { it.key.namespace }
        selectedBot?.let { visibleCategories.add(it.namespace) }

        // Setting all visible if you have no addons
        if (grouped.keys.all { it == MOD_ID }) visibleCategories = grouped.keys.toMutableSet()

        // Adding the entries
        grouped.forEach { (namespace, botEntries) ->
            val category = BotEntry(category = namespace) { _, _ ->
                if (namespace !in visibleCategories) {
                    visibleCategories.add(namespace)
                } else {
                    visibleCategories.remove(namespace)
                }
                setBots(bots)
            }
            addEntry(category)
            if (namespace in visibleCategories) botEntries.sortedBy { it.value.name }.forEach { (botId, bot) ->
                if (!showExcluded && excluded.contains(botId) && botId != selectedBot) return@forEach
                val entry = BotEntry(botId, bot) { botId, variant ->
                    selectedBot = botId
                    selectedVariant = variant
                    onChange(botId, variant)
                }
                addEntry(entry)
            }
        }
    }

    inner class BotEntry(val botId: ResourceId? = null, val bot: AddonBotEntry? = null, val category: String? = null, val click: OnChange) : Entry<BotEntry>() {
        val name = bot?.name
        val authorString = bot?.authors?.joinToString(", ")
        val nameComp = run {
            val comp = Component.empty()!!
            var insideParen = false
            for (char in name ?: "") {
                if (char == '(') insideParen = true
                val color = if (insideParen) 0xFFDDDDDD.toInt() else 0xFFFFFFFF.toInt()
                comp.append(Component.literal(char.toString()).withColor(color))
                if (char == ')') insideParen = false
            }
            comp
        }

        val variantButton: CycleButton<String> = CycleButton.builder<String>() { Component.literal(it) }
            .withValues(botId?.let { ShowbizClient.bots[it]?.variants?.keys } ?: setOf(selectedVariant))
            .withInitialValue(selectedVariant)
            .displayOnlyValue()
            .withTooltip { Tooltip.create(Component.literal("Variant")) }
            .create(Component.empty()) { _, variant ->
                selectedVariant = variant
                click(selectedBot, selectedVariant)
            }

        val button = Button.builder(nameComp) {
                selectedBot = botId
                click(selectedBot, selectedVariant)
            }
            .tooltip(Tooltip.create(Component.literal("By $authorString")))
            .build()!!

        override fun render(guiGraphics: GuiGraphics, index: Int, top: Int, left: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovering: Boolean, partialTick: Float) {
            val font = Minecraft.getInstance().font

            // Category
            category?.let { category ->
                val pad = 4
                val rect = Rect2i(
                    left,
                    top + (height / 2) - pad,
                    width,
                    font.lineHeight + (pad * 2)
                )
                val open = category in visibleCategories
                val hovered = rect.contains(mouseX, mouseY)
                val bgColor = if (hovered || open) 0xEE444444.toInt() else 0xAA333333.toInt()
                guiGraphics.fill(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height, bgColor)

                val outlineColor = if (hovered) 0xAAFFFFFF.toInt() else 0x88000000.toInt()
                guiGraphics.renderOutline(rect.x, rect.y, rect.width, rect.height, outlineColor)

                val compColor = if (open) ChatFormatting.WHITE else ChatFormatting.GRAY
                val comp = Component.literal(category).withStyle(compColor, ChatFormatting.BOLD)
                guiGraphics.drawCenteredString(font, comp, left + (width / 2), top + (height / 2), 0xfff)
                return
            }

            // Bot / category button
            val selected = selectedBot == botId
            val variants = botId?.let { ShowbizClient.bots[it]?.variants?.keys } ?: setOf(IBot.DEFAULT_VARIANT)
            val showVariant = selected && variants.size > 1
            val variantWidth = if (showVariant) variants.maxOf { font.width(it) + 20 } else 0

            button.setPosition(left, top)
            button.setSize(width - variantWidth, itemHeight)
            button.message = (if (selected) Component.literal(name ?: "").withStyle(ChatFormatting.GRAY) else nameComp.copy())
                .let {
                    if (botId in excluded) it.append(
                        Component.literal(" (hidden)").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)
                    ) else it
                }
            button.tooltip = Tooltip.create(Component.literal(if (selected) "Already selected" else "By $authorString"))

            if (showVariant) {
                variantButton.setPosition(button.right, top)
                variantButton.setSize(variantWidth, itemHeight)
            }
            button.active = !selected
            if (bot != null && botId != null) {
                button.render(guiGraphics, mouseX, mouseY, partialTick)
                if (showVariant) variantButton.render(guiGraphics, mouseX, mouseY, partialTick)
                if (selected && !button.isHoveredOrFocused) {
                    guiGraphics.renderOutline(button.x - 1, button.y - 1, button.width + 1, button.height + 1, 0xDDFFFFFF.toInt())
                }
                if (button.isHoveredOrFocused) onHover(botId)
            }

            // Bot supported formats
            runCatching {
                if (bot == null) return@runCatching

                val supported = Component.empty()
                if (bot.accepts.rockafire) supported.append(Component.literal("r").withColor(Showbiz.charts.getColorId(BitChartStore.RAE_ID)))
                if (bot.accepts.fnaf1) supported.append(Component.literal("f").withColor(Showbiz.charts.getColorId(BitChartStore.FAZ_ID)))

                val (w, h) = Pair(font.width(supported), font.lineHeight / 2)
                val (x, y) = Pair(button.x - 15, button.y + (button.height * 0.25).roundToInt())
                if ((mouseX > x - w && mouseX < x + w) && (mouseY > y - w && mouseY < y + h)) {
                    guiGraphics.renderTooltip(font, Component.literal("Supported formats"), mouseX, mouseY)
                }
                guiGraphics.drawCenteredString(font, supported, x, y, 0xfff)
            }
        }

        override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            if (category != null) {
                click(selectedBot, selectedVariant)
                return true
            }
            return listOf(this.button, this.variantButton).any { it.mouseClicked(mouseX, mouseY, button) }
        }

        override fun children() = if (selectedBot == botId) listOf(button, variantButton) else listOf(button)
        override fun narratables() = if (selectedBot == botId) listOf(button, variantButton) else listOf(button)

        override fun setFocused(focused: Boolean) { button.isFocused = focused }
        override fun isFocused() = button.isFocused
    }
}