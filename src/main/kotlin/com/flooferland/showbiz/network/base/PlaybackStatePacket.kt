package com.flooferland.showbiz.network.base

import com.flooferland.showbiz.utils.rl
import net.minecraft.core.*
import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*

class PlaybackStatePacket(val blockPos: BlockPos, val playing: Boolean = false) : CustomPacketPayload {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<PlaybackStatePacket>(rl("show_state"))
        val codec = StreamCodec.of<FriendlyByteBuf, PlaybackStatePacket>(
            { buf, state ->
                buf.writeBlockPos(state.blockPos)
                buf.writeBoolean(state.playing)
            },
            { buf ->
                val blockPos = buf.readBlockPos()
                val playing = buf.readBoolean()
                PlaybackStatePacket(
                    blockPos = blockPos,
                    playing = playing
                )
            }
        )!!
    }
}