package com.flooferland.showbiz.screens.widgets

import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.narration.*
import net.minecraft.network.chat.*
import net.minecraft.util.*
import com.flooferland.showbiz.types.math.Color3
import com.flooferland.showbiz.types.math.Kelvin
import com.flooferland.showbiz.utils.Extensions.formatDecimal
import java.awt.Color
import kotlin.math.roundToInt

class ColorPicker(x: Int, y: Int, width: Int, height: Int, defaultColor: Int? = null, defaultKelvin: Int? = null, defaultMode: Mode? = null) : AbstractContainerWidget(x, y, width, height, Component.empty()) {
    val pad get() = 2
    val sliderHeight get() = height / 3
    val kelvinRange get() = 3000..20000

    data class SliderData(val string: StringWidget, val slider: SliderWidget) {
        var visible: Boolean
            get() = string.visible && slider.visible
            set(value) {
                string.visible = value
                slider.visible = value
            }
        operator fun component3() = visible
    }
    enum class Mode { HSV, Kelvin }

    var sliderHue: SliderData
    var sliderSat: SliderData
    var sliderKel: SliderData
    var sliderVal: SliderData
    var modeHsv: Button
    var modeKel: Button
    val sliders = mutableListOf<SliderData>()
    val modeButtons = hashMapOf<Mode, Button>()
    val children = mutableListOf<AbstractWidget>()

    var allowedModes = Mode.entries.toMutableSet()
        set(value) {
            field = value
            if (mode !in value) mode = value.firstOrNull() ?: mode
            updateMode()
        }
    var prevMode: Mode? = null
    var mode: Mode = defaultMode ?: allowedModes.first()
    var kelvin: Int
        get() = (kelvinRange.first + (sliderKel.slider.value * (kelvinRange.last - kelvinRange.first))).roundToInt()
        set(value) {
            sliderKel.slider.value = (value - kelvinRange.first).toDouble() / (kelvinRange.last - kelvinRange.first)
            color = Kelvin.toColor(value)
        }
    var value: Float
        get() = sliderVal.slider.value.toFloat()
        set(value) { sliderVal.slider.value = value.toDouble() }
    var color: Int
        get() = when (mode) {
            Mode.HSV ->
                FastColor.ARGB32.color(255, Color.HSBtoRGB(sliderHue.slider.value.toFloat(), sliderSat.slider.value.toFloat(), sliderVal.slider.value.toFloat()))
            Mode.Kelvin ->
                FastColor.ARGB32.lerp(value, CommonColors.BLACK, Kelvin.toColor(kelvin))
        }
        set(packed) {
            val hsb = Color3.fromPacked(packed).toHSB()
            sliderHue.slider.value = hsb.h.toDouble()
            sliderSat.slider.value = hsb.s.toDouble()
            sliderVal.slider.value = hsb.b.toDouble()
        }

    fun addSlider(text: String, default: Double): SliderData {
        val textComp = Component.literal(text.first().toString())
        val textWidth = Minecraft.getInstance().font.width(textComp)
        val title = StringWidget(0, 0, textWidth, sliderHeight - pad, textComp, Minecraft.getInstance().font)
        title.tooltip = Tooltip.create(Component.literal(text))
        val slider = SliderWidget(0, 0, width - (textWidth * 2) - (pad * 5), sliderHeight - pad, default) {}
        val data = SliderData(title, slider)
        sliders += data
        children += title
        children += slider
        return data
    }
    fun addMode(text: String, pickerMode: Mode): Button {
        val button = Button.builder(Component.literal(text.first().toString()))
            {
                prevMode = mode
                mode = pickerMode
                update()
            }
            .tooltip(Tooltip.create(Component.literal("Switch to $text")))
            .build()
        modeButtons[pickerMode] = button
        children += button
        return button
    }

    init {
        sliderHue = addSlider("Hue", 0.0)
        sliderSat = addSlider("Saturation", 1.0)
        sliderKel = addSlider("Kelvin", 0.5)
        sliderVal = addSlider("Value", 1.0)
        modeHsv = addMode("HSV", Mode.HSV)
        modeKel = addMode("Kelvin", Mode.Kelvin)
        color = defaultColor ?: defaultKelvin?.let { Kelvin.toColor(it) } ?: 0xffffff
        defaultKelvin?.let { kelvin = it }
        update()
    }

    fun update() {
        updateMode()
        updatePositions()
    }

    fun updatePositions() {
        var yPos = y + pad
        for ((string, slider, visible) in sliders) {
            if (!visible) continue
            string.setPosition(x + pad, yPos)
            slider.setPosition(x + pad + string.width + pad, yPos)
            yPos += slider.height + pad
        }

        yPos = y + pad
        for (button in modeButtons.values) {
            if (!visible) continue
            button.setPosition(x + (width - 10), yPos)
            button.setSize(10, sliderHeight - pad)
            yPos += button.height + pad
        }
    }

    fun updateMode() {
        for ((mode, button) in modeButtons) {
            button.visible = mode in allowedModes
        }
        sliderVal.visible = true
        when (mode) {
            Mode.HSV -> {
                modeHsv.active = false
                modeKel.active = true
                sliderHue.visible = true
                sliderSat.visible = true
                sliderKel.visible = false
                if (prevMode == Mode.Kelvin)
                    color = Kelvin.toColor(kelvin)
            }
            Mode.Kelvin -> {
                modeHsv.active = true
                modeKel.active = false
                sliderHue.visible = false
                sliderSat.visible = false
                sliderKel.visible = true
            }
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
        guiGraphics.fill(x, y, x + width, y + height, color)
        sliders.forEach { (string, slider, visible) ->
            if (!visible) return@forEach
            val font = Minecraft.getInstance().font
            val isKelvin = slider == sliderKel.slider
            if (isKelvin && allowedModes.size > 1)
                guiGraphics.drawString(font, "* Can't convert back from HSV", slider.x - 5, slider.y + 3 + (slider.height * 2) + pad, 0xFFFFFF)
            string.render(guiGraphics, mouseX, mouseY, partialTick)
            slider.render(guiGraphics, mouseX, mouseY, partialTick)
            if (slider.isHovered) {
                val value: String = if (isKelvin) "$kelvin K" else slider.value.formatDecimal()
                guiGraphics.renderTooltip(font, Component.literal(value), mouseX, mouseY)
            }
        }
        if (modeButtons.count { it.value.visible } > 1) modeButtons.values.forEach { button ->
            button.render(guiGraphics, mouseX, mouseY, partialTick)
        }
    }

    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {

    }

    override fun children() = children
}