package com.flooferland.showbiz.screens

import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Checkbox
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import com.flooferland.showbiz.ShowbizClient
import com.flooferland.showbiz.ShowbizClientConfig
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.full.staticProperties

typealias Category = String

class ShowbizConfigScreen : Screen(Component.literal("Showbiz Config")) {
    val config = ShowbizClient.config.clone()

    data class ConfigWidget(val name: String, val widget: AbstractWidget)
    val configEntries = mutableMapOf<Category, MutableList<ConfigWidget>>()

    override fun init() {
        for (categoryClass in ShowbizClientConfig::class.nestedClasses) {
            when (categoryClass) {
                ShowbizClientConfig.Audio::class -> categoryAddWidgets("Audio", config.audio)
            }
        }

        // Placing the UI
        for ((categoryName, widgets) in configEntries) {
            widgets.firstOrNull()?.widget?.isFocused = true

            val categoryNameWidget = StringWidget(20, 0, width, 20, Component.literal(categoryName), font).alignLeft()
            addRenderableWidget(categoryNameWidget)
            for ((i, entry) in widgets.withIndex()) {
                val location = i + 1
                val (name, widget) = entry
                val spacing = 40
                val x = 20
                val y = (location * spacing)

                val nameHeight = (font.lineHeight * 1.65f).toInt()
                val nameWidget = StringWidget(x, y, width - x, 20, Component.literal(name), font).alignLeft()
                addRenderableWidget(nameWidget)

                widget.x = x
                widget.y = y + nameHeight
                addRenderableWidget(widget)
            }
        }
    }

    private inline fun <reified T> categoryAddWidgets(categoryName: String, category: T) {
        val props = T::class.staticProperties
        props.forEach { prop ->
            val prop = prop as KMutableProperty0
            val categoryName = categoryName
            val propName = Component.literal(prop.name)
            val propValue = prop.get()!!

            @Suppress("UNCHECKED_CAST")
            val widget = when (propValue) {
                is Boolean -> Checkbox.builder(propName, font)
                    .selected(propValue)
                    .onValueChange { _, bool -> (prop as KMutableProperty0<Boolean>).set(bool) }
                    .build()
                else -> error("Prop of this type does not exist")
            }

            val widgets = configEntries.getOrPut(categoryName) { mutableListOf() }
            widgets.add(ConfigWidget(prop.name, widget))
        }
    }

    override fun onClose() {
        ShowbizClient.config = config
    }
}