package com.flooferland.showbiz.types

import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.network.*
import net.minecraft.network.protocol.common.custom.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.inventory.*
import net.minecraft.world.item.*
import com.flooferland.showbiz.show.toBitId
import com.flooferland.showbiz.utils.Extensions.getCompoundOrNull
import com.flooferland.showbiz.utils.Extensions.getIntArrayOrNull
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory

open class EditScreenMenu<P>(containerId: Int, menuType: MenuType<*>, val data: P) : AbstractContainerMenu(menuType, containerId)
where P: EditScreenMenu.EditScreenPacketPayload {
    val pos = data.base.blockPos

    abstract class EditScreenPacketPayload(val base: EditScreenBuf) : CustomPacketPayload
    data class EditScreenBuf(val blockPos: BlockPos, val bitFilter: MappedBits = MappedBits(), var mapping: String? = null) {
        fun loadAdditional(tag: CompoundTag) {
            bitFilter.clearCharts()

            // Backwards compatibility with pre-0.4.0
            if (tag.contains("bit_filter", Tag.TAG_INT_ARRAY.toInt())) {
                tag.getIntArrayOrNull("bit_filter")?.forEach {
                    bitFilter.addBit(BitChartStore.DEFAULT, it.toBitId())
                }
                return
            }

            tag.getCompoundOrNull("bit_filter")?.let { filterTag ->
                filterTag.allKeys.forEach { chartId ->
                    filterTag.getIntArrayOrNull(chartId)?.forEach { bitId ->
                        bitFilter.addBit(chartId, bitId.toBitId())
                    }
                }
            }
        }
        fun saveAdditional(tag: CompoundTag) {
            tag.put("bit_filter", CompoundTag().also { tag ->
                bitFilter.charts.forEach { chartId ->
                    val bitsArray = bitFilter.getOrPutDefault(chartId).map { it.toInt() }.toIntArray()
                    tag.putIntArray(chartId, bitsArray)
                }
            })
        }

        fun encode(buf: FriendlyByteBuf) {
            buf.writeBlockPos(blockPos)

            buf.writeVarInt(bitFilter.charts.size)
            bitFilter.charts.forEach { chartId ->
                buf.writeUtf(chartId)
                val bits = bitFilter.getOrPutDefault(chartId).map { it.toInt() }.toIntArray()
                buf.writeVarIntArray(bits)
            }

            buf.writeUtf(mapping ?: "")
        }
        companion object {
            fun decode(buf: FriendlyByteBuf): EditScreenBuf {
                val blockPos = buf.readBlockPos()

                val bitFilter = MappedBits()
                val size = buf.readVarInt()
                repeat(size) {
                    val chartId = buf.readUtf()
                    buf.readVarIntArray().forEach { bitFilter.addBit(chartId, it.toBitId()) }
                }

                val mapping = buf.readUtf().takeIf { it.isNotEmpty() }
                return EditScreenBuf(blockPos, bitFilter, mapping)
            }
        }
    }

    override fun quickMoveStack(player: Player, index: Int) = ItemStack.EMPTY!!
    override fun stillValid(player: Player) =
        (player.level().getBlockEntity(this.pos) as? ExtendedScreenHandlerFactory<*> != null) && player.distanceToSqr(pos.center) < reachDistanceSqr

    companion object {
        val reachDistanceSqr = 12.0f.let { it * it }
    }
}