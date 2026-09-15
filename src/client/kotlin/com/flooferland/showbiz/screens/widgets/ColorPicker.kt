package com.flooferland.showbiz.screens.widgets

import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.narration.*
import net.minecraft.network.chat.*
import net.minecraft.util.*
import com.flooferland.showbiz.utils.Extensions.formatDecimal
import java.awt.Color
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

class ColorPicker(x: Int, y: Int, width: Int, height: Int, defaultColor: Int = 0xffffff, defaultMode: ColorPickerMode = ColorPickerMode.HSV) : AbstractContainerWidget(x, y, width, height, Component.empty()) {
    val pad get() = 2
    val sliderHeight get() = height / 3
    val kelvinRange get() = 1500..15000

    data class SliderData(val string: StringWidget, val slider: SliderWidget) {
        var visible: Boolean
            get() = string.visible && slider.visible
            set(value) {
                string.visible = value
                slider.visible = value
            }
        operator fun component3() = visible
    }
    enum class ColorPickerMode { HSV, Kelvin }

    var sliderHue: SliderData
    var sliderSat: SliderData
    var sliderVal: SliderData
    var sliderKel: SliderData
    var modeHsv: Button
    var modeKel: Button
    val sliders = mutableListOf<SliderData>()
    val modeButtons = mutableListOf<Button>()
    val children = mutableListOf<AbstractWidget>()

    var prevMode: ColorPickerMode? = null
    var mode: ColorPickerMode = defaultMode
    var value: Int
        get() = when (mode) {
            ColorPickerMode.HSV ->
                FastColor.ARGB32.color(255, Color.HSBtoRGB(sliderHue.slider.value.toFloat(), sliderSat.slider.value.toFloat(), sliderVal.slider.value.toFloat()))
            ColorPickerMode.Kelvin ->
                kelvinToColor(getKelvin(sliderKel.slider.value))
        }
        set(rgb) {
            val hsb = Color.RGBtoHSB(FastColor.ARGB32.red(rgb), FastColor.ARGB32.green(rgb), FastColor.ARGB32.blue(rgb), null)
            sliderHue.slider.value = hsb[0].toDouble()
            sliderSat.slider.value = hsb[1].toDouble()
            sliderVal.slider.value = hsb[2].toDouble()
        }

    fun addSlider(text: String, default: Double): SliderData {
        val textComp = Component.literal(text)
        val textWidth = Minecraft.getInstance().font.width(textComp)
        val title = StringWidget(0, 0, textWidth, sliderHeight - pad, textComp, Minecraft.getInstance().font)
        val slider = SliderWidget(0, 0, width - (textWidth * 2) - (pad * 5), sliderHeight - pad, default) {}
        val data = SliderData(title, slider)
        sliders += data
        children += title
        children += slider
        return data
    }
    fun addMode(text: String, pickerMode: ColorPickerMode): Button {
        val button = Button.builder(Component.literal(text.first().toString()))
            {
                prevMode = mode
                mode = pickerMode
                update()
            }
            .tooltip(Tooltip.create(Component.literal("Switch to $text")))
            .build()
        modeButtons.add(button)
        children += button
        return button
    }

    init {
        sliderHue = addSlider("H", 0.0)
        sliderSat = addSlider("S", 1.0)
        sliderVal = addSlider("V", 1.0)
        sliderKel = addSlider("K", 0.5)
        modeHsv = addMode("HSV", ColorPickerMode.HSV)
        modeKel = addMode("Kelvin", ColorPickerMode.Kelvin)
        update()
        value = defaultColor
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
        for (button in modeButtons) {
            if (!visible) continue
            button.setPosition(x + (width - 10), yPos)
            button.setSize(10, sliderHeight - pad)
            yPos += button.height + pad
        }
    }

    fun updateMode() {
        when (mode) {
            ColorPickerMode.HSV -> {
                modeHsv.active = false
                modeKel.active = true
                sliderHue.visible = true
                sliderSat.visible = true
                sliderVal.visible = true
                sliderKel.visible = false
                if (prevMode == ColorPickerMode.Kelvin)
                    value = kelvinToColor(getKelvin(sliderKel.slider.value))
            }
            ColorPickerMode.Kelvin -> {
                modeHsv.active = true
                modeKel.active = false
                sliderHue.visible = false
                sliderSat.visible = false
                sliderVal.visible = false
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
        guiGraphics.fill(x, y, x + width, y + height, value)
        sliders.forEach { (string, slider, visible) ->
            if (!visible) return@forEach
            val font = Minecraft.getInstance().font
            val isKelvin = slider == sliderKel.slider
            if (isKelvin) {
                guiGraphics.drawString(font, "Note that HSV can't be", slider.x, slider.y + slider.height + pad, 0xFFFFFF)
                guiGraphics.drawString(font, "converted back to Kelvin", slider.x, slider.y + slider.height + pad + font.lineHeight, 0xFFFFFF)
            }
            string.render(guiGraphics, mouseX, mouseY, partialTick)
            slider.render(guiGraphics, mouseX, mouseY, partialTick)
            if (slider.isHovered) {
                val value: String = if (isKelvin) "${getKelvin(slider.value)} K" else slider.value.formatDecimal()
                guiGraphics.renderTooltip(font, Component.literal(value), mouseX, mouseY)
            }
        }
        modeButtons.forEach { button ->
            button.render(guiGraphics, mouseX, mouseY, partialTick)
        }
    }

    /** Kelvin value from a 0 to 1 input */
    fun getKelvin(value: Double): Int =
        (kelvinRange.first + (value * (kelvinRange.last - kelvinRange.first))).roundToInt()

    /** Thanks to https://tannerhelland.com/2012/09/18/convert-temperature-rgb-algorithm-code.html */
    fun kelvinToColor(kelvin: Int): Int {
        val temperature = kelvin / 100.0

        val red = if (temperature <= 66.0) {
            255.0
        } else {
            329.698727446 * (temperature - 60.0).pow(-0.1332047592)
        }.coerceIn(0.0, 255.0)

        val green = if (temperature <= 66.0) {
            99.4708025861 * ln(temperature) - 161.1195681661
        } else {
            288.1221695283 * (temperature - 60.0).pow(-0.0755148492)
        }.coerceIn(0.0, 255.0)

        val blue = when {
            temperature >= 66.0 -> 255.0
            temperature <= 19.0 -> 0.0
            else -> 138.5177312231 * ln(temperature - 10.0) - 305.0447927307
        }.coerceIn(0.0, 255.0)

        return FastColor.ARGB32.color(red.roundToInt(), green.roundToInt(), blue.roundToInt())
    }

    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {

    }

    override fun children() = children
}