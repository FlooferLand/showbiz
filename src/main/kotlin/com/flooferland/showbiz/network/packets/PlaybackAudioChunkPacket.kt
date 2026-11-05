package com.flooferland.showbiz.network.packets

import com.flooferland.showbiz.types.FriendlyAudioFormat
import com.flooferland.showbiz.utils.rl
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

class PlaybackAudioChunkPacket(val id: Int, val blockPos: BlockPos, val audioChunk: ByteArray, val format: FriendlyAudioFormat, val lead: Double = 0.0) : CustomPacketPayload {
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
                buf.writeDouble(chunk.lead)
            },
            { buf ->
                val id = buf.readInt()
                val blockPos = buf.readBlockPos()
                val audioChunk = buf.readByteArray(buf.readInt())
                val format = FriendlyAudioFormat.codec.decode(buf)
                val lead = buf.readDouble()
                PlaybackAudioChunkPacket(
                    id = id,
                    blockPos = blockPos,
                    audioChunk = audioChunk,
                    format = format,
                    lead = lead
                )
            }
        )!!
    }
}