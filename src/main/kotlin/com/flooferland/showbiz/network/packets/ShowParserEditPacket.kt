package com.flooferland.showbiz.network.packets

import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import com.flooferland.showbiz.show.BitId
import com.flooferland.showbiz.show.toBitId
import com.flooferland.showbiz.utils.rl

class ShowParserEditPacket(val blockPos: BlockPos, val bitFilter: MutableList<BitId>, val mapping: String?) : CustomPacketPayload {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<ShowParserEditPacket>(rl("show_parser_edit"))
        val codec = StreamCodec.of<FriendlyByteBuf, ShowParserEditPacket>(
            { buf, conf ->
                buf.writeBlockPos(conf.blockPos)
                buf.writeVarIntArray(conf.bitFilter.map { it.toInt() }.toIntArray())
                buf.writeUtf(conf.mapping ?: "")
            },
            { buf ->
                val blockPos = buf.readBlockPos()
                val bitFilter = buf.readVarIntArray().map { it.toBitId() }.toMutableList()
                val mapping = buf.readUtf()
                ShowParserEditPacket(blockPos = blockPos, bitFilter = bitFilter, mapping = mapping)
            }
        )!!
    }
}