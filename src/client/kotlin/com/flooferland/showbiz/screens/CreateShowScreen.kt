package com.flooferland.showbiz.screens

import net.minecraft.*
import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.screens.*
import net.minecraft.client.resources.sounds.*
import net.minecraft.network.chat.*
import net.minecraft.sounds.*
import com.flooferland.showbiz.FileServer
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.network.packets.ShowFileEditPacket
import com.flooferland.showbiz.screens.widgets.FileUploadButton
import com.flooferland.showbiz.types.BitChartStore
import com.flooferland.showbiz.utils.Extensions.alwaysEndsWith
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import org.lwjgl.glfw.GLFW

class CreateShowScreen(val parent: Screen? = null) : Screen(Component.literal("Create a show")) {
    lateinit var showName: EditBox
    lateinit var submitButton: Button
    lateinit var uploadAudioButton: FileUploadButton

    val editHint = Component.literal("File name").withStyle(ChatFormatting.DARK_GRAY)!!
    val chartButtons = mutableListOf<Button>()
    var selectedFormat = BitChartStore.DEFAULT

    override fun isPauseScreen() = false
    override fun shouldCloseOnEsc() = !uploadAudioButton.processing

    override fun init() {
        chartButtons.clear()
        clearWidgets()

        // File name
        showName = EditBox(font, (width * 0.6).toInt(), 20, Component.literal("Show name"))
        showName.setPosition(width / 2 - showName.width / 2, 50)
        showName.setHint(editHint)
        showName.setFormatter { string, _ ->
            if (string.contains('.') || showName.cursorPosition != showName.value.length) return@setFormatter Component.literal(string).visualOrderText
            Component.literal(string).append(Component.literal(".${Showbiz.charts.idsToInfo[selectedFormat]?.extension}").withStyle(ChatFormatting.DARK_GRAY)).visualOrderText
        }
        showName.setResponder { string ->
            for ((ext, id) in Showbiz.charts.extensionToId) {
                if (string.endsWith(".$ext")) {
                    if (selectedFormat != id) {
                        Minecraft.getInstance().soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f))
                    }
                    selectedFormat = id
                    chartButtons.forEach { it.active = (it.message.string != selectedFormat) }
                    break
                }
            }
            updateSubmitActive()
        }
        addRenderableWidget(showName)
        setInitialFocus(showName)

        // ID list
        for (chartId in Showbiz.charts.ids) {
            val button = Button.builder(Component.literal(chartId))
                { b ->
                    selectedFormat = chartId
                    chartButtons.forEach { it.active = (it.message.string != selectedFormat) }
                    updateWidgets()
                }
                .size(font.width(" $chartId "), 20)
                .tooltip(Tooltip.create(Component.literal("The ${Showbiz.charts.idsToInfo[chartId]?.extension} file format")))
                .build()
            chartButtons += button
            addRenderableWidget(button)
        }
        var xOffset = 0
        val buttonWidthTotal = chartButtons.sumOf { it.width }
        for (button in chartButtons) {
            button.active = (button.message.string != selectedFormat)
            button.setPosition(((width / 2) + xOffset) - (buttonWidthTotal / 2), showName.bottom + 5)
            xOffset += button.width
        }

        // Upload audio button
        uploadAudioButton = FileUploadButton(showName.x, showName.bottom + 50, filter = FileUploadButton.Filter.AudioWav)
        uploadAudioButton.onSubmit = { path ->
            updateSubmitActive()
        }
        uploadAudioButton.onUploadFinish = {
            updateSubmitActive()
        }
        addRenderableWidget(uploadAudioButton)

        // Submit button
        submitButton = Button.Builder(Component.literal("Create")) {
                val ext = Showbiz.charts.idsToInfo[selectedFormat]?.extension ?: return@Builder
                val file = showName.value.alwaysEndsWith(".$ext")
                ClientPlayNetworking.send(ShowFileEditPacket(file, FileServer.FileAction.Create))
                minecraft?.setScreen(null)
            }
            .size(70, 20)
            .tooltip(Tooltip.create(Component.literal("Create a new file")))
            .pos(showName.x, (height * 0.9).toInt())
            .build()
        submitButton.active = false
        addRenderableWidget(submitButton)

        updateWidgets()
    }

    fun updateSubmitActive() {
        submitButton.active = showName.value.isNotBlank() && uploadAudioButton.isUploadFinished
    }

    fun updateWidgets() {
        val ext = Showbiz.charts.idsToInfo[selectedFormat]?.extension
        showName.setHint(ext?.let { editHint.copy().append(".$it") } ?: editHint)
    }

    override fun onClose() {
        if (parent != null) minecraft?.setScreen(parent)
        else super.onClose()
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            if (focused == showName) {
                if (submitButton.active) {
                    submitButton.onPress()
                } else {
                    Minecraft.getInstance().soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.8f))
                }
                return true
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        guiGraphics.drawCenteredString(font, title, (width / 2), 20, 0xFFFFFFFF.toInt())
    }
}