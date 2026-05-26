package com.flooferland.showbiz.network.packets

import net.minecraft.core.*
import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.utils.rl

class PlaybackStatePacket(val blockPos: BlockPos, val playing: Boolean, val paused: Boolean, val seek: Double) : CustomPacketPayload {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<PlaybackStatePacket>(rl("show_state"))
        val codec = StreamCodec.of<FriendlyByteBuf, PlaybackStatePacket>(
            { buf, state ->
                buf.writeBlockPos(state.blockPos)
                buf.writeBoolean(state.playing)
                buf.writeBoolean(state.paused)
                buf.writeFloat(state.seek.toFloat())
            },
            { buf ->
                val blockPos = buf.readBlockPos()
                val playing = buf.readBoolean()
                val paused = buf.readBoolean()
                val seek = buf.readFloat()
                PlaybackStatePacket(
                    blockPos = blockPos,
                    playing = playing,
                    paused = paused,
                    seek = seek.toDouble()
                )
            }
        )!!
    }
}