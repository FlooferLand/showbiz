package com.flooferland.showbiz.screens

import net.minecraft.ChatFormatting
import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.screens.*
import net.minecraft.client.gui.screens.inventory.MenuAccess
import net.minecraft.network.chat.*
import net.minecraft.world.entity.player.Inventory
import com.flooferland.showbiz.addons.data.AddonBotEntry
import com.flooferland.showbiz.menus.BotSelectMenu
import com.flooferland.showbiz.network.packets.BotListPacket
import com.flooferland.showbiz.network.packets.BotListSelectPacket
import com.flooferland.showbiz.screens.widgets.BotListWidget
import com.flooferland.showbiz.types.ResourceId
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import org.lwjgl.glfw.GLFW
import kotlin.collections.filter

class BotSelectScreen(val selectMenu: BotSelectMenu, inventory: Inventory, title: Component) : Screen(title), MenuAccess<BotSelectMenu> {
    var bots = mutableMapOf<ResourceId, AddonBotEntry>()
    var loading = true

    override fun getMenu() = selectMenu
    override fun isPauseScreen() = false

    var searchText: String = ""

    override fun init() {
        ClientPlayNetworking.send(BotListPacket())
        refresh()
    }

    fun botFilter(bot: Map.Entry<ResourceId, AddonBotEntry>) =
        bot.value.name.contains(searchText)

    fun botSelected(botId: ResourceId) {
        ClientPlayNetworking.send(BotListSelectPacket(selectMenu.data.blockPos, botId))
        Minecraft.getInstance().setScreen(null)
    }

    fun refresh() {
        clearWidgets()
        val maxWidth = (width * 0.8).toInt()
        val maxHeight = (height * 0.8).toInt()

        // Bot listing
        val botListWidget = BotListWidget(
            (width - maxWidth) / 2, (height - maxHeight) / 2, maxWidth, maxHeight
        )
        botListWidget.setBots(bots) { botSelected(it) }
        addRenderableWidget(botListWidget)

        // Title
        addRenderableWidget(
            StringWidget(botListWidget.x, botListWidget.y - 20, botListWidget.width, 20, Component.literal("Select a bot"), font).alignCenter()
        )

        // Search bar
        val searchBar = EditBox(font, botListWidget.x, botListWidget.y + botListWidget.height, botListWidget.width, 20, Component.literal("Search") )
        searchBar.setHint(Component.literal("Search..").withStyle(ChatFormatting.DARK_GRAY))
        searchBar.setResponder { name ->
            searchText = name;
            botListWidget.setBots(bots.filter { botFilter(it) }) { botSelected(it) }
        }
        searchBar.isFocused = true
        searchBar.setCanLoseFocus(true)
        addRenderableWidget(searchBar)
        this.focused = searchBar
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == GLFW.GLFW_KEY_ENTER && this.focused is EditBox) {
            val handled = bots.filter({ botFilter(it) }).keys.firstOrNull()?.let { botSelected(it); true } ?: false
            return handled
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
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
            ClientPlayNetworking.registerGlobalReceiver(BotListPacket.type) { packet, _ ->
                val screen = (Minecraft.getInstance().screen as? BotSelectScreen) ?: return@registerGlobalReceiver
                screen.updateBots(packet.bots)
            }
        }
    }
}