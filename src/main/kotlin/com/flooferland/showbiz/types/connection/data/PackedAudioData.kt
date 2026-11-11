package com.flooferland.showbiz.types.connection.data

import com.flooferland.showbiz.network.packets.PlaybackAudioChunkPacket
import com.flooferland.showbiz.network.packets.PlaybackAudioStatePacket
import com.flooferland.showbiz.types.FriendlyAudioFormat
import com.flooferland.showbiz.types.connection.ConnectionData
import com.flooferland.showbiz.types.connection.ConnectionPort
import com.flooferland.showbiz.types.connection.data.PackedAudioData.Action
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3

fun ConnectionPort<PackedAudioData>.sendTrigger(action: Action) {
    data.tempReset()
    data.triggerAction = action
    send()
    data.triggerAction = Action.None
}

data class PackedAudioData(
    var mono: ByteArray = byteArrayOf(),
    var triggerAction: Action = Action.None,
    val format: FriendlyAudioFormat = FriendlyAudioFormat(),
) : ConnectionData<PackedAudioData>("audio") {
    public var chunkId: Int = 0
    var lead = 0.0

    enum class Action {
        None,
        Pause,
        Unpause,
        Stop
    }

    override fun encode(buf: FriendlyByteBuf) {
        buf.writeEnum(triggerAction)
        format.encode(buf)
    }

    override fun decode(buf: FriendlyByteBuf) {
        triggerAction = buf.readEnum(Action::class.java)
        format.decode(buf)
    }

    override fun tempReset() {
        mono = byteArrayOf()
    }

    override fun merge(other: PackedAudioData): Boolean {
        mono = other.mono.copyOf()
        chunkId = other.chunkId
        lead = other.lead
        triggerAction = other.triggerAction
        return true
    }

    override fun canReach(origin: Vec3, player: ServerPlayer): Boolean =
        player.distanceToSqr(origin) < MAX_DISTANCE_SQR

    // region | Public stuff
    fun tick(level: ServerLevel, source: BlockPos) {
        when {
            triggerAction != Action.None -> broadcastState(level, source)
            mono.isNotEmpty() -> broadcastAudio(level, source)
        }
    }

    /** Broadcasts the audio to all players in range */
    fun broadcastAudio(level: ServerLevel, source: BlockPos) {
        for (player in level.players()) {
            if (player.distanceToSqr(source.center) > MAX_DISTANCE_SQR) continue
            val payload = PlaybackAudioChunkPacket(chunkId, source, mono, format, lead)
            ServerPlayNetworking.send(player, payload)
        }
    }
    fun broadcastState(level: ServerLevel, source: BlockPos) {
        for (player in level.players()) {
            if (player.distanceToSqr(source.center) > MAX_DISTANCE_SQR) continue
            val playing = triggerAction != Action.Stop
            val paused = triggerAction == Action.Pause
            val payload = PlaybackAudioStatePacket(source, playing, paused)
            ServerPlayNetworking.send(player, payload)
        }
    }
    fun clear() {
        tempReset()
        chunkId = 0
    }
    // endregion

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PackedAudioData

        if (chunkId != other.chunkId) return false
        if (!mono.contentEquals(other.mono)) return false
        if (triggerAction != other.triggerAction) return false
        if (format != other.format) return false

        return true
    }
    override fun hashCode(): Int {
        var result = chunkId
        result = 31 * result + mono.contentHashCode()
        result = 31 * result + triggerAction.hashCode()
        result = 31 * result + format.hashCode()
        return result
    }

    companion object {
        val MAX_DISTANCE get() = 48f
        val MAX_DISTANCE_SQR get() = MAX_DISTANCE * MAX_DISTANCE
    }
}