package com.flooferland.showbiz.screens

import net.minecraft.client.*
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.screens.*
import net.minecraft.client.gui.screens.inventory.*
import net.minecraft.network.chat.*
import net.minecraft.world.entity.player.*
import com.flooferland.showbiz.blocks.entities.BitViewBlockEntity
import com.flooferland.showbiz.menus.BitViewMenu
import com.flooferland.showbiz.screens.widgets.BitListWidget

class BitViewScreen(val bitViewMenu: BitViewMenu, inventory: Inventory, title: Component) : Screen(title), MenuAccess<BitViewMenu> {
    var blockEntity: BitViewBlockEntity? = null

    lateinit var bitList: BitListWidget
    lateinit var bitIdCheckbox: Checkbox

    val maxWidth get() = (width * 0.95).toInt()
    val maxHeight get() = (height * 0.9).toInt()

    var lastMapping: String? = null

    override fun getMenu() = bitViewMenu
    override fun isPauseScreen() = false

    override fun init() {
        blockEntity = Minecraft.getInstance().level?.getBlockEntity(bitViewMenu.data.blockPos) as? BitViewBlockEntity

        bitList = BitListWidget(
            (width - maxWidth) / 2, (height - maxHeight) / 2, maxWidth, maxHeight
        )
        lastMapping = blockEntity?.show?.data?.mapping
        addRenderableWidget(bitList)

        bitIdCheckbox = Checkbox.builder(Component.literal("Show bit IDs"), font)
            .onValueChange { _, value -> bitList.showBitIds = value }
            .selected(bitList.showBitIds)
            .build()
        bitIdCheckbox.setPosition(4, height - bitIdCheckbox.height - 4)
        addRenderableWidget(bitIdCheckbox)

        updateUi()
    }

    fun updateUi() {
        val mapping = blockEntity?.show?.data?.mapping
        if (lastMapping != mapping) {
            bitList.updateBitInfo()
            lastMapping = mapping
        }
        bitList.frame = blockEntity?.show?.data
    }

    override fun tick() {
        updateUi()
    }
}