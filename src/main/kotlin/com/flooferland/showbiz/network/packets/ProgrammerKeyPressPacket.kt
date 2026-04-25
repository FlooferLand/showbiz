package com.flooferland.showbiz.network.packets

import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.utils.rl

public class ProgrammerKeyPressPacket(val key: Int, val bit: Short, val pressed: Boolean) : CustomPacketPayload {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<ProgrammerKeyPressPacket>(rl("programmer_keypress"))
        val codec = StreamCodec.of<FriendlyByteBuf, ProgrammerKeyPressPacket>(
            { buf, packet ->
                buf.writeInt(packet.key)
                buf.writeShort(packet.bit.toInt())
                buf.writeBoolean(packet.pressed)
            },
            { buf ->
                ProgrammerKeyPressPacket(key = buf.readInt(), bit = buf.readShort(), pressed = buf.readBoolean())
            }
        )!!
    }
}