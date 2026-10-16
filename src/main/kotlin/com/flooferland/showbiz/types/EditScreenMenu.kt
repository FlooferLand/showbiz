package com.flooferland.showbiz.types

import net.minecraft.network.*
import net.minecraft.network.protocol.common.custom.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.inventory.*
import net.minecraft.world.item.*
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory

open class EditScreenMenu<P>(containerId: Int, menuType: MenuType<*>, val data: P) : AbstractContainerMenu(menuType, containerId)
where P: EditScreenMenu.EditScreenPacketPayload {
    val id = data.base.id

    abstract class EditScreenPacketPayload(val base: EditScreenBuf) : CustomPacketPayload
    data class EditScreenBuf(val id: OwnerId) {
        fun encode(buf: FriendlyByteBuf) {
            id.encode(buf)
        }
        companion object {
            fun decode(buf: FriendlyByteBuf): EditScreenBuf {
                val id = OwnerId.decode(buf)
                return EditScreenBuf(id)
            }
        }
    }

    override fun quickMoveStack(player: Player, index: Int) = ItemStack.EMPTY!!
    override fun stillValid(player: Player): Boolean {
        val level = player.level()
        val exists = when (id) {
            is OwnerId.BlockId -> level.getBlockEntity(id.blockPos) as? ExtendedScreenHandlerFactory<*> != null
            is OwnerId.EntityId -> id.grabEntity(level) as? ExtendedScreenHandlerFactory<*> != null
        }
        val pos = id.grabPos(level) ?: return false
        return exists && player.distanceToSqr(pos) < reachDistanceSqr
    }

    companion object {
        val reachDistanceSqr = 12.0f.let { it * it }
    }
}