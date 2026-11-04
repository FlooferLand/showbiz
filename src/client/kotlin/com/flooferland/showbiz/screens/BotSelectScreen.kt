package com.flooferland.showbiz.screens

import com.flooferland.showbiz.ClientPackets
import com.flooferland.showbiz.addons.data.AddonBotEntry
import com.flooferland.showbiz.menus.BotSelectMenu
import com.flooferland.showbiz.network.packets.BotListPacket
import com.flooferland.showbiz.network.packets.BotListSelectPacket
import com.flooferland.showbiz.screens.widgets.BotListWidget
import com.flooferland.showbiz.screens.widgets.BotPreviewWidget
import com.flooferland.showbiz.types.ResourceId
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.MenuAccess
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import org.lwjgl.glfw.GLFW

class BotSelectScreen(val selectMenu: BotSelectMenu, inventory: Inventory, title: Component) : Screen(title), MenuAccess<BotSelectMenu> {
    var bots = mutableMapOf<ResourceId, AddonBotEntry>()
    var loading = true

    override fun getMenu() = selectMenu
    override fun isPauseScreen() = false

    var selectedBot = menu.data.botId
    var selectedVariant = menu.data.botVariant
    var searchText: String = ""

    override fun init() {
        ClientPlayNetworking.send(BotListPacket())
        refresh()
    }

    fun botFilter(bot: Map.Entry<ResourceId, AddonBotEntry>) =
        bot.value.name.contains(searchText, ignoreCase = true)

    fun refresh() {
        clearWidgets()
        val searchBarHeight = 20
        val maxWidth = (width * 0.9).toInt()
        val maxHeight = (height * 0.9).toInt() - searchBarHeight

        // Bot preview
        val botPreview = BotPreviewWidget((width - maxWidth) / 2, (height - maxHeight) / 2, 150, 150)
        botPreview.botId = selectedBot
        botPreview.botVariant = selectedVariant
        botPreview.bots = bots
        addRenderableWidget(botPreview)

        // Bot listing
        val botListWidget = BotListWidget(
            botPreview.right + 4, (height - maxHeight) / 2, maxWidth - botPreview.width, maxHeight
        )
        botListWidget.selectedBot = selectedBot
        botListWidget.selectedVariant = selectedVariant
        botListWidget.onHover = { if (it != null) botPreview.botId = it }
        botListWidget.onChange = { botId, variant ->
            selectedBot = botId
            selectedVariant = variant
        }
        botListWidget.setBots(bots)
        addRenderableWidget(botListWidget)

        // Title
        addRenderableWidget(
            StringWidget(
                botListWidget.x,
                botListWidget.y - 20,
                botListWidget.width,
                20,
                Component.literal("Select a bot"),
                font
            ).alignCenter()
        )

        // Search bar
        val searchBar = EditBox(font, botListWidget.x, botListWidget.y + botListWidget.height, botListWidget.width, searchBarHeight, Component.literal("Search") )
        searchBar.setHint(Component.literal("Search..").withStyle(ChatFormatting.DARK_GRAY))
        searchBar.setResponder { name ->
            searchText = name.trim()
            botListWidget.showExcluded =
                botListWidget.excluded.any { searchText == it.toString() || searchText == it.path }

            if (searchText.isBlank()) {
                botListWidget.visibleCategories = mutableSetOf()
                botPreview.bots = emptyMap()
            } else {
                val filtered = bots.filter { botFilter(it) }
                botListWidget.visibleCategories = filtered.keys.map { it.namespace }.toMutableSet()
                botPreview.bots = filtered
            }
            botListWidget.setBots(if (searchText.isBlank()) bots else bots.filter { botFilter(it) })
        }
        addRenderableWidget(searchBar)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == GLFW.GLFW_KEY_ENTER && this.focused is EditBox) {
            val handled = bots.filter { botFilter(it) }.keys.firstOrNull()?.let { selectedBot = it; closeScreen(); true } ?: false
            return handled
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun onClose() {
        ClientPlayNetworking.send(BotListSelectPacket(selectMenu.data.bot, selectedBot, selectedVariant))
        super.onClose()
    }

    fun closeScreen() {
        Minecraft.getInstance().setScreen(null)
    }

    fun updateBots(elements: Map<ResourceId, AddonBotEntry>) {
        loading = false
        bots.clear()
        bots.putAll(elements)
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
            // BotListPacket response
            ClientPackets.listen(BotListPacket.type) { packet, _ ->
                val screen = (Minecraft.getInstance().screen as? BotSelectScreen) ?: return@listen
                screen.updateBots(packet.bots)
            }
        }
    }
}