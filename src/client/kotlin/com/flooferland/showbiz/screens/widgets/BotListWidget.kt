package com.flooferland.showbiz.screens.widgets

import net.minecraft.*
import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.gui.components.*
import net.minecraft.client.renderer.*
import net.minecraft.network.chat.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.Showbiz.MOD_ID
import com.flooferland.showbiz.addons.data.AddonBotEntry
import com.flooferland.showbiz.types.BitChartStore
import com.flooferland.showbiz.types.ResourceId
import kotlin.math.roundToInt

/** Lists bots mainly for [com.flooferland.showbiz.screens.BotSelectScreen] (or anything that requires a bot selection list) */
class BotListWidget(x: Int, y: Int, width: Int, height: Int) : ContainerObjectSelectionList<BotListWidget.BotEntry>(Minecraft.getInstance(), width, height, y, 18) {
    var visibleCategories = mutableSetOf<String>()
    var onHover: (ResourceId?) -> Unit = {}
    var selected: ResourceId? = null

    init {
        setPosition(x, y)
    }

    override fun renderListBackground(guiGraphics: GuiGraphics) {
        super.renderListBackground(guiGraphics)
    }

    override fun renderListSeparators(guiGraphics: GuiGraphics) {

    }

    override fun getScrollbarPosition(): Int {
        return x + width - 5
    }

    public fun setBots(bots: Map<ResourceId, AddonBotEntry>, click: (id: ResourceId) -> Unit) {
        clearEntries()

        val grouped = bots.entries.sortedBy { it.key.namespace }.groupBy { it.key.namespace }
        selected?.let { visibleCategories.add(it.namespace) }

        // All visible if you have no addons
        if (grouped.keys.all { it == MOD_ID }) visibleCategories = grouped.keys.toMutableSet()

        grouped.forEach { (namespace, botEntries) ->
            val category = BotEntry(category = namespace) {
                if (namespace !in visibleCategories) {
                    visibleCategories.add(namespace)
                } else {
                    visibleCategories.remove(namespace)
                }
                setBots(bots, click)
            }
            addEntry(category)
            if (namespace in visibleCategories) botEntries.forEach { (botId, bot) ->
                if (botId.toString() == "showbiz:conner") return@forEach
                addEntry(BotEntry(botId, bot) { click(botId) })
            }
        }
    }

    inner class BotEntry(val botId: ResourceId? = null, val bot: AddonBotEntry? = null, val category: String? = null, val click: (BotEntry) -> Unit) : Entry<BotEntry>() {
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
        val button = Button.builder(nameComp) { click(this) }.size(width - 70, itemHeight)
            .tooltip(Tooltip.create(Component.literal("By $authorString")))
            .build()!!
            .also {
                if (selected == botId) {
                    it.active = false
                    it.message = Component.literal(it.message.string).withStyle(ChatFormatting.GRAY)
                    it.tooltip = Tooltip.create(Component.literal("Already selected"))
                }
            }

        override fun render(guiGraphics: GuiGraphics, index: Int, top: Int, left: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovering: Boolean, partialTick: Float) {
            val font = Minecraft.getInstance().font

            // Category
            category?.let { category ->
                val pad = 4
                val rect = Rect2i(
                    left + (width - button.width) / 2,
                    top + (height / 2) - pad,
                    button.width,
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
            }

            // Bot / category button
            button.x = left + (width - button.width) / 2
            button.y = top
            if (bot != null && botId != null) {
                button.render(guiGraphics, mouseX, mouseY, partialTick)
                if (botId == selected && !button.isHoveredOrFocused) {
                    guiGraphics.renderOutline(button.x - 1, button.y - 1, button.width + 1, button.height + 1, 0xDDFFFFFF.toInt())
                }
                if (button.isHoveredOrFocused) onHover(botId)
            }

            // Bot supported formats
            runCatching {
                if (bot == null) return@runCatching

                val supported = Component.empty()
                if (bot.accepts.rockafire) supported.append(Component.literal("r").withColor(Showbiz.charts.getColor(id = BitChartStore.RAE_ID)))
                if (bot.accepts.fnaf1) supported.append(Component.literal("f").withColor(Showbiz.charts.getColor(id = BitChartStore.FAZ_ID)))

                val (w, h) = Pair(font.width(supported), font.lineHeight / 2)
                val (x, y) = Pair(button.x - 15, button.y + (button.height * 0.25).roundToInt())
                if ((mouseX > x - w && mouseX < x + w) && (mouseY > y - w && mouseY < y + h)) {
                    guiGraphics.renderTooltip(font, Component.literal("Supported formats"), mouseX, mouseY)
                }
                guiGraphics.drawCenteredString(font, supported, x, y, 0xfff)
            }
        }

        override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int) =
            this.button.mouseClicked(mouseX, mouseY, button)

        override fun children() = listOf(button)
        override fun narratables() = listOf(button)

        override fun setFocused(focused: Boolean) { button.isFocused = focused }
        override fun isFocused() = button.isFocused
    }
}