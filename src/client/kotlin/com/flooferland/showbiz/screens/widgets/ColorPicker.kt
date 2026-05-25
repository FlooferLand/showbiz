package com.flooferland.showbiz.screens.widgets

import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.narration.*
import net.minecraft.network.chat.*
import net.minecraft.util.*
import java.awt.Color

class ColorPicker(x: Int, y: Int, width: Int, height: Int, defaultColor: Int) : AbstractContainerWidget(x, y, width, height, Component.empty()) {
    val pad get() = 2
    val sliderHeight get() = height / 3

    data class SliderData(val string: StringWidget, val slider: SliderWidget)

    val sliders = mutableListOf<SliderData>()
    val children = mutableListOf<AbstractWidget>()

    private data class HSV(var h: Float, var s: Float, var v: Float)
    private val state = HSV(0f, 1f, 1f)

    var value: Int
        get() = FastColor.ARGB32.color(255, Color.HSBtoRGB(state.h, state.s, state.v))
        set(rgb) {
            val hsb = Color.RGBtoHSB(FastColor.ARGB32.red(rgb), FastColor.ARGB32.green(rgb), FastColor.ARGB32.blue(rgb), null)
            if (hsb[2] > 0f) { state.h = hsb[0]; state.s = hsb[1] }
            state.v = hsb[2]
            sliders.forEachIndexed { i, data -> data.slider.value = if(i == 0) state.h else if(i == 1) state.s else state.v }
        }

    init {
        fun addSlider(text: String, default: Float) {
            val textComp = Component.literal(text)
            val textWidth = Minecraft.getInstance().font.width(textComp)
            val title = StringWidget(0, 0, textWidth, sliderHeight - pad, textComp, Minecraft.getInstance().font)
            val slider = SliderWidget(0, 0, width - (textWidth * 2) - pad, sliderHeight - pad, default) {
                state.h = sliders[0].slider.value
                state.s = sliders[1].slider.value
                state.v = sliders[2].slider.value
            }
            children += title
            children += slider
            sliders += SliderData(title, slider)
        }

        addSlider("H", 0f)
        addSlider("S", 1f)
        addSlider("V", 1f)
        updatePositions()
        value = defaultColor
    }

    fun updatePositions() {
        var yPos = y + pad
        for (data in sliders) {
            data.string.setPosition(x + pad, yPos)
            data.slider.setPosition(x + pad + data.string.width, yPos)
            yPos += data.slider.height + 2
        }
    }

    override fun setX(x: Int) {
        super.setX(x)
        updatePositions()
    }
    override fun setY(y: Int) {
        super.setY(y)
        updatePositions()
    }

    override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        guiGraphics.fill(x, y, x + width, y + height, value)
        children.forEach { it.render(guiGraphics, mouseX, mouseY, partialTick) }
    }

    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {

    }

    override fun children() = children
}