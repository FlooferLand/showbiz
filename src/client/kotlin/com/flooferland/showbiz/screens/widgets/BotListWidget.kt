package com.flooferland.showbiz.screens.widgets

import net.minecraft.*
import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.gui.components.*
import net.minecraft.client.renderer.*
import net.minecraft.network.chat.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.addons.data.AddonBotEntry
import com.flooferland.showbiz.types.BitChartStore
import com.flooferland.showbiz.types.ResourceId
import kotlin.math.roundToInt

/** Lists bots mainly for [com.flooferland.showbiz.screens.BotSelectScreen] (or anything that requires a bot selection list) */
class BotListWidget(x: Int, y: Int, width: Int, height: Int) : ContainerObjectSelectionList<BotListWidget.BotEntry>(Minecraft.getInstance(), width, height, y, 18) {
    var hiddenCategories = mutableSetOf<String>()

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
        grouped.forEach { (namespace, entries) ->
            val category = BotEntry(category = namespace) {
                if (namespace !in hiddenCategories) {
                    hiddenCategories.add(namespace)
                } else {
                    hiddenCategories.remove(namespace)
                }
                setBots(bots, click)
            }
            addEntry(category)
            if (namespace !in hiddenCategories) entries.forEach { (botId, bot) ->
                if (botId.toString() == "showbiz:conner") return@forEach
                addEntry(BotEntry(bot = bot) { click(botId) })
            }
        }
    }

    inner class BotEntry(val bot: AddonBotEntry? = null, val category: String? = null, val click: (BotEntry) -> Unit) : Entry<BotEntry>() {
        val name = bot?.name
        val authorString = bot?.authors?.joinToString(", ")
        val button = Button.builder(Component.literal(name ?: "")) { click(this) }.size(width - 70, itemHeight)
            .tooltip(Tooltip.create(Component.literal("By $authorString")))
            .build()!!

        override fun render(guiGraphics: GuiGraphics, index: Int, top: Int, left: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovering: Boolean, partialTick: Float) {
            val font = Minecraft.getInstance().font

            // Category
            category?.let { category ->
                val comp = Component.literal(category).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD)
                val pad = 4
                val rect = Rect2i(
                    left + (width - button.width) / 2,
                    top + (height / 2) - pad,
                    button.width,
                    font.lineHeight + (pad * 2)
                )
                val hovered = rect.contains(mouseX, mouseY)
                val color = if (hovered) 0xCC222222.toInt() else 0x99222222.toInt()
                guiGraphics.fill(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height, color)

                val outlineColor = if (hovered) 0xAAFFFFFF.toInt() else 0x88000000.toInt()
                guiGraphics.renderOutline(rect.x, rect.y, rect.width, rect.height, outlineColor)
                guiGraphics.drawCenteredString(font, comp, left + (width / 2), top + (height / 2), 0xfff)
            }

            // Bot / category button
            button.x = left + (width - button.width) / 2
            button.y = top
            if (bot != null) button.render(guiGraphics, mouseX, mouseY, partialTick)

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