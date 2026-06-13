package com.flooferland.showbiz.network.packets

import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.types.ShowFileInfo
import com.flooferland.showbiz.utils.rl

class ShowFileListPacket(val toClient: Boolean = false, val playerAuthorized: Boolean = false, val files: Set<ShowFileInfo> = setOf()) : CustomPacketPayload {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<ShowFileListPacket>(rl("show_file_list"))
        val codec = StreamCodec.of<FriendlyByteBuf, ShowFileListPacket>(
            { buf, packet ->
                buf.writeBoolean(packet.toClient)
                buf.writeBoolean(packet.playerAuthorized)
                if (packet.toClient) {
                    buf.writeShort(packet.files.size)
                    for (entry in packet.files) entry.encode(buf)
                }
            },
            { buf ->
                val isResponse = buf.readBoolean()  // toClient
                val playerAuthorized = buf.readBoolean()
                if (isResponse) {
                    val size = buf.readShort().toInt()
                    val files = MutableList(size) { ShowFileInfo.decode(buf) }
                    ShowFileListPacket(true, playerAuthorized, files.toSet())
                } else {
                    ShowFileListPacket(false)
                }
            }
        )!!
    }
}