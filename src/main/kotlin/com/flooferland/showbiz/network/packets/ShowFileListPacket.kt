package com.flooferland.showbiz.network.packets

import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.utils.rl

class ShowFileListPacket(val isResponse: Boolean = false, val files: Array<String> = arrayOf()) : CustomPacketPayload {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<ShowFileListPacket>(rl("show_file_list"))
        val codec = StreamCodec.of<FriendlyByteBuf, ShowFileListPacket>(
            { buf, packet ->
                buf.writeBoolean(packet.isResponse)
                if (packet.isResponse) {
                    buf.writeShort(packet.files.size)
                    for (entry in packet.files) {
                        buf.writeUtf(entry)
                    }
                }
            },
            { buf ->
                val isResponse = buf.readBoolean()
                if (isResponse) {
                    val size = buf.readShort().toInt()
                    val files = MutableList(size) { buf.readUtf() }
                    ShowFileListPacket(true, files.toTypedArray())
                } else {
                    ShowFileListPacket(false)
                }
            }
        )!!
    }
}