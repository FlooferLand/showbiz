package com.flooferland.showbiz.screens.widgets

import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.gui.components.*
import net.minecraft.network.chat.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.types.ShowFileInfo

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

    public fun setSelected(filename: String) {
        for (child in children()) {
            child.button.active = (child.file.id != filename)
        }
    }

    public fun setFiles(files: Collection<ShowFileInfo>, click: (ShowFileInfo) -> Unit) {
        clearEntries()
        files.forEach { addEntry(FileEntry(it) { click(it) }) }
    }

    inner class FileEntry(val file: ShowFileInfo, val click: () -> Unit) : Entry<FileEntry>() {
        val font = Minecraft.getInstance().font!!
        val name = file.id.substringBefore('.')
        val ext = file.id.substringAfter('.')
        val button = Button.builder(Component.literal(name))
            { setSelected(file.id); click() }
            .size(width - 70, itemHeight)
            .tooltip(Tooltip.create(Component.literal(file.id)))
            .build()!!
        val extLabel = StringWidget(Component.empty(), font)
            .also {
                val color = Showbiz.charts.getColor(ext = ext)
                it.message = Component.literal(ext.firstOrNull()?.toString() ?: ext).withColor(color)
                it.tooltip = Tooltip.create(Component.literal(ext).withColor(color))
            }
            .alignLeft()!!
        val videoLabel = StringWidget(Component.empty(), font)
            .also {
                val color = 0xFF24D3F9.toInt()
                it.message = Component.literal("V").withColor(color)
                it.tooltip = Tooltip.create(Component.literal("Has a video").withColor(color))
            }
            .alignRight()!!

        override fun render(guiGraphics: GuiGraphics, index: Int, top: Int, left: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovering: Boolean, partialTick: Float) {
            button.x = left + (width - button.width) / 2
            button.y = top
            button.render(guiGraphics, mouseX, mouseY, partialTick)

            with(extLabel) {
                this.width = font.width(message) * 2
                this.height = font.lineHeight
                x = button.x - this.width
                y = button.y + (font.lineHeight / 2)
                render(guiGraphics, mouseX, mouseY, partialTick)
            }

            with(videoLabel) {
                this.width = font.width(message) * 2
                this.height = font.lineHeight
                x = button.x + button.width
                y = button.y + (font.lineHeight / 2)
                if (file.hasVideo) render(guiGraphics, mouseX, mouseY, partialTick)
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