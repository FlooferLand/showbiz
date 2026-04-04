package com.flooferland.showbiz.screens.widgets

import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.gui.components.*
import net.minecraft.network.chat.*
import com.flooferland.showbiz.addons.data.AddonBotEntry
import kotlin.math.roundToInt

/** Lists bots mainly for [com.flooferland.showbiz.screens.BotSelectScreen] (or anything that requires a bot selection list) */
class BotListWidget(x: Int, y: Int, width: Int, height: Int) : ContainerObjectSelectionList<BotListWidget.BotEntry>(Minecraft.getInstance(), width, height, y, 20) {
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

    public fun setBots(bots: Map<String, AddonBotEntry>, click: (id: String) -> Unit) {
        clearEntries()
        bots.forEach { addEntry(BotEntry(it.value) { click(it.key) }) }
    }

    inner class BotEntry(val bot: AddonBotEntry, val click: () -> Unit) : Entry<BotEntry>() {
        val name = bot.name
        val authorString = bot.authors.joinToString(", ")
        val button = Button.builder(Component.literal(name)) { click() }.size(width - 70, itemHeight)
            .tooltip(Tooltip.create(Component.literal("By $authorString")))
            .build()!!

        override fun render(
            guiGraphics: GuiGraphics,
            index: Int,
            top: Int,
            left: Int,
            width: Int,
            height: Int,
            mouseX: Int,
            mouseY: Int,
            hovering: Boolean,
            partialTick: Float
        ) {
            button.x = left + (width - button.width) / 2
            button.y = top
            button.render(guiGraphics, mouseX, mouseY, partialTick)

            runCatching {
                val font = Minecraft.getInstance().font

                // TODO: Move the format colours to be inside the rshw/fshw/etc format container class
                val supported = Component.empty()
                if (bot.accepts.rockafire) supported.append(Component.literal("r").withColor(0x60AF4F2B))
                if (bot.accepts.fnaf1) supported.append(Component.literal("f").withColor(0x608D6320))

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