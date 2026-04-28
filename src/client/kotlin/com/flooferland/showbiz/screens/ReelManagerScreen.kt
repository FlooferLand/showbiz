package com.flooferland.showbiz.screens

import net.minecraft.ChatFormatting
import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.screens.*
import net.minecraft.network.chat.*
import net.minecraft.world.item.*
import com.flooferland.showbiz.FileStorage
import com.flooferland.showbiz.network.packets.ShowFileListPacket
import com.flooferland.showbiz.network.packets.ShowFileSelectPacket
import com.flooferland.showbiz.screens.widgets.ShowFileListWidget
import com.flooferland.showbiz.utils.PlatformUtils
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import kotlin.io.path.pathString

class ReelManagerScreen(val reelStack: ItemStack) : Screen(Component.literal("Reel Manager")) {
    var files = mutableListOf<String>()
    var loading = true
    var authorized = false

    override fun isPauseScreen() = false

    override fun init() {
        ClientPlayNetworking.send(ShowFileListPacket())
        refresh()
    }

    fun refresh() {
        clearWidgets()
        val maxWidth = (width * 0.7).toInt()
        val maxHeight = (height * 0.7).toInt()
        val openDirButton =
            Button.builder(Component.literal("Open file manager")) {
                PlatformUtils.openFileManager(FileStorage.SHOWS_DIR)
            }.size(100, 20).build()

        // No files prompt
        if (files.isEmpty()) {
            addRenderableWidget(
                StringWidget(width / 2 - 50, height / 2 - 50, 100, 20, Component.literal("No shows found"), font)
            )
            run {
                val message = Component.literal("Upload an ${FileStorage.SUPPORTED_FORMATS.joinToString("/")} show file to your ")
                if (!isLocalServer()) message.append("server's ")
                message.append(Component.literal(FileStorage.SHOWS_DIR.pathString).withStyle(ChatFormatting.BOLD))
                addRenderableWidget(
                    MultiLineTextWidget(
                        (width - maxWidth) / 2,
                        height / 2 - 20,
                        message,
                        font
                    ).setCentered(true).setMaxWidth(maxWidth).also { it.setPosition((width - it.width) / 2, height / 2 - 20) }
                )
            }
            openDirButton.setPosition(width / 2 - 50, height / 2 + 20)
            if (isLocalServer()) addRenderableWidget(openDirButton)
            return
        }

        // File listing
        val fileListWidget = ShowFileListWidget(
            (width - maxWidth) / 2, (height - maxHeight) / 2, maxWidth, maxHeight
        )
        fileListWidget.setFiles(files) { file ->
            ClientPlayNetworking.send(ShowFileSelectPacket(file))
            Minecraft.getInstance().setScreen(null)
        }
        addRenderableWidget(fileListWidget)

        // Additional
        addRenderableWidget(
            StringWidget(fileListWidget.x, fileListWidget.y - 20, fileListWidget.width, 20, Component.literal("Upload a show to this reel"), font).alignCenter()
        )
        openDirButton.setPosition(width / 2 - 50, fileListWidget.bottom + 10)
        if (isLocalServer()) addRenderableWidget(openDirButton)
    }

    fun isLocalServer() = Minecraft.getInstance().isLocalServer

    fun updateFiles(paths: Array<String>) {
        loading = false
        files.clear()
        files.addAll(paths)
        refresh()
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        if (!loading) {
            super.render(guiGraphics, mouseX, mouseY, partialTick)
        } else {
            guiGraphics.drawCenteredString(font, "Loading..", width / 2, height / 2, 0xFFFFFF)
        }
    }

    companion object {
        init {
            ClientPlayNetworking.registerGlobalReceiver(ShowFileListPacket.type) { packet, _ ->
                val screen = (Minecraft.getInstance().screen as? ReelManagerScreen) ?: return@registerGlobalReceiver
                screen.authorized = packet.playerAuthorized
                screen.updateFiles(packet.files)
            }
        }
    }
}