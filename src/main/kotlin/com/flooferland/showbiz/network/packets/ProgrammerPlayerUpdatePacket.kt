package com.flooferland.showbiz.network.packets

import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.show.readBitId
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
                    val entries = mappedBits.entries.toTypedArray()
                    buf.writeInt(entries.size)
                    entries.forEach { (map, bitId) ->
                        buf.writeUtf(map)
                        buf.writeShort(bitId.toInt())
                    }
                }
            },
            { buf ->
                val keysToBits = Array(PlayerProgrammingData.SIZE) { MappedBits() }
                repeat(PlayerProgrammingData.SIZE) {
                    val i = buf.readInt()
                    val mappedBitsSize = buf.readInt()
                    repeat(mappedBitsSize) {
                        val mapping = buf.readUtf()
                        keysToBits[i][mapping] = buf.readBitId()
                    }
                }
                ProgrammerPlayerUpdatePacket(keysToBits)
            }
        )!!
    }
}