package com.flooferland.showbiz.menus

import net.minecraft.world.entity.player.*
import net.minecraft.world.inventory.*
import net.minecraft.world.item.*
import com.flooferland.showbiz.network.packets.BotListSelectPacket
import com.flooferland.showbiz.registry.ModScreenHandlers
import com.flooferland.showbiz.types.IBot

class BotSelectMenu(containerId: Int, val data: BotListSelectPacket) : AbstractContainerMenu(ModScreenHandlers.BotSelect.type, containerId) {
    override fun quickMoveStack(player: Player, index: Int) = ItemStack.EMPTY!!
    override fun stillValid(player: Player): Boolean {
        val botPos = (data.bot.grabConnectable(player.level()) as? IBot)?.botPos
        return botPos != null && player.distanceToSqr(botPos) < 12.0f * 12.0f
    }
}