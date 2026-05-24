package com.flooferland.showbiz.network.packets

import net.minecraft.core.BlockPos
import net.minecraft.network.*
import net.minecraft.network.codec.*
import net.minecraft.network.protocol.common.custom.*
import com.flooferland.showbiz.utils.rl

class JukeboxLyricPacket(val blockPos: BlockPos, val lyric: String) : CustomPacketPayload {
    override fun type() = type

    companion object {
        val type = CustomPacketPayload.Type<JukeboxLyricPacket>(rl("jukebox_lyric"))
        val codec = StreamCodec.of<FriendlyByteBuf, JukeboxLyricPacket>(
            { buf, packet ->
                buf.writeBlockPos(packet.blockPos)
                buf.writeUtf(packet.lyric)
            },
            { buf ->
                JukeboxLyricPacket(blockPos = buf.readBlockPos(), lyric = buf.readUtf())
            }
        )!!
    }
}