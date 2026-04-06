package com.flooferland.showbiz.screens

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.MenuAccess
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import com.flooferland.showbiz.blocks.entities.BitViewBlockEntity
import com.flooferland.showbiz.menus.BitViewMenu
import com.flooferland.showbiz.screens.widgets.BitListWidget

class BitViewScreen(val bitViewMenu: BitViewMenu, inventory: Inventory, title: Component) : Screen(title), MenuAccess<BitViewMenu> {
    var blockEntity: BitViewBlockEntity? = null
    var bitList: BitListWidget? = null

    val maxWidth get() = (width * 0.8).toInt()
    val maxHeight get() = (height * 0.8).toInt()

    override fun getMenu() = bitViewMenu
    override fun isPauseScreen() = false

    override fun init() {
        blockEntity = Minecraft.getInstance().level?.getBlockEntity(bitViewMenu.data.blockPos) as? BitViewBlockEntity
        bitList = BitListWidget(
            (width - maxWidth) / 2, (height - maxHeight) / 2, maxWidth, maxHeight
        )
        addRenderableWidget(bitList!!)
        updateUi()
    }

    fun updateUi() {
        bitList?.frame = blockEntity?.show?.data
    }

    override fun tick() {
        updateUi()
    }
}