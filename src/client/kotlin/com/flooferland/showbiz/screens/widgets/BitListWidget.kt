package com.flooferland.showbiz.screens.widgets

import net.minecraft.*
import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.components.events.*
import net.minecraft.client.gui.narration.*
import net.minecraft.network.chat.*
import net.minecraft.util.*
import com.flooferland.bizlib.bits.BitUtils
import com.flooferland.showbiz.show.BitId
import com.flooferland.showbiz.show.Drawer
import com.flooferland.showbiz.show.toBitId
import com.flooferland.showbiz.types.connection.data.PackedShowData

/** Lists bits mainly for [com.flooferland.showbiz.screens.BitViewScreen] (or anything that requires a bit view list) */
class BitListWidget(x: Int, y: Int, width: Int, height: Int) : ContainerObjectSelectionList<BitListWidget.BitLineEntry>(Minecraft.getInstance(), width, height, y, SIZE) {
    val entries = mutableListOf<BitLineEntry>()
    companion object {
        const val SIZE: Int = 22
    }

    val padding = 10
    val innerWidth get() = width - padding
    val bitsPerRow get() = (innerWidth / SIZE)

    var showBitIds = true
    var frame: PackedShowData? = null
    var hoveredBit: Int? = null
    var hoveredBitOn = false

    var bitInfo = hashMapOf<BitId, String>()

    init {
        setPosition(x, y)
        for (i in 0..BitUtils.DRAWER_MAX.toInt() / bitsPerRow) {
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

        updateBitInfo()

        // Title
        run {
            val title = Component.literal("Bit View")
            frame?.mapping?.let { if (it.isNotEmpty()) title.append(" ($it)") }
            guiGraphics.drawString(font, title, x, 5, 0xFFFFFFFF.toInt())
        }

        hoveredBit?.let { bit ->
            val color = if (hoveredBitOn) ChatFormatting.GREEN else ChatFormatting.WHITE
            val bitInfo = bitInfo[bit.toBitId()] ?: "n/a"
            val bitIdComp = Component.literal("Bit $bit").withStyle(color)
                .append(Component.literal(" (${Drawer.formatBit(bit.toBitId())})").withStyle(ChatFormatting.GRAY))
            val bitInfoComp = Component.literal(bitInfo).withStyle(ChatFormatting.GRAY)
            guiGraphics.renderComponentTooltip(font, listOf(bitIdComp, bitInfoComp), mouseX, mouseY)
            hoveredBit = null
        }
    }

    fun updateBitInfo() {
        bitInfo.clear()
        frame?.mapping?.let {
            val bitMap = BitUtils.readBitmap(it) ?: return@let null
            for ((fixture, movements) in bitMap.entries) {
                for ((name, moveBit) in movements)
                    bitInfo[moveBit] = "$fixture.$name"
            }
            return@let null
        }
    }

    inner class BitLineEntry(val rowNum: Int) : Entry<BitLineEntry>() {
        val colorOn = 0xFF_00_CC_00.toInt()
        val colorOff = 0xFF_30_30_30.toInt()

        val colorNamedBit = 0x33_00_FF_22.toInt()
        val colorUnnamedBit = 0x66_00_00_00.toInt()

        override fun render(guiGraphics: GuiGraphics, index: Int, top: Int, left: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovering: Boolean, partialTick: Float) {
            val rowWidth = bitsPerRow * SIZE
            val x = this@BitListWidget.x + (this@BitListWidget.width / 2) - (rowWidth / 2)
            val y = top + (height / 2)
            val font = Minecraft.getInstance().font

            for (i in 1 until bitsPerRow) {
                val bitId = (rowNum * bitsPerRow) + i
                if (bitId >= BitUtils.DRAWER_MAX.toInt()) break

                val info = bitInfo[bitId.toBitId()]

                val bitOn = frame?.signal?.frameHas(bitId) ?: false
                val bitColor = if (bitOn) colorOn else colorOff
                val x = x + (i * SIZE)
                guiGraphics.fill(x, y, x + SIZE, y + SIZE, 0xAA000000.toInt())
                guiGraphics.fill(x + 1, y + 1, x + SIZE - 1, y + SIZE - 1, if (info != null) colorNamedBit else colorUnnamedBit)
                guiGraphics.fill(x + 2, y + 2, x + SIZE - 2, y + SIZE - 2, bitColor)
                if (showBitIds) guiGraphics.drawCenteredString(font, bitId.toString(), x + (SIZE / 2), y + 7, FastColor.ARGB32.color(150, bitColor))
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