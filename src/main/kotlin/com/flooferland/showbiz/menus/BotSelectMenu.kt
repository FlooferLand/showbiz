package com.flooferland.showbiz.menus

import net.minecraft.world.entity.player.*
import net.minecraft.world.inventory.*
import net.minecraft.world.item.*
import com.flooferland.showbiz.network.packets.BotListSelectPacket
import com.flooferland.showbiz.registry.ModScreenHandlers
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory

class BotSelectMenu(containerId: Int, val data: BotListSelectPacket) : AbstractContainerMenu(ModScreenHandlers.BotSelect.type, containerId) {
    override fun quickMoveStack(player: Player, index: Int) = ItemStack.EMPTY!!
    override fun stillValid(player: Player) =
        (player.level().getBlockEntity(data.blockPos) as? ExtendedScreenHandlerFactory<*> != null) && player.distanceToSqr(data.blockPos.center) < 12.0f * 12.0f
}