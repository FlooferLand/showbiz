package com.flooferland.showbiz.network.packets

import net.minecraft.core.*
import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.types.FriendlyAudioFormat
import com.flooferland.showbiz.utils.rl

class PlaybackAudioChunkPacket(val id: Int, val blockPos: BlockPos, val audioChunk: ByteArray, val format: FriendlyAudioFormat) : CustomPacketPayload {
    override fun type() = type

    val playing = audioChunk.isNotEmpty()

    companion object {
        val type = CustomPacketPayload.Type<PlaybackAudioChunkPacket>(rl("audio_show_data"))
        val codec = StreamCodec.of<FriendlyByteBuf, PlaybackAudioChunkPacket>(
            { buf, chunk ->
                buf.writeInt(chunk.id)
                buf.writeBlockPos(chunk.blockPos)
                buf.writeInt(chunk.audioChunk.size)
                buf.writeByteArray(chunk.audioChunk)
                FriendlyAudioFormat.codec.encode(buf, chunk.format)
            },
            { buf ->
                val id = buf.readInt()
                val blockPos = buf.readBlockPos()
                val audioChunk = buf.readByteArray(buf.readInt())
                val format = FriendlyAudioFormat.codec.decode(buf)
                PlaybackAudioChunkPacket(
                    id = id,
                    blockPos = blockPos,
                    audioChunk = audioChunk,
                    format = format
                )
            }
        )!!
    }
}