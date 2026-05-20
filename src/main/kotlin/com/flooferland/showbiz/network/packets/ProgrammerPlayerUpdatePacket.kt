package com.flooferland.showbiz.network.packets

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.show.readBitId
import com.flooferland.showbiz.show.toBitId
import com.flooferland.showbiz.types.MappedBits
import com.flooferland.showbiz.types.entity.PlayerProgrammingData
import com.flooferland.showbiz.utils.rl

class ProgrammerPlayerUpdatePacket(val keysToBits: Array<MappedBits>) : CustomPacketPayload {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<ProgrammerPlayerUpdatePacket>(rl("programmer_player_update"))
        val codec = StreamCodec.of<FriendlyByteBuf, ProgrammerPlayerUpdatePacket>(
            { buf, state ->
                state.keysToBits.forEachIndexed { i, mappedBits ->
                    buf.writeInt(i)
                    buf.writeInt(mappedBits.size)
                    mappedBits.forEach { (map, bitId) ->
                        buf.writeUtf(map)
                        buf.writeShort(bitId.toInt())
                    }
                }
            },
            { buf ->
                val keysToBits = MutableList(PlayerProgrammingData.SIZE) { MappedBits() }
                repeat(PlayerProgrammingData.SIZE) {
                    val i = buf.readInt()
                    val mappedBitsSize = buf.readInt()
                    repeat(mappedBitsSize) {
                        val mapping = buf.readUtf()
                        keysToBits[i][mapping] = buf.readBitId()
                    }
                }
                ProgrammerPlayerUpdatePacket(keysToBits.toTypedArray())
            }
        )!!
    }
}