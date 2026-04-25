package com.flooferland.showbiz.network.packets

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import com.flooferland.showbiz.show.BitIdArray
import com.flooferland.showbiz.utils.rl

class ProgrammerPlayerUpdatePacket(val keysToBits: BitIdArray) : CustomPacketPayload {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<ProgrammerPlayerUpdatePacket>(rl("programmer_player_update"))
        val codec = StreamCodec.of<FriendlyByteBuf, ProgrammerPlayerUpdatePacket>(
            { buf, state ->
                buf.writeVarInt(state.keysToBits.size)
                state.keysToBits.forEach { buf.writeShort(it.toInt()) }
            },
            { buf ->
                ProgrammerPlayerUpdatePacket(
                    keysToBits = BitIdArray(buf.readVarInt()).also { it.forEachIndexed { i, _ -> it[i] = buf.readShort().toUShort() } }
                )
            }
        )!!
    }
}