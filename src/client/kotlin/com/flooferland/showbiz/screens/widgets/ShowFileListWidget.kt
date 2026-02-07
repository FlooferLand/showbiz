package com.flooferland.showbiz.screens.widgets

import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.gui.components.*
import net.minecraft.core.Vec3i
import net.minecraft.network.chat.*
import net.minecraft.world.phys.Vec2
import com.flooferland.showbiz.types.Vec3f
import com.mojang.blaze3d.systems.RenderSystem
import kotlin.math.roundToInt

/** Lists shw files mainly for [com.flooferland.showbiz.screens.ReelUploadScreen] (or anything that requires a show file listing) */
class ShowFileListWidget(x: Int, y: Int, width: Int, height: Int) : ContainerObjectSelectionList<ShowFileListWidget.FileEntry>(Minecraft.getInstance(), width, height, y, 15) {
    init {
        setPosition(x, y)
    }

    override fun renderListBackground(guiGraphics: GuiGraphics?) {
        super.renderListBackground(guiGraphics)
    }

    override fun renderListSeparators(guiGraphics: GuiGraphics?) {

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
                // TODO: Move the format colours to be inside the rshw/fshw/etc format container class
                val color = when (ext) {
                    "rshw" -> 0x60AF4F2B
                    "fshw" -> 0x608D6320
                    else -> 0x60FFFFFF
                }
                val (x, y) = Pair(button.x - 10, button.y + (button.height * 0.25).roundToInt())
                val (w, h) = Pair(font.width(ext) / 2, font.lineHeight / 2)
                if ((mouseX > x-w && mouseX < x+w) && (mouseY > y-w && mouseY < y+h)) {
                    guiGraphics.renderTooltip(font, Component.literal(ext), mouseX, mouseY)
                }
                guiGraphics.drawCenteredString(font, ext.first().toString(), x, y, color)
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