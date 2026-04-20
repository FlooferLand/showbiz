package com.flooferland.showbiz.screens.widgets

import net.minecraft.ChatFormatting
import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.network.chat.*
import com.flooferland.bizlib.bits.BitUtils
import com.flooferland.showbiz.show.Drawer
import com.flooferland.showbiz.show.toBitId
import com.flooferland.showbiz.types.connection.data.PackedShowData

/** Lists bits mainly for [com.flooferland.showbiz.screens.BitViewScreen] (or anything that requires a bit view list) */
class BitListWidget(x: Int, y: Int, width: Int, height: Int) : ContainerObjectSelectionList<BitListWidget.BitLineEntry>(Minecraft.getInstance(), width, height, y, SIZE) {
    val entries = mutableListOf<BitLineEntry>()
    companion object {
        const val SIZE: Int = 15
    }

    val padding = 20
    val innerWidth get() = width - padding
    val bitsPerRow get() = (innerWidth / SIZE)

    var frame: PackedShowData? = null
    var hoveredBit: Int? = null
    var hoveredBitOn = false

    init {
        setPosition(x, y)
        for (i in 0..(BitUtils.NEXT_DRAWER * 2u).toInt() / bitsPerRow) {
            val entry = BitLineEntry(i)
            entries.add(entry)
            addEntry(entry)
        }
    }

    override fun renderListBackground(guiGraphics: GuiGraphics) {
        super.renderListBackground(guiGraphics)
    }

    override fun renderListSeparators(guiGraphics: GuiGraphics) {

    }

    override fun getScrollbarPosition(): Int {
        return x + width - 5
    }

    override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick)
        val font = Minecraft.getInstance().font

        // Title
        run {
            val title = Component.literal("Bit View")
            frame?.mapping?.let { if (it.isNotEmpty()) title.append(" ($it)") }
            guiGraphics.drawString(font, title, x, 10, 0xFFFFFFFF.toInt())
        }

        hoveredBit?.let { bit ->
            val color = if (hoveredBitOn) ChatFormatting.GREEN else ChatFormatting.WHITE
            val bitInfo = frame?.mapping?.let {
                val bitMap = BitUtils.readBitmap(it) ?: return@let null
                for ((fixture, movements) in bitMap.entries) {
                    for ((name, moveBit) in movements)
                        if (moveBit == bit.toBitId()) return@let "$fixture.$name"
                }
                return@let null
            } ?: "n/a"
            val bitIdComp = Component.literal("Bit $bit").withStyle(color)
                .append(Component.literal(" (${Drawer.formatBit(bit.toBitId())})").withStyle(ChatFormatting.GRAY))
            val bitInfoComp = Component.literal(bitInfo).withStyle(ChatFormatting.GRAY)
            guiGraphics.renderComponentTooltip(font, listOf(bitIdComp, bitInfoComp), mouseX, mouseY)
            hoveredBit = null
        }
    }

    inner class BitLineEntry(val rowNum: Int) : Entry<BitLineEntry>() {
        val colorOn = 0xFF_00_CC_00.toInt()
        val colorOff = 0xFF_30_30_30.toInt()

        override fun render(guiGraphics: GuiGraphics, index: Int, top: Int, left: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovering: Boolean, partialTick: Float) {
            val rowWidth = bitsPerRow * SIZE
            val x = this@BitListWidget.x + (this@BitListWidget.width / 2) - (rowWidth / 2)
            val y = top + (height / 2)

            for (i in 0 until bitsPerRow) {
                val bitId = (rowNum * bitsPerRow) + i
                if (bitId >= (BitUtils.NEXT_DRAWER * 2u).toInt()) break

                val bitOn = frame?.signal?.frameHas(bitId) ?: false
                val x = x + (i * SIZE)
                guiGraphics.fill(x, y, x + SIZE, y + SIZE, 0xAA000000.toInt())
                guiGraphics.fill(x + 2, y + 2, x + SIZE - 2, y + SIZE - 2, if (bitOn) colorOn else colorOff)
                if (mouseX in x..x + SIZE && mouseY in y..y + SIZE) {
                    hoveredBit = bitId
                    hoveredBitOn = bitOn
                }
            }
        }

        override fun children() = listOf<GuiEventListener>()
        override fun narratables() = listOf<NarratableEntry>()
    }
}