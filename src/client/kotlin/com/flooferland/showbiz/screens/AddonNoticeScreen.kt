package com.flooferland.showbiz.screens

import net.minecraft.*
import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.screens.*
import net.minecraft.network.chat.*
import net.minecraft.util.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.addons.AddonClientNotices
import com.flooferland.showbiz.addons.AddonNotice

class AddonNoticeScreen(val parent: Screen? = null) : Screen(Component.literal("Showbiz Addon Notice")) {
    lateinit var addonList: SelectionList
    lateinit var noticeList: SelectionList
    var wrapWidth = 0

    override fun init() {
        val minecraft = minecraft ?: return
        if (!Showbiz.config.addons.showNoticeScreenOnReload) {
            minecraft.setScreen(null)
            return
        }

        val listTop = 32
        val listHeight = height - 60
        val addonIds = AddonClientNotices.getAddonIds().toList()

        val nameWidthMax = addonIds.maxOfOrNull { font.width(it) } ?: 0
        val addonWidth = nameWidthMax + font.width(makeBadge(99, 99)) + 25
        val noticeX = addonWidth + 20
        val noticeWidth = width - noticeX - 10
        wrapWidth = noticeWidth - 30

        // Addon name list
        addonList = SelectionList(minecraft, addonWidth, listHeight, listTop, font.lineHeight + 5)
        addonList.place(10, listTop, addonWidth, listHeight)
        for (id in addonIds) {
            val notices = AddonClientNotices.getAddon(id)
            val errors = notices.count { it.severity == AddonNotice.Severity.Error }
            val warnings = notices.count { it.severity == AddonNotice.Severity.Warning }
            val row = AddonRow(id, errors, warnings) { setAddon(id) }
            addonList.addRow(row)
            if (id == addonIds.firstOrNull()) addonList.selected = row
        }
        addWidget(addonList)

        // Notice list
        noticeList = SelectionList(minecraft, noticeWidth, listHeight, listTop, font.lineHeight + 2)
        noticeList.place(noticeX, listTop, noticeWidth, listHeight)
        addWidget(noticeList)

        addonIds.firstOrNull()?.let { setAddon(it) }

        // Close button
        addRenderableWidget(
            Button.builder(CommonComponents.GUI_ACKNOWLEDGE) { onClose() }
                .bounds(width / 2 - 100, height - 25, 200, 20)
                .build()
        )
    }

    override fun onClose() {
        minecraft?.setScreen(parent)
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        guiGraphics.drawCenteredString(font, title, width / 2, 12, CommonColors.WHITE)

        addonList.render(guiGraphics, mouseX, mouseY, partialTick)
        noticeList.render(guiGraphics, mouseX, mouseY, partialTick)
    }

    fun setAddon(addonId: String) {
        noticeList.clearRows()
        noticeList.scrollAmount = 0.0
        for ((i, notice) in AddonClientNotices.getAddon(addonId).withIndex()) {
            val backgroundColor = if (i % 2 == 0) 0x66000000 else 0x66404040
            val (labelText, labelColor) = when (notice.severity) {
                AddonNotice.Severity.Info -> Pair("Info", CommonColors.LIGHTER_GRAY)
                AddonNotice.Severity.Warning -> Pair("Warning", CommonColors.YELLOW)
                AddonNotice.Severity.Error -> Pair("Error", CommonColors.RED)
            }

            val label = Component.literal(labelText)
            notice.title?.let { label.append(Component.literal(" : $it").withStyle(ChatFormatting.GRAY)) }

            val text = notice.text.copy()
            notice.throwable?.let { text.append(Component.literal("\n${it}\n").withStyle(ChatFormatting.GRAY)) }

            // Adding lines
            noticeList.addRow(NoticeLine(label.visualOrderText, labelColor, backgroundColor, 4, true, text))
            font.split(text, wrapWidth).forEach { line ->
                noticeList.addRow(NoticeLine(line, 0xFFFFFF, backgroundColor, 12, false, text))
            }
            noticeList.addRow(NoticeLine(FormattedCharSequence.EMPTY, CommonColors.WHITE, backgroundColor, 0, false, text))
        }
    }

    fun makeBadge(errors: Int, warnings: Int): Component =
        Component.empty()
            .append(Component.literal("$errors E").withStyle(ChatFormatting.RED))
            .append(Component.literal(" "))
            .append(Component.literal("$warnings W").withStyle(ChatFormatting.YELLOW))

    //region Selection lists
    class SelectionList(minecraft: Minecraft, width: Int, height: Int, y: Int, itemHeight: Int) : ObjectSelectionList<Row>(minecraft, width, height, y, itemHeight) {
        override fun getRowWidth() = width - 8
        override fun getRowLeft() = x + 5
        override fun getScrollbarPosition() = x + width - 6

        fun place(x: Int, y: Int, width: Int, height: Int) {
            updateSizeAndPosition(width, height, y)
            this.x = x
        }

        fun addRow(row: Row) = addEntry(row)
        fun clearRows() = clearEntries()
    }

    abstract class Row : ObjectSelectionList.Entry<Row>() {
        override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean = false
    }

    inner class AddonRow(val addonId: String, val errors: Int, val warnings: Int, val onSelect: () -> Unit) : Row() {
        override fun getNarration() = Component.literal(addonId)!!
        override fun render(graphics: GuiGraphics, index: Int, top: Int, left: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovering: Boolean, partialTick: Float) {
            val y = top + (height - font.lineHeight) / 2
            val addonText = Component.literal(addonId)
            val badgeText = makeBadge(errors = errors, warnings = warnings)

            val color = if (hovering) 0xFFFFA0 else CommonColors.WHITE
            graphics.drawString(font, addonText, left + 4, y, color, false)
            graphics.drawString(font, badgeText, left + width - font.width(badgeText) - 8, y, color, false)
        }
        override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            onSelect()
            return true
        }
    }

    inner class NoticeLine(val line: FormattedCharSequence, val color: Int, val backgroundColor: Int, val indent: Int, val topBorder: Boolean, val text: Component) : Row() {
        override fun getNarration() = text
        override fun render(graphics: GuiGraphics, index: Int, top: Int, left: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovering: Boolean, partialTick: Float) {
            val fillTop = top - 4
            graphics.fill(left, fillTop, left + width, top + height, backgroundColor)
            if (topBorder) graphics.fill(left, fillTop, left + width, fillTop + 1, 0x66FFFFFF)
            graphics.drawString(font, line, left + indent, top + (height - font.lineHeight) / 2, color, false)
        }
    }
    //endregion
}