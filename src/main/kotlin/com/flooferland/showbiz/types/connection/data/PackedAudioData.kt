package com.flooferland.showbiz.types.connection.data

import net.minecraft.core.*
import net.minecraft.network.*
import net.minecraft.server.level.*
import com.flooferland.showbiz.blocks.entities.SpeakerBlockEntity.Companion.AUDIO_DIST_SQUARE
import com.flooferland.showbiz.network.packets.PlaybackAudioChunkPacket
import com.flooferland.showbiz.types.FriendlyAudioFormat
import com.flooferland.showbiz.types.connection.ConnectionData
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

data class PackedAudioData(
    var left: ByteArray = byteArrayOf(),
    var right: ByteArray = byteArrayOf(),
    val format: FriendlyAudioFormat = FriendlyAudioFormat()
) : ConnectionData<PackedAudioData>("audio") {
    public var chunkId: Int = 0
    public var mono: ByteArray
        get() = left
        set(value) { left = value }

    override fun encode(buf: FriendlyByteBuf) {
        format.encode(buf)
    }

    override fun decode(buf: FriendlyByteBuf) {
        format.decode(buf)
    }

    override fun tempReset() {
        left = byteArrayOf()
        right = byteArrayOf()
    }

    override fun merge(other: PackedAudioData): Boolean {
        left = other.left.copyOf()
        right = other.right.copyOf()
        chunkId = other.chunkId
        return true
    }

    /** Broadcasts the audio to all players in range */
    fun broadcastAudio(level: ServerLevel, origin: BlockPos) {
        chunkId++
        for (player in level.players()) {
            if (player.distanceToSqr(origin.center) > AUDIO_DIST_SQUARE) continue
            val payload = PlaybackAudioChunkPacket(chunkId, origin, mono, format)
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