package com.flooferland.showbiz.screens

import net.minecraft.*
import net.minecraft.client.gui.*
import net.minecraft.client.gui.layouts.*
import net.minecraft.client.gui.screens.*
import net.minecraft.core.registries.*
import net.minecraft.network.chat.*
import net.minecraft.resources.*
import net.minecraft.util.*
import com.flooferland.showbiz.handbook.Handbook
import com.flooferland.showbiz.handbook.HandbookEntry
import com.flooferland.showbiz.screens.widgets.ItemWidget
import com.flooferland.showbiz.screens.widgets.TextWidget
import com.flooferland.showbiz.utils.rl
import com.mojang.blaze3d.systems.RenderSystem
import kotlin.math.roundToInt

class HandbookPageScreen(val parent: Screen? = null, val key: ResourceLocation) : Screen(Component.literal("The Showbiz Handbook").withStyle(ChatFormatting.BOLD)) {
    val background = rl("textures/gui/handbook_page.png")

    val texSize = 256
    val size get() = (texSize * 1.5).roundToInt()
    val textureX get() = (width / 2) - (size / 2)
    val textureY get() = (height / 2) - (size / 2)

    val entry: HandbookEntry? = Handbook.cache.items.get(key)

    override fun init() {
        val contentWidth = size - 220
        val layout = LinearLayout.vertical().spacing(6)
        layout.defaultCellSetting().alignHorizontallyCenter()

        if (entry == null) {
            layout.addChild(TextWidget(font, Component.literal("No page was found"), contentWidth))
        } else {
            BuiltInRegistries.ITEM.getOptional(key).ifPresent {
                layout.addChild(ItemWidget(it.defaultInstance, size = 32))
                layout.addChild(TextWidget(font, it.getName(it.defaultInstance).copy().withStyle(ChatFormatting.BOLD), contentWidth))
            }
            layout.addChild(TextWidget(font, Component.literal(entry.summary).withStyle(ChatFormatting.ITALIC), contentWidth))
            layout.addChild(SpacerElement.height(font.lineHeight))

            for (fact in entry.facts) {
                layout.addChild(TextWidget(font, Component.literal(fact), contentWidth))
            }
        }

        layout.arrangeElements()
        val x = textureX + (size / 2)
        val y = textureY + (size * 0.2f).toInt() + font.lineHeight * 2
        layout.setPosition(x - (layout.width / 2), y)
        layout.visitWidgets(this::addRenderableWidget)
    }

    override fun onClose() {
        if (parent != null) minecraft?.setScreen(parent)
        else super.onClose()
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(graphics, mouseX, mouseY, partialTick)

        // Handbook title
        val x = textureX + (size / 2) - (font.width(title) / 2)
        val y = textureY + (size * 0.2f).toInt()
        graphics.drawString(font, title, x, y, CommonColors.BLACK, false)
    }

    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        RenderSystem.defaultBlendFunc()
        graphics.setColor(1f, 1f, 1f, 1f)
        super.renderBackground(graphics, mouseX, mouseY, partialTick)

        RenderSystem.enableBlend()
        graphics.blit(background, textureX, textureY, 0f, 0f, size, size, size, size)

        RenderSystem.defaultBlendFunc()
        graphics.setColor(1f, 1f, 1f, 1f)
    }
}