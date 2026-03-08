package com.flooferland.showbiz.menus

import net.minecraft.world.entity.player.*
import net.minecraft.world.inventory.*
import net.minecraft.world.item.*
import com.flooferland.showbiz.blocks.SpotlightBlock
import com.flooferland.showbiz.network.packets.SpotlightEditPacket
import com.flooferland.showbiz.registry.ModScreenHandlers

class SpotlightEditMenu(containerId: Int, val data: SpotlightEditPacket) : AbstractContainerMenu(ModScreenHandlers.SpotlightEdit.type, containerId) {
    val pos = data.blockPos

    override fun quickMoveStack(player: Player?, index: Int) = ItemStack.EMPTY!!
    override fun stillValid(player: Player) =
        player.level().getBlockState(this.pos).block is SpotlightBlock && player.distanceToSqr(pos.center) < 16.0f
}