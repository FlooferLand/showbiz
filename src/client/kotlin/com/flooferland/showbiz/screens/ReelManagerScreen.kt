package com.flooferland.showbiz.screens

import net.minecraft.*
import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.screens.*
import net.minecraft.network.chat.*
import net.minecraft.world.item.*
import com.flooferland.showbiz.ClientPackets
import com.flooferland.showbiz.FileStorage
import com.flooferland.showbiz.items.ReelItem
import com.flooferland.showbiz.network.packets.ShowFileListPacket
import com.flooferland.showbiz.network.packets.ShowFileSelectPacket
import com.flooferland.showbiz.screens.widgets.ShowFileListWidget
import com.flooferland.showbiz.types.ShowFileInfo
import com.flooferland.showbiz.utils.ShowbizEnv
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import kotlin.io.path.pathString

class ReelManagerScreen(val reelStack: ItemStack) : Screen(Component.literal("Reel Manager")) {
    var files = mutableSetOf<ShowFileInfo>()
    var loading = true
    var authorized = false

    val bottomBarButtons = mutableListOf<Button>()

    lateinit var fileListWidget: ShowFileListWidget

    override fun isPauseScreen() = false

    override fun init() {
        ClientPlayNetworking.send(ShowFileListPacket())
        refresh()
    }

    fun onFileSelected(file: ShowFileInfo) {
        ClientPlayNetworking.send(ShowFileSelectPacket(file))
        Minecraft.getInstance().setScreen(null)
    }

    fun refresh() {
        clearWidgets()
        val maxWidth = (width * 0.8).toInt()
        val maxHeight = (height * 0.8).toInt()

        val filename = ReelItem.getFilename(reelStack)

        // File listing
        fileListWidget = ShowFileListWidget(
            (width - maxWidth) / 2, (height - maxHeight) / 2, maxWidth, maxHeight
        )
        fileListWidget.setFiles(files, ::onFileSelected)
        filename?.let { fileListWidget.setSelected(it) }
        addRenderableWidget(fileListWidget)

        // Additional
        updateBottomBar(fileListWidget.x + 4, fileListWidget.y + fileListWidget.height + 4)
        addRenderableWidget(
            StringWidget(fileListWidget.x, fileListWidget.y - 20, fileListWidget.width, 20, Component.literal("Reel Manager"), font).alignCenter()
        )

        // No files prompt
        if (files.isEmpty()) {
            addRenderableWidget(
                StringWidget(width / 2 - 50, height / 2 - 50, 100, 20, Component.literal("No shows found"), font)
            )
            run {
                val message = Component.literal("Upload or create a show file in your ")
                if (!isLocalServer()) message.append("server's ")
                message.append(Component.literal(FileStorage.SHOWS_DIR.pathString).withStyle(ChatFormatting.BOLD))
                message.append(Component.literal(" folder"))
                addRenderableWidget(
                    MultiLineTextWidget(
                        (width - maxWidth) / 2,
                        height / 2 - 20,
                        message,
                        font
                    ).setCentered(true).setMaxWidth(maxWidth).also { it.setPosition((width - it.width) / 2, height / 2 - 20) }
                )
            }
            updateBottomBar(width / 2, height / 2 + 20, center = true)
            return
        }
    }

    fun updateBottomBar(x: Int, y: Int, center: Boolean = false) {
        bottomBarButtons.removeIf { removeWidget(it); true }
        var offsetX = 0

        if (isLocalServer()) {
            val openDirButton =
                Button.builder(Component.literal("Open file manager")) {
                    ShowbizEnv.openFileManager(FileStorage.SHOWS_DIR)
                }.pos(x, y).size(100, 20).build()
            bottomBarButtons.add(openDirButton)
            addRenderableWidget(openDirButton)
            offsetX = openDirButton.right
        }
        if (authorized) {
            val createShowButton =
                Button.builder(Component.literal("Create a show")) {
                    Minecraft.getInstance().setScreen(CreateShowScreen(this))
                }.pos(offsetX, y).size(100, 20).build()
            bottomBarButtons.add(createShowButton)
            addRenderableWidget(createShowButton)
            offsetX = createShowButton.right
        }

        // Search bar
        val searchBar = EditBox(font, offsetX, y, width - offsetX - 70, 20, Component.literal("Search") )
        searchBar.setHint(Component.literal("Search..").withStyle(ChatFormatting.DARK_GRAY))
        searchBar.setSuggestion("Search..")
        searchBar.setResponder { name ->
            if (name.isBlank()) {
                fileListWidget.setFiles(files, ::onFileSelected)
                searchBar.setSuggestion("Search..")
                return@setResponder
            }
            searchBar.setSuggestion(null)

            val files = files.filter { it.id.contains(name, ignoreCase = true) }
            fileListWidget.setFiles(files, ::onFileSelected)
        }
        searchBar.isFocused = true
        searchBar.setCanLoseFocus(true)
        addRenderableWidget(searchBar)
        this.focused = searchBar

        if (center) updateBottomBar(x - (offsetX / 2), y)
    }

    fun isLocalServer() = Minecraft.getInstance().isLocalServer

    fun updateFiles(files: Set<ShowFileInfo>) {
        loading = false
        this.files.clear()
        this.files.addAll(files)
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
            ClientPackets.listen(ShowFileListPacket.type) { packet, _ ->
                val screen = (Minecraft.getInstance().screen as? ReelManagerScreen) ?: return@listen
                screen.authorized = packet.playerAuthorized
                screen.updateFiles(packet.files)
            }
        }
    }
}