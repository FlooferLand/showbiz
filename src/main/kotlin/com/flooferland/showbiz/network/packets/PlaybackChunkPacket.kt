package com.flooferland.showbiz.network.packets

import com.flooferland.showbiz.utils.rl
import net.minecraft.core.*
import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*

class PlaybackChunkPacket(val blockPos: BlockPos, val audioChunk: ByteArray) : CustomPacketPayload {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<PlaybackChunkPacket>(rl("show_data"))
        val codec = StreamCodec.of<FriendlyByteBuf, PlaybackChunkPacket>(
            { buf, chunk ->
                buf.writeBlockPos(chunk.blockPos)
                buf.writeInt(chunk.audioChunk.size)
                buf.writeByteArray(chunk.audioChunk)
            },
            { buf ->
                val blockPos = buf.readBlockPos()
                val audioChunk = buf.readByteArray(buf.readInt())
                PlaybackChunkPacket(
                    blockPos = blockPos,
                    audioChunk = audioChunk
                )
            }
        )!!
    }
}