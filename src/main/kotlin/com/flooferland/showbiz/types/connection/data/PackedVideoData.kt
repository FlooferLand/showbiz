package com.flooferland.showbiz.types.connection.data

import net.minecraft.network.*
import net.minecraft.server.level.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.blocks.entities.MonitorBlockEntity.Companion.VIDEO_DIST_SQUARE
import com.flooferland.showbiz.types.connection.ConnectionData

data class PackedVideoData(
    var bytes: ByteArray = byteArrayOf(),
    var width: Int = 0,
    var height: Int = 0,
    var channels: Int = 3
) : ConnectionData<PackedVideoData>("video") {
    override fun encode(buf: FriendlyByteBuf) {
        buf.writeInt(width)
        buf.writeInt(height)
        buf.writeInt(channels)
        buf.writeByteArray(bytes)
    }

    override fun decode(buf: FriendlyByteBuf) {
        width = buf.readInt()
        height = buf.readInt()
        channels = buf.readInt()
        bytes = buf.readByteArray(width * height * channels)
    }

    override fun tempReset() {
        bytes = byteArrayOf()
    }

    override fun merge(other: PackedVideoData): Boolean {
        bytes = other.bytes.copyOf()
        return true
    }

    override fun canSend(level: ServerLevel, origin: Vec3) =
        level.players().any { it.distanceToSqr(origin) < VIDEO_DIST_SQUARE }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PackedVideoData
        if (width != other.width) return false
        if (height != other.height) return false
        if (channels != other.channels) return false
        if (!bytes.contentEquals(other.bytes)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + channels
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}