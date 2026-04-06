package com.flooferland.showbiz.network.packets

import net.minecraft.core.BlockPos
import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.utils.rl

class BitViewPacket(val blockPos: BlockPos) : CustomPacketPayload {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<BitViewPacket>(rl("bit_view"))
        val codec = StreamCodec.of<FriendlyByteBuf, BitViewPacket>(
            { buf, packet ->
                buf.writeBlockPos(packet.blockPos)
            },
            { buf ->
                BitViewPacket(buf.readBlockPos())
            }
        )!!
    }
}