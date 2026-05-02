package com.flooferland.showbiz.network.packets

import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.utils.rl

class FileUploadChunkPacket(val chunk: ByteArray) : CustomPacketPayload {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<FileUploadChunkPacket>(rl("file_upload_chunk_packet"))
        val codec = StreamCodec.of<FriendlyByteBuf, FileUploadChunkPacket>(
            { buf, packet ->
                buf.writeByteArray(packet.chunk)
            },
            { buf ->
                FileUploadChunkPacket(chunk = buf.readByteArray())
            }
        )!!
    }
}