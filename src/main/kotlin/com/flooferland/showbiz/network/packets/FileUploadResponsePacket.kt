package com.flooferland.showbiz.network.packets

import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.utils.rl

/** Server's response to client packets */
class FileUploadResponsePacket(val status: ServerMessage, val bytesSoFar: Long) : CustomPacketPayload {
    override fun type() = type
    enum class ServerMessage {
        Continue,
        Done,
        FuckOff
    }

    companion object {
        val type = CustomPacketPayload.Type<FileUploadResponsePacket>(rl("file_upload_response"))
        val codec = StreamCodec.of<FriendlyByteBuf, FileUploadResponsePacket>(
            { buf, packet ->
                buf.writeEnum(packet.status)
                buf.writeLong(packet.bytesSoFar)
            },
            { buf ->
                FileUploadResponsePacket(
                    status = buf.readEnum(ServerMessage::class.java),
                    bytesSoFar = buf.readLong()
                )
            }
        )!!
    }
}