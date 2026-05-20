package com.flooferland.showbiz.network.packets

import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.show.readBitId
import com.flooferland.showbiz.show.writeBitId
import com.flooferland.showbiz.types.MappedBits
import com.flooferland.showbiz.utils.rl
import kotlin.collections.set

public class ProgrammerKeyPressPacket(val key: Int, val mappedBits: MappedBits, val pressed: Boolean) : CustomPacketPayload {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<ProgrammerKeyPressPacket>(rl("programmer_keypress"))
        val codec = StreamCodec.of<FriendlyByteBuf, ProgrammerKeyPressPacket>(
            { buf, packet ->
                buf.writeInt(packet.key)
                buf.writeInt(Showbiz.charts.size)
                packet.mappedBits.forEach { (map, bitId) ->
                    buf.writeUtf(map)
                    buf.writeBitId(bitId)
                }
                buf.writeBoolean(packet.pressed)
            },
            { buf ->
                val key = buf.readInt()

                val mappedBits = MappedBits()
                val bitChartCount = buf.readInt()
                repeat(bitChartCount) {
                    val mapping = buf.readUtf()
                    mappedBits[mapping] = buf.readBitId()
                }
                ProgrammerKeyPressPacket(key = key, mappedBits = mappedBits, pressed = buf.readBoolean())
            }
        )!!
    }
}