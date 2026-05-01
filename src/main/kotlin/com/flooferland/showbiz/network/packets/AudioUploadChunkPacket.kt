package com.flooferland.showbiz.network.packets

import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.utils.rl

class AudioUploadChunkPacket(val chunk: ByteArray) : CustomPacketPayload {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<AudioUploadChunkPacket>(rl("audio_upload_chunk_packet"))
        val codec = StreamCodec.of<FriendlyByteBuf, AudioUploadChunkPacket>(
            { buf, packet ->
                buf.writeByteArray(packet.chunk)
            },
            { buf ->
                AudioUploadChunkPacket(chunk = buf.readByteArray())
            }
        )!!
    }
}