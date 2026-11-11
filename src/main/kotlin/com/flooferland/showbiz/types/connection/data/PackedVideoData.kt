package com.flooferland.showbiz.types.connection.data

import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.blocks.entities.MonitorBlockEntity.Companion.VIDEO_DIST_SQUARE
import com.flooferland.showbiz.types.OwnerId
import com.flooferland.showbiz.types.connection.ConnectionData
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3

data class PackedVideoData(
    var bytes: ByteArray = byteArrayOf(),
    var width: Int = 0,
    var height: Int = 0,
    var channels: Int = 3,
    var timestamp: Long = 0L
) : ConnectionData<PackedVideoData>("video") {
    override fun encode(buf: FriendlyByteBuf) {
        buf.writeInt(width)
        buf.writeInt(height)
        buf.writeInt(channels)
        buf.writeLong(timestamp)
        buf.writeByteArray(bytes)
    }

    override fun decode(buf: FriendlyByteBuf) {
        width = buf.readInt()
        height = buf.readInt()
        channels = buf.readInt()
        timestamp = buf.readLong()
        bytes = buf.readByteArray(width * height * channels)
    }

    override fun tempReset() {
        bytes = byteArrayOf()
    }

    override fun merge(other: PackedVideoData): Boolean {
        bytes = other.bytes.copyOf()
        return true
    }

    override fun canReach(origin: Vec3, player: ServerPlayer): Boolean {
        return player.distanceToSqr(origin) < VIDEO_DIST_SQUARE
    }

    override fun canSendTo(ownerId: OwnerId, player: ServerPlayer): Boolean {
        return Showbiz.playerPrefs[player.uuid]?.config?.video?.playVideo ?: true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PackedVideoData
        if (width != other.width) return false
        if (height != other.height) return false
        if (channels != other.channels) return false
        if (timestamp != other.timestamp) return false
        if (!bytes.contentEquals(other.bytes)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + channels
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}