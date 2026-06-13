package com.flooferland.showbiz.types

import net.minecraft.network.*

/**
 * Sent with [com.flooferland.showbiz.network.packets.ShowFileListPacket].
 * The client doesn't have the show file stored on its disk, so this is the next best thing
 */
data class ShowFileInfo(val id: String, val hasVideo: Boolean) {
    fun encode(buf: FriendlyByteBuf) {
        buf.writeUtf(id)
        buf.writeBoolean(hasVideo)
    }
    companion object {
        fun decode(buf: FriendlyByteBuf): ShowFileInfo {
            val id = buf.readUtf()
            val hasVideo = buf.readBoolean()
            return ShowFileInfo(id, hasVideo)
        }
    }
}
