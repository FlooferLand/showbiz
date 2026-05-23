package com.flooferland.showbiz.network.packets

import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.show.BitId
import com.flooferland.showbiz.show.readBitId
import com.flooferland.showbiz.show.writeBitId
import com.flooferland.showbiz.types.MappedBits
import com.flooferland.showbiz.utils.rl

public class ProgrammerKeyPressPacket(val key: Int, val mappedBits: MappedBits, val pressed: Boolean) : CustomPacketPayload {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<ProgrammerKeyPressPacket>(rl("programmer_keypress"))
        val codec = StreamCodec.of<FriendlyByteBuf, ProgrammerKeyPressPacket>(
            { buf, packet ->
                buf.writeInt(packet.key)
                buf.writeInt(packet.mappedBits.size)
                packet.mappedBits.forEach { (map, bits) ->
                    buf.writeUtf(map)
                    buf.writeInt(bits.size)
                    bits.forEach { buf.writeBitId(it) }
                }
                buf.writeBoolean(packet.pressed)
            },
            { buf ->
                val key = buf.readInt()
                val mappedBits = MappedBits()
                val bitChartCount = buf.readInt()

                repeat(bitChartCount) {
                    val chartId = buf.readUtf()
                    val size = buf.readInt()
                    val bits = mutableSetOf<BitId>()
                    repeat(size) { bits.add(buf.readBitId()) }
                    mappedBits.setBits(chartId, bits)
                }
                ProgrammerKeyPressPacket(key = key, mappedBits = mappedBits, pressed = buf.readBoolean())
            }
        )!!
    }
}