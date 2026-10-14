package com.flooferland.showbiz.screens.widgets

import net.minecraft.client.gui.components.*
import net.minecraft.network.chat.*

@Suppress("PROPERTY_HIDES_JAVA_FIELD")
class SliderWidget(x: Int, y: Int, width: Int, height: Int, value: Double, val callback: (Double) -> Unit) : AbstractSliderButton(x, y, width, height, Component.empty(), value.toDouble()) {
    var value: Double
        get() = super.value
        set(value) {
            super.value = Math.clamp(value, 0.0, 1.0)
            applyValue()
            updateMessage()
        }

    init {
        this.value = value
    }

    override fun updateMessage() {

    }

    override fun applyValue() {
        callback(value)
    }
}