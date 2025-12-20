package com.flooferland.showbiz.network.packets

import com.flooferland.showbiz.utils.rl
import net.minecraft.core.*
import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.types.FriendlyAudioFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioFormat.Encoding

class PlaybackChunkPacket(val blockPos: BlockPos, val audioChunk: ByteArray, val format: FriendlyAudioFormat) : CustomPacketPayload {
    override fun type() = type

    val playing = audioChunk.isNotEmpty()

    companion object {
        val type = CustomPacketPayload.Type<PlaybackChunkPacket>(rl("show_data"))
        val codec = StreamCodec.of<FriendlyByteBuf, PlaybackChunkPacket>(
            { buf, chunk ->
                buf.writeBlockPos(chunk.blockPos)
                buf.writeInt(chunk.audioChunk.size)
                buf.writeByteArray(chunk.audioChunk)
                FriendlyAudioFormat.codec.encode(buf, chunk.format)
            },
            { buf ->
                val blockPos = buf.readBlockPos()
                val audioChunk = buf.readByteArray(buf.readInt())
                val format = FriendlyAudioFormat.codec.decode(buf)
                PlaybackChunkPacket(
                    blockPos = blockPos,
                    audioChunk = audioChunk,
                    format = format
                )
            }
        )!!
    }
}