package com.flooferland.showbiz.screens

import com.flooferland.showbiz.handbook.Handbook
import com.flooferland.showbiz.handbook.Handbook.cache
import com.flooferland.showbiz.handbook.HandbookEntry
import com.flooferland.showbiz.handbook.HandbookXml
import com.flooferland.showbiz.interfaces.TextureSizeAccessor
import com.flooferland.showbiz.mixin.accessor.AbstractContainerScreenAccessor
import com.flooferland.showbiz.registry.ModClientInput
import com.flooferland.showbiz.screens.widgets.ItemWidget
import com.flooferland.showbiz.screens.widgets.TextWidget
import com.flooferland.showbiz.utils.Sounds.localPlayer
import com.flooferland.showbiz.utils.rl
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.ImageWidget
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.layouts.LayoutSettings
import net.minecraft.client.gui.layouts.LinearLayout
import net.minecraft.client.gui.layouts.SpacerElement
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.PageButton
import net.minecraft.client.renderer.texture.SimpleTexture
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.CommonColors
import net.minecraft.world.item.ItemStack
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import kotlin.math.roundToInt

class HandbookPageScreen(val parent: Screen? = null, val key: ResourceLocation) : Screen(Component.literal("The Showbiz Handbook")) {
    val background = rl("textures/gui/handbook_page.png")

    val texSize = 256
    val size get() = (texSize * 1.5).roundToInt()
    val textureX get() = (width / 2) - (size / 2)
    val textureY get() = (height / 2) - (size / 2)
    val contentWidth get() = size - 220
    val contentHeight get() = size - 220

    val entry: HandbookEntry? = Handbook.cache.items.get(key)
    var pageBack: PageButton? = null
    var pageForward: PageButton? = null
    var page: Page = Page.Main()

    sealed class Page(val id: String) {
        class Main : Page("main") {
            override fun getName(entry: HandbookEntry) =
                Component.literal("Main")!!
        }
        class Facts : Page("facts") {
            override fun getName(entry: HandbookEntry) =
                Component.literal("Facts")!!
        }
        class Content(val number: Int = 0) : Page("page$number") {
            override fun getName(entry: HandbookEntry) =
                entry.pages.getOrNull(number)?.title?.let { Component.literal(it) } ?: Component.literal("Page $number")!!
        }

        abstract fun getName(entry: HandbookEntry): Component

        fun back(entry: HandbookEntry): Page = when (this) {
            is Main -> this
            is Facts -> Main()
            is Content -> when {
                number > 0 -> Content(number - 1)
                entry.facts.isNotEmpty() -> Facts()
                else -> Main()
            }
        }

        fun next(entry: HandbookEntry): Page = when (this) {
            is Main -> when {
                entry.facts.isNotEmpty() -> Facts()
                entry.pages.isNotEmpty() -> Content(0)
                else -> this
            }
            is Facts -> when {
                entry.pages.isNotEmpty() -> Content(0)
                else -> this
            }
            is Content -> when {
                number + 1 < entry.pages.size -> Content(number + 1)
                else -> this
            }
        }
    }

    override fun init() {
        updateLayout()
    }

    fun pageBack() {
        page = page.back(entry ?: return)
        updateLayout()
    }

    fun pageNext() {
        page = page.next(entry ?: return)
        updateLayout()
    }

    fun updateLayout() {
        clearWidgets()
        val entry = entry ?: return

        run {
            val x = textureX + 110
            val y = textureY + (size * 0.76f).toInt()
            pageBack = PageButton(x, y, false, { pageBack() }, true)
                .also { button ->
                    val back = page.back(entry)
                    button.visible = back.id != page.id
                    button.tooltip = Tooltip.create(back.getName(entry))
                    addRenderableWidget(button)
                }
            pageForward = PageButton(x + 140, y, true, { pageNext() }, true)
                .also { button ->
                    val next = page.next(entry)
                    button.visible = next.id != page.id
                    button.tooltip = Tooltip.create(next.getName(entry))
                    addRenderableWidget(button)
                }
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
            addTitle(layout, "Page $page was not found")
            return
        }

        val page = page
        when (page) {
            is Page.Main -> {
                val itemSize = 32
                BuiltInRegistries.ITEM.getOptional(key).ifPresent {
                    layout.addChild(ItemWidget(it.defaultInstance, size = itemSize))
                    layout.addChild(TextWidget(font, it.getName(it.defaultInstance).copy().withStyle(ChatFormatting.BOLD), contentWidth))
                }

                for (entry in entry.summary.entries) {
                    addElement(layout, entry)
                }
                layout.addChild(SpacerElement.height(font.lineHeight))
            }

            is Page.Facts -> {
                for (fact in entry.facts) {
                    layout.addChild(TextWidget(font, fact, contentWidth))
                }
            }

            // Pages
            is Page.Content -> {
                val page = entry.pages.getOrNull(page.number) ?: run {
                    addTitle(layout, "Failed to find page ${page.number}")
                    return
                }
                for (entry in page.entries) {
                    addElement(layout, entry)
                }
            }
        }
    }

    fun addTitle(layout: LinearLayout, text: String) {
        layout.addChild(TextWidget(font, Component.literal(text).withStyle(ChatFormatting.BOLD), contentWidth))
        layout.addChild(SpacerElement.height(font.lineHeight))
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
            is HandbookXml.Element.Line -> {
                addText(Component.literal("- ").append(entry.toComp()))
            }
            is HandbookXml.Element.TextContent -> {
                addText(entry.toComp())
            }
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
            is HandbookXml.Element.ListElement -> {
                for (entry in entry.lines) addElement(layout, entry)
            }
            is HandbookXml.Element.Container -> {
                for (entry in entry.entries) addElement(layout, entry)
            }
            is HandbookXml.Element.Summary -> {
                for (entry in entry.entries) addElement(layout, entry)
            }
        }
    }

    override fun onClose() {
        if (parent != null) minecraft?.setScreen(parent)
        else super.onClose()
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(graphics, mouseX, mouseY, partialTick)

        // Handbook title
        val title = run {
            val text = if (page !is Page.Main && entry != null) page.getName(entry).copy() else title.copy()
            text.withStyle(ChatFormatting.BOLD)
        }
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

    companion object {
        /**
         * Not sure where else to put this.
         * Handles opening the screen and cancelling other input.
         * Called in mixins
         */
        fun handleKeyPressed(self: AbstractContainerScreenAccessor, keyCode: Int, scanCode: Int, modifiers: Int, cir: CallbackInfoReturnable<Boolean?>) {
            val instance = Minecraft.getInstance()
            val player = instance.player ?: return
            val hoveredSlot = self.hoveredSlot ?: return
            if (!hoveredSlot.hasItem()) return

            val pressed = ModClientInput.OpenInHandbook.mapping.matches(keyCode, scanCode)
            if (pressed && instance.screen !is HandbookPageScreen) {
                val stack: ItemStack = hoveredSlot.getItem()
                val id = BuiltInRegistries.ITEM.getKey(stack.getItem())
                val entry = cache.items.get(id) ?: return

                instance.setScreen(HandbookPageScreen(instance.screen, id))
                localPlayer(player).click()
                cir.setReturnValue(true)
            }
        }
    }
}