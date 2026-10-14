package com.flooferland.showbiz.screens

import net.minecraft.client.gui.components.*
import net.minecraft.network.chat.*
import net.minecraft.world.entity.player.*
import com.flooferland.showbiz.menus.ShowBridgeEditMenu
import com.flooferland.showbiz.network.packets.editscreen.ShowBridgeEditPacket
import com.flooferland.showbiz.screens.base.EditScreen
import com.flooferland.showbiz.utils.rl

class ShowBridgeEditScreen(editMenu: ShowBridgeEditMenu, inventory: Inventory, title: Component) : EditScreen<ShowBridgeEditMenu, ShowBridgeEditPacket>(editMenu, inventory, title) {
    override val background = rl("textures/gui/show_bridge.png")

    enum class Profile(var button: Button? = null) { Player, Server }
    var selectedProfile: Profile = Profile.Player

    override fun buildUi() {
        // TODO: Fix the entire layout system for EditScreen its killing me
        val x = (textureX + 80)
        val y = (textureY.coerceAtLeast(0) + 30)

        // Profiles
        run {
            var profileX = 0
            val name = run {
                val text = Component.literal("Profiles")
                StringWidget(x, y, font.width(text), 20, text, font).alignLeft()
            }
            addRenderableWidget(name)
            profileX += name.width

            for (profile in Profile.entries) {
                val button =
                    Button.builder(Component.literal(profile.name))
                        {
                            selectedProfile = profile
                            for (p in Profile.entries)
                                p.button?.active = p != selectedProfile
                        }
                        .pos(x + profileX, y)
                        .size(80, 20)
                        .build()
                button.active = selectedProfile != profile
                addRenderableWidget(button)
                profile.button = button
                profileX += button.width + 4
            }
        }
    }

    override fun saveCustom(data: ShowBridgeEditPacket) {
        // TODO: Set data
    }
}