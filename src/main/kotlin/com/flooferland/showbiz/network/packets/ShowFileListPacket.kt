package com.flooferland.showbiz.network.packets

import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.utils.rl

class ShowFileListPacket(val toClient: Boolean = false, val files: Array<String> = arrayOf(), val playerAuthorized: Boolean = false) : CustomPacketPayload {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<ShowFileListPacket>(rl("show_file_list"))
        val codec = StreamCodec.of<FriendlyByteBuf, ShowFileListPacket>(
            { buf, packet ->
                buf.writeBoolean(packet.toClient)
                if (packet.toClient) {
                    buf.writeShort(packet.files.size)
                    for (entry in packet.files) {
                        buf.writeUtf(entry)
                    }
                }
                buf.writeBoolean(packet.playerAuthorized)
            },
            { buf ->
                val isResponse = buf.readBoolean()  // toClient
                if (isResponse) {
                    val size = buf.readShort().toInt()
                    val files = MutableList(size) { buf.readUtf() }
                    val playerAuthorized = buf.readBoolean()
                    ShowFileListPacket(true, files.toTypedArray(), playerAuthorized)
                } else {
                    ShowFileListPacket(false)
                }
            }
        )!!
    }
}