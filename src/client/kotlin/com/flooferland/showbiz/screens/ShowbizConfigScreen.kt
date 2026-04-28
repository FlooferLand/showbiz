package com.flooferland.showbiz.screens

import net.minecraft.client.*
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.screens.*
import net.minecraft.network.chat.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.registry.ModConfig
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
                }
            }
        }.onFailure { Showbiz.log.error("Failure adding config categories", it) }

        // Placing the UI
        if (configEntries.isEmpty()) Minecraft.getInstance().screen = parent
        var categoryWidthsAcc = 0
        for ((categoryIndex, categoryName) in configEntries.keys.withIndex()) {
            val widgets = configEntries[categoryName] ?: continue
            widgets.firstOrNull()?.widget?.isFocused = true

            if (selectedCategory == null) selectedCategory = categoryName

            val categoryWidth = font.width("  $categoryName  ")
            categoryWidthsAcc += categoryWidth
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
                .pos(20 + categoryWidthsAcc, 0)
                .size(categoryWidth, 20)
                .build()
            addRenderableWidget(categoryButton)
            categoryButtons.add(categoryButton)

            for ((widgetIndex, entry) in widgets.withIndex()) {
                val location = widgetIndex + 1
                val (name, widget) = entry
                val spacing = 40
                val x = 20
                val y = (location * spacing) + (categoryIndex * spacing)

                val nameHeight = (font.lineHeight * 1.65f).toInt()
                val nameWidget = StringWidget(x, y, width - x, 20, Component.translatable("config.prop.${categoryName.lowercase()}.$name"), font).alignLeft()
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
                is Boolean -> Checkbox.builder(propName, font)
                    .selected(propValue)
                    .onValueChange { _, bool ->
                        (prop as? KMutableProperty1<T, Boolean>)?.set(category, bool) ?: Showbiz.log.error("Failed to set '${propName.string}'")
                    }
                    .build()
                else -> { Showbiz.log.error("Prop of this type does not exist for ${propName.string}"); return@forEach }
            }

            val widgets = configEntries.getOrPut(categoryName) { mutableListOf() }
            widgets.add(ConfigWidget(prop.name, widget))
        }
    }

    override fun onClose() {
        Showbiz.config = config
        super.onClose()
    }
}