package com.flooferland.showbiz.network.packets

import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.FileServer
import com.flooferland.showbiz.utils.rl

class ShowFileEditPacket(val file: String, val action: FileServer.FileAction) : CustomPacketPayload {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<ShowFileEditPacket>(rl("show_file_edit"))
        val codec = StreamCodec.of<FriendlyByteBuf, ShowFileEditPacket>(
            { buf, packet ->
                buf.writeUtf(packet.file)
                buf.writeEnum(packet.action)
            },
            { buf ->
                ShowFileEditPacket(file = buf.readUtf(), action = buf.readEnum(FileServer.FileAction::class.java))
            }
        )!!
    }
}