package com.flooferland.showbiz.network.packets

import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.utils.rl

class ShowFileSelectPacket(val selected: String) : CustomPacketPayload {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<ShowFileSelectPacket>(rl("show_file_select"))
        val codec = StreamCodec.of<FriendlyByteBuf, ShowFileSelectPacket>(
            { buf, packet ->
                buf.writeUtf(packet.selected)
            },
            { buf ->
                ShowFileSelectPacket(selected = buf.readUtf())
            }
        )!!
    }
}