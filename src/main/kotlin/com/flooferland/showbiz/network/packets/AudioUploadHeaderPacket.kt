package com.flooferland.showbiz.network.packets

import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.FileServer
import com.flooferland.showbiz.utils.rl

class AudioUploadHeaderPacket(val file: String, val fileSizeBytes: Long) : CustomPacketPayload {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<AudioUploadHeaderPacket>(rl("audio_upload_header_packet"))
        val codec = StreamCodec.of<FriendlyByteBuf, AudioUploadHeaderPacket>(
            { buf, packet ->
                buf.writeUtf(packet.file)
                buf.writeLong(packet.fileSizeBytes)
            },
            { buf ->
                AudioUploadHeaderPacket(file = buf.readUtf(), fileSizeBytes = buf.readLong())
            }
        )!!
    }
}