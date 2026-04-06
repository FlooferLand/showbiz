package com.flooferland.showbiz.menus

import net.minecraft.world.entity.player.*
import net.minecraft.world.inventory.*
import net.minecraft.world.item.*
import com.flooferland.showbiz.network.packets.BitViewPacket
import com.flooferland.showbiz.network.packets.BotListSelectPacket
import com.flooferland.showbiz.registry.ModScreenHandlers
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory

class BitViewMenu(containerId: Int, val data: BitViewPacket) : AbstractContainerMenu(ModScreenHandlers.BitView.type, containerId) {
    override fun quickMoveStack(player: Player, index: Int) = ItemStack.EMPTY!!
    override fun stillValid(player: Player) = true
}