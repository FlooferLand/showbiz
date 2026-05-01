package com.flooferland.showbiz.screens.widgets

import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.gui.components.*
import net.minecraft.network.chat.*
import net.minecraft.util.FastColor
import com.flooferland.showbiz.Showbiz
import com.mojang.blaze3d.systems.RenderSystem
import kotlin.math.roundToInt

/** Lists shw files mainly for [com.flooferland.showbiz.screens.ReelManagerScreen] (or anything that requires a show file listing) */
class ShowFileListWidget(x: Int, y: Int, width: Int, height: Int) : ContainerObjectSelectionList<ShowFileListWidget.FileEntry>(Minecraft.getInstance(), width, height, y, 15) {
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

    public fun setFiles(files: List<String>, click: (String) -> Unit) {
        clearEntries()
        files.forEach { addEntry(FileEntry(it) { click(it) }) }
    }

    inner class FileEntry(nameString: String, val click: () -> Unit) : Entry<FileEntry>() {
        val name = nameString.substringBefore('.')
        val ext = nameString.substringAfter('.')
        val button = Button.builder(Component.literal(name)) { click() }.size(width - 70, itemHeight)
            .tooltip(Tooltip.create(Component.literal(nameString)))
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
                val color = Showbiz.charts.getColor(ext = ext)
                val (x, y) = Pair(button.x - 10, button.y + (button.height * 0.25).roundToInt())
                val (w, h) = Pair(font.width(ext) / 2, font.lineHeight / 2)
                if ((mouseX > x-w && mouseX < x+w) && (mouseY > y-w && mouseY < y+h)) {
                    guiGraphics.renderTooltip(font, Component.literal(ext), mouseX, mouseY)
                }
                RenderSystem.setShaderColor(1f, 1f, 1f, 0.8f)
                guiGraphics.drawCenteredString(font, ext.first().toString(), x, y, color)
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
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