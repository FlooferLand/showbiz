package com.flooferland.showbiz.types

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack
import com.flooferland.showbiz.show.BitId
import com.flooferland.showbiz.show.toBitId
import com.flooferland.showbiz.utils.Extensions.getIntArrayOrNull
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory

open class EditScreenMenu<P>(containerId: Int, menuType: MenuType<*>, val data: P) : AbstractContainerMenu(menuType, containerId)
where P: EditScreenMenu.EditScreenPacketPayload {
    val pos = data.base.blockPos

    // TODO: Make bitFilter filter bits per map (ex: make it a HashMap of show mappings to BitIds). Same with SpotlightBlockEntity

    abstract class EditScreenPacketPayload(val base: EditScreenBuf) : CustomPacketPayload
    data class EditScreenBuf(val blockPos: BlockPos, var bitFilter: MutableList<BitId> = mutableListOf<BitId>(), var mapping: String? = null) {
        fun loadAdditional(tag: CompoundTag) {
            bitFilter = (tag.getIntArrayOrNull("bit_filter") ?: intArrayOf()).map { it.toBitId() }.toMutableList()
        }
        fun saveAdditional(tag: CompoundTag) {
            tag.putIntArray("bit_filter", bitFilter.map { it.toInt() })
        }

        fun encode(buf: FriendlyByteBuf) {
            buf.writeBlockPos(blockPos)
            buf.writeVarIntArray(bitFilter.map { it.toInt() }.toIntArray())
            buf.writeUtf(mapping ?: "")
        }
        companion object {
            fun decode(buf: FriendlyByteBuf): EditScreenBuf {
                val blockPos = buf.readBlockPos()
                val bitFilter = buf.readVarIntArray().map { it.toBitId() }.toMutableList()
                val mapping = buf.readUtf()
                return EditScreenBuf(blockPos, bitFilter, mapping)
            }
        }
    }

    override fun quickMoveStack(player: Player, index: Int) = ItemStack.EMPTY!!
    override fun stillValid(player: Player) =
        (player.level().getBlockEntity(this.pos) as? ExtendedScreenHandlerFactory<*> != null) && player.distanceToSqr(pos.center) < 12.0f
}