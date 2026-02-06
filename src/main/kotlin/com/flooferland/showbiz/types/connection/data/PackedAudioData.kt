package com.flooferland.showbiz.types.connection.data

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import com.flooferland.showbiz.blocks.entities.SpeakerBlockEntity.Companion.AUDIO_DIST_SQUARE
import com.flooferland.showbiz.network.packets.PlaybackChunkPacket
import com.flooferland.showbiz.types.FriendlyAudioFormat
import com.flooferland.showbiz.types.connection.ConnectionData
import com.flooferland.showbiz.utils.Extensions.getByteArrayOrNull
import com.flooferland.showbiz.utils.Extensions.getIntOrNull
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

data class PackedAudioData(
    var left: ByteArray = byteArrayOf(),
    var right: ByteArray = byteArrayOf(),
    val format: FriendlyAudioFormat = FriendlyAudioFormat()
) : ConnectionData("audio") {
    public var chunkId: Int = 0
    public var mono: ByteArray
        get() = left
        set(value) { left = value }

    override fun saveOrThrow(tag: CompoundTag) {
        tag.putByteArray("left", left)
        tag.putByteArray("right", right)
        tag.put("format", CompoundTag().also { format.saveOrThrow(it) })
    }

    override fun loadOrThrow(tag: CompoundTag) {
        left = tag.getByteArrayOrNull("left") ?: byteArrayOf()
        right = tag.getByteArrayOrNull("right") ?: byteArrayOf()
        tag.getCompound("format").let { format.loadOrThrow(it) }
    }

    fun broadcastToAll(level: ServerLevel, origin: BlockPos) {
        chunkId++
        for (player in level.players()) {
            if (player.distanceToSqr(origin.center) > AUDIO_DIST_SQUARE) continue
            val payload = PlaybackChunkPacket(chunkId, origin, mono, format)
            ServerPlayNetworking.send(player, payload)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PackedAudioData
        if (!left.contentEquals(other.left)) return false
        if (!right.contentEquals(other.right)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = left.contentHashCode()
        result = 31 * result + right.contentHashCode()
        result = 31 * chunkId
        return result
    }
}