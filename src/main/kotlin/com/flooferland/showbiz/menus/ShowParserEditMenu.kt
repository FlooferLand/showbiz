package com.flooferland.showbiz.menus

import net.minecraft.world.entity.player.*
import net.minecraft.world.inventory.*
import net.minecraft.world.item.*
import com.flooferland.showbiz.blocks.ShowParserBlock
import com.flooferland.showbiz.network.packets.ShowParserEditPacket
import com.flooferland.showbiz.registry.ModScreenHandlers

class ShowParserEditMenu(containerId: Int, val data: ShowParserEditPacket) : AbstractContainerMenu(ModScreenHandlers.ShowParserEdit.type, containerId) {
    val pos = data.blockPos

    override fun quickMoveStack(player: Player?, index: Int) = ItemStack.EMPTY!!
    override fun stillValid(player: Player) =
        player.level().getBlockState(this.pos).block is ShowParserBlock && player.distanceToSqr(pos.center) < 16.0f
}