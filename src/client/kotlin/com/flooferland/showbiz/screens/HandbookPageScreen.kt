package com.flooferland.showbiz.screens

import net.minecraft.*
import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.layouts.*
import net.minecraft.client.gui.screens.*
import net.minecraft.client.gui.screens.inventory.*
import net.minecraft.client.renderer.texture.*
import net.minecraft.core.registries.*
import net.minecraft.network.chat.*
import net.minecraft.resources.*
import net.minecraft.util.*
import com.flooferland.showbiz.accessor.TextureSizeAccessor
import com.flooferland.showbiz.handbook.Handbook
import com.flooferland.showbiz.handbook.HandbookEntry
import com.flooferland.showbiz.handbook.HandbookXml
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
    val contentWidth get() = size - 220

    val entry: HandbookEntry? = Handbook.cache.items.get(key)
    var pageBack: PageButton? = null
    var pageForward: PageButton? = null
    var page = -1

    override fun init() {
        updateLayout()
    }

    fun updateLayout() {
        clearWidgets()
        run {
            val x = textureX + 90
            val y = textureY + (size * 0.77f).toInt()
            pageBack = PageButton(x, y, false, { page -= 1; updateLayout() }, true)
            pageBack?.visible = entry?.let { page > -1 } ?: false
            pageForward = PageButton(x + (size * 0.45).toInt(), y, true, { page += 1; updateLayout() }, true)
            pageForward?.visible = entry?.let { page + 1 < it.pages.size } ?: false
            addRenderableWidget(pageBack!!)
            addRenderableWidget(pageForward!!)
        }

        val layout = LinearLayout.vertical().spacing(6)
        layout.defaultCellSetting().alignHorizontallyLeft()

        runCatching { loadPages(layout) }
            .onFailure {
                layout.addChild(TextWidget(font, Component.literal("Failed to create page layout.\n").append(it.toString()), contentWidth))
            }

        layout.arrangeElements()
        val x = textureX + (size / 2)
        val y = textureY + (size * 0.2f).toInt() + font.lineHeight * 2
        layout.setPosition(x - (layout.width / 2), y)
        layout.visitWidgets(this::addRenderableWidget)
    }

    fun loadPages(layout: LinearLayout) {
        if (entry == null) {
            layout.addChild(TextWidget(font, Component.literal("No page was found"), contentWidth))
        } else if (page < 0) {
            BuiltInRegistries.ITEM.getOptional(key).ifPresent {
                layout.addChild(ItemWidget(it.defaultInstance, size = 32))
                layout.addChild(TextWidget(font, it.getName(it.defaultInstance).copy().withStyle(ChatFormatting.BOLD), contentWidth))
            }
            layout.addChild(TextWidget(font, entry.summary.copy().withStyle(ChatFormatting.ITALIC), contentWidth))
            layout.addChild(SpacerElement.height(font.lineHeight))

            for (fact in entry.facts) {
                layout.addChild(TextWidget(font, fact, contentWidth))
            }
        } else if (page < entry.pages.size) {
            val page = entry.pages[page]
            layout.addChild(TextWidget(font, Component.literal("Title: ${page.title}"), contentWidth))
            for (entry in page.entries) {
                addElement(layout, entry)
            }
        }
    }

    // Probably not a good idea to use recursion..
    fun addElement(layout: LinearLayout, entry: HandbookXml.Element) {
        val settings = LayoutSettings.defaults().alignHorizontallyLeft()
        fun add(widget: AbstractWidget, options: (LayoutSettings) -> LayoutSettings = { it }) {
            layout.addChild(widget, options.invoke(settings))
        }
        fun addText(comp: Component) {
            add(TextWidget(font, comp, contentWidth))
        }
        when (entry) {
            is HandbookXml.Element.Line ->
                addText(Component.literal("- ").append(entry.toComponent()))
            is HandbookXml.Element.TextContent ->
                addText(entry.toComponent())
            is HandbookXml.Element.Image -> {
                val instance = Minecraft.getInstance()
                val id = ResourceLocation.tryParse(entry.src)?.let { id ->
                    var id = id
                    if (!id.path.startsWith("textures/"))
                        id = id.withPrefix("textures/")
                    if (!id.path.endsWith(".png"))
                        id = id.withSuffix(".png")
                    id
                }
                val texture = id?.let { runCatching { instance.textureManager.getTexture(id) }.getOrNull() }
                if (texture is SimpleTexture) {
                    runCatching { texture.load(instance.resourceManager) }
                }
                val accessor = texture as? TextureSizeAccessor
                val width = accessor?.showbiz_getWidth() ?: 0
                val height = accessor?.showbiz_getHeight() ?: 0

                @Suppress("KotlinConstantConditions")
                if (width > 0 && height > 0 && id != null) {
                    val widget = ImageWidget.texture(width, height, id, width, height)
                    widget.tooltip = Component.literal(entry.alt.ifEmpty { "image" }).let { Tooltip.create(it, it) }
                    add(widget)
                } else {
                    addText(Component.literal("[${entry.alt}]"))
                }
            }
            is HandbookXml.Element.ListElement ->
                for (entry in entry.lines) { addElement(layout, entry) }
            is HandbookXml.Element.Container ->
                for (entry in entry.entries) { addElement(layout, entry) }
        }
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