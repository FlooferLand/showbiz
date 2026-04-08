package com.flooferland.showbiz.screens

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

class BotSelectScreen(val selectMenu: BotSelectMenu, inventory: Inventory, title: Component) : Screen(title), MenuAccess<BotSelectMenu> {
    var bots = mutableMapOf<ResourceId, AddonBotEntry>()
    var loading = true

    override fun getMenu() = selectMenu
    override fun isPauseScreen() = false

    override fun init() {
        ClientPlayNetworking.send(BotListPacket())
        refresh()
    }

    fun refresh() {
        clearWidgets()
        val maxWidth = (width * 0.6).toInt()
        val maxHeight = (height * 0.6).toInt()

        // Bot listing
        val botListWidget = BotListWidget(
            (width - maxWidth) / 2, (height - maxHeight) / 2, maxWidth, maxHeight
        )
        botListWidget.setBots(bots) { bot ->
            ClientPlayNetworking.send(BotListSelectPacket(selectMenu.data.blockPos, bot))
            Minecraft.getInstance().setScreen(null)
        }
        addRenderableWidget(botListWidget)

        // Title
        addRenderableWidget(
            StringWidget(botListWidget.x, botListWidget.y - 20, botListWidget.width, 20, Component.literal("Select a bot"), font).alignCenter()
        )
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