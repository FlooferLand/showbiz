package com.flooferland.showbiz.screens

import net.minecraft.*
import net.minecraft.client.*
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.screens.*
import net.minecraft.network.chat.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.registry.ModConfig
import com.flooferland.showbiz.registry.PermissionSelector
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties

typealias Category = String

class ShowbizConfigScreen(val parent: Screen? = null) : Screen(Component.literal("Showbiz Config")) {
    val config = Showbiz.config.clone()

    data class ConfigWidget(val name: String, val widget: AbstractWidget, var nameWidget: StringWidget? = null)
    val configEntries = mutableMapOf<Category, MutableList<ConfigWidget>>()

    val categoryButtons = mutableListOf<Button>()
    var selectedCategory: String? = null

    override fun init() {
        categoryButtons.clear()
        configEntries.clear()
        clearWidgets()

        runCatching {
            for (categoryClass in ModConfig::class.nestedClasses) {
                when (categoryClass) {
                    ModConfig.Audio::class -> categoryAddWidgets("Audio", config.audio)
                    ModConfig.Permissions::class -> categoryAddWidgets("Permissions", config.permissions)
                    ModConfig.Other::class -> categoryAddWidgets("Other", config.other)
                }
            }
        }.onFailure { Showbiz.log.error("Failure adding config categories", it) }

        // Placing the UI

        // Logo
        val logoText = Component.literal("Showbiz").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
        val logo = StringWidget(20, (font.lineHeight / 2) + 4, font.width(logoText), font.lineHeight, logoText, font)
        addRenderableWidget(logo)

        // Categories and widgets
        if (configEntries.isEmpty()) Minecraft.getInstance().setScreen(parent)
        var xAcc = logo.right
        for ((categoryIndex, categoryName) in configEntries.keys.withIndex()) {
            val widgets = configEntries[categoryName] ?: continue
            widgets.firstOrNull()?.widget?.isFocused = true

            if (selectedCategory == null) selectedCategory = categoryName

            // Category button
            val categoryWidth = font.width("    $categoryName    ")
            val categoryButton = Button.builder(Component.literal(categoryName))
                { b ->
                    selectedCategory = categoryName
                    categoryButtons.forEach { button -> button.active = (button != b) }
                    configEntries.forEach { (category, widgets) ->
                        widgets.forEach {
                            it.nameWidget?.visible = category == selectedCategory
                            it.widget.visible = category == selectedCategory
                        }
                    }
                }
                .pos(20 + xAcc, 0)
                .size(categoryWidth, 20)
                .build()
            categoryButton.active = (categoryName != selectedCategory)
            addRenderableWidget(categoryButton)
            categoryButtons.add(categoryButton)
            xAcc += categoryWidth

            // Widgets
            for ((widgetIndex, entry) in widgets.withIndex()) {
                val location = widgetIndex + 1
                val (name, widget) = entry
                val spacing = 40
                val x = 20
                val y = (location * spacing)

                val nameHeight = (font.lineHeight * 1.7f).toInt()
                val nameText = Component.translatable("config.prop.${categoryName.lowercase()}.$name")
                val nameWidget = StringWidget(x, y, width - x, 20, nameText, font).alignLeft()
                nameWidget.visible = (selectedCategory == categoryName)
                addRenderableWidget(nameWidget)
                entry.nameWidget = nameWidget

                widget.x = x
                widget.y = y + nameHeight
                widget.visible = (selectedCategory == categoryName)
                addRenderableWidget(widget)
            }
        }
    }

    private inline fun <reified T: Any> categoryAddWidgets(categoryName: String, category: T) {
        val props = T::class.memberProperties
        props.forEach { prop ->
            val categoryName = categoryName
            val propName = Component.literal(prop.name)
            val propValue = prop.call(category) ?: return@forEach

            @Suppress("UNCHECKED_CAST")
            val widget = when (propValue) {
                is Boolean -> Checkbox.builder(Component.literal("Enable").withStyle(ChatFormatting.GRAY), font)
                    .selected(propValue)
                    .onValueChange { _, bool ->
                        (prop as? KMutableProperty1<T, Boolean>)?.set(category, bool) ?: Showbiz.log.error("Failed to set '${propName.string}'")
                    }
                    .build()
                is PermissionSelector -> CycleButton.builder<PermissionSelector> { Component.literal(it.name) }
                    .withInitialValue(propValue)
                    .withValues(PermissionSelector.entries)
                    .create(Component.literal("Minimum")) { b, value ->
                        (prop as? KMutableProperty1<T, PermissionSelector>)?.set(category, value) ?: Showbiz.log.error("Failed to set '${propName.string}'")
                    }
                else -> { Showbiz.log.error("Prop of this type does not exist for ${propName.string}"); return@forEach }
            }

            val widgets = configEntries.getOrPut(categoryName) { mutableListOf() }
            widgets.add(ConfigWidget(prop.name, widget))
        }
    }

    override fun onClose() {
        Showbiz.config = config
        Showbiz.config.save()
        Minecraft.getInstance().setScreen(parent)
        super.onClose()
    }
}