package com.flooferland.showbiz.screens.widgets

import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.narration.*
import net.minecraft.network.chat.*
import net.minecraft.util.*
import net.minecraft.world.level.levelgen.SurfaceRules.state
import com.flooferland.showbiz.utils.Extensions.formatDecimal
import java.awt.Color

class ColorPicker(x: Int, y: Int, width: Int, height: Int, defaultColor: Int = 0xffffff) : AbstractContainerWidget(x, y, width, height, Component.empty()) {
    val pad get() = 2
    val sliderHeight get() = height / 3

    data class SliderData(val string: StringWidget, val slider: SliderWidget)

    var hslider: SliderData
    var sslider: SliderData
    var vslider: SliderData
    val children = mutableListOf<AbstractWidget>()

    var value: Int
        get() = FastColor.ARGB32.color(255, Color.HSBtoRGB(hslider.slider.value.toFloat(), sslider.slider.value.toFloat(), vslider.slider.value.toFloat()))
        set(rgb) {
            val hsb = Color.RGBtoHSB(FastColor.ARGB32.red(rgb), FastColor.ARGB32.green(rgb), FastColor.ARGB32.blue(rgb), null)
            println("${hsb[0]}, ${hsb[1]}, ${hsb[2]}")
            hslider.slider.value = hsb[0].toDouble()
            sslider.slider.value = hsb[1].toDouble()
            vslider.slider.value = hsb[2].toDouble()
        }

    fun addSlider(text: String, default: Double): SliderData {
        val textComp = Component.literal(text)
        val textWidth = Minecraft.getInstance().font.width(textComp)
        val title = StringWidget(0, 0, textWidth, sliderHeight - pad, textComp, Minecraft.getInstance().font)
        val slider = SliderWidget(0, 0, width - (textWidth * 2) - pad, sliderHeight - pad, default) {}
        children += title
        children += slider
        return SliderData(title, slider)
    }

    init {
        hslider = addSlider("H", 0.0)
        sslider = addSlider("S", 1.0)
        vslider = addSlider("V", 1.0)
        updatePositions()
        value = defaultColor
    }

    fun updatePositions() {
        var yPos = y + pad
        for ((string, slider) in arrayOf(hslider, sslider, vslider)) {
            string.setPosition(x + pad, yPos)
            slider.setPosition(x + pad + string.width, yPos)
            yPos += slider.height + 2
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
        children.forEach { child ->
            child.render(guiGraphics, mouseX, mouseY, partialTick)
            if (child is SliderWidget && child.isHovered) {
                guiGraphics.renderTooltip(Minecraft.getInstance().font, Component.literal(child.value.formatDecimal()), mouseX, mouseY)
                child.value
            }
        }
    }

    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {

    }

    override fun children() = children
}