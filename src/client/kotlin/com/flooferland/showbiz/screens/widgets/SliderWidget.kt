package com.flooferland.showbiz.screens.widgets

import net.minecraft.client.gui.components.*
import net.minecraft.network.chat.*

@Suppress("PROPERTY_HIDES_JAVA_FIELD")
class SliderWidget(x: Int, y: Int, width: Int, height: Int, var value: Float, val callback: (Float) -> Unit) : AbstractSliderButton(x, y, width, height, Component.empty(), value.toDouble()) {
    override fun updateMessage() {

    }

    override fun applyValue() {
        value = super.value.toFloat()
        callback(value)
    }
}