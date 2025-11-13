package com.flooferland.showbiz.blocks.entities

import com.flooferland.showbiz.network.base.PlaybackChunkPacket
import com.flooferland.showbiz.network.base.PlaybackStatePacket
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.show.ShowData
import com.flooferland.showbiz.show.SignalFrame
import com.flooferland.showbiz.show.bitIdArrayOf
import com.flooferland.showbiz.types.connection.ConnectionManager
import com.flooferland.showbiz.types.connection.IConnectable
import com.flooferland.showbiz.types.connection.Ports
import com.flooferland.showbiz.utils.Extensions.getBooleanOrNull
import com.flooferland.showbiz.utils.Extensions.getDoubleOrNull
import com.flooferland.showbiz.utils.Extensions.getIntArrayOrNull
import com.flooferland.showbiz.utils.Extensions.markDirtyNotifyAll
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.network.protocol.game.*
import net.minecraft.server.level.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import kotlin.math.roundToInt

class ReelToReelBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.ReelToReel.entity!!, pos, blockState), IConnectable {
    override val connectionManager = ConnectionManager(this) {
        bind(Ports.PlayingOut)
        bind(Ports.SignalOut)
    }

    val aFrameMs: Int
        get() =  50 * (if (getFormat().channels == 1) 50 * 2 else 50 * 4)
    val aBufferSize: Int
        get() = (getFormat().sampleRate.toInt() * aFrameMs) / 1_000

    public var show: ShowData = ShowData(this)
    public var seek = 0.0
    public val signal = SignalFrame()

    var playing = false
        public get private set
    private val tickDelta = (50 * 0.001) // ms
    private val fps = 60  // Tied to rshw

    private var audioBytesWritten = 0

    fun tick() {
        if (level?.isClientSide ?: true) return
        if (!playing || show.isEmpty()) return
        var shouldUpdate = false

        seek += tickDelta
        val seekInt = (seek * fps).roundToInt()

        // Signal
        run {
            val entry = show.signal.getOrNull(seekInt) ?: bitIdArrayOf()
            signal.set(entry)
            shouldUpdate = signal.raw.isNotEmpty()
            connectionManager.send(Ports.SignalOut, signal)
        }

        // Audio
        val targetBytes = (seek * getFormat().sampleRate * getFormat().frameSize).toInt()
        if (audioBytesWritten < targetBytes && audioBytesWritten < show.audio.size) {
            var toWrite = aBufferSize
            toWrite -= toWrite % getFormat().frameSize

            val remain = show.audio.size - audioBytesWritten
            if (toWrite > remain) toWrite = remain

            if (toWrite > 0) {
                val chunk = show.audio.copyOfRange(audioBytesWritten, audioBytesWritten + toWrite)
                sendChunk(chunk, audioBytesWritten)
                audioBytesWritten += toWrite
            }
        }

        // Updating the block entity (sends network packet)
        if (shouldUpdate) markDirtyNotifyAll()
    }

    private fun sendChunk(chunk: ByteArray, startIndex: Int) {
        val serverLevel = level as? ServerLevel ?: return

        // TODO: Find only near players
        for (player in serverLevel.players()) {
            val payload = PlaybackChunkPacket(blockPos, chunk)
            ServerPlayNetworking.send(player, payload)
        }
    }

    fun setPlaying(playing: Boolean) {
        this.playing = playing
        if (!playing) resetPlayback()
        connectionManager.send(Ports.PlayingOut, playing)

        // TODO: Find only near players
        val serverLevel = level as? ServerLevel ?: return
        for (player in serverLevel.players()) {
            val state = PlaybackStatePacket(blockPos, playing = playing)
            ServerPlayNetworking.send(player, state)
        }
    }

    fun getFormat() = show.format ?: show.targetFormat

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)

        // Save other
        tag.putBoolean("Playing", playing)
        tag.putDouble("Seek", seek)
        tag.putIntArray("Signal-Frame", signal.save())
        connectionManager.save(tag)
        show.saveNBT(tag)
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)

        // Load other
        playing = tag.getBooleanOrNull("Playing") ?: false
        seek = tag.getDoubleOrNull("Seek") ?: 0.0
        signal.load(tag.getIntArrayOrNull("Signal-Frame"))
        connectionManager.load(tag)
        show.loadNBT(tag)
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = CompoundTag()
        saveAdditional(tag, registries)
        return tag
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket =
        ClientboundBlockEntityDataPacket.create(this) { _, registries ->
            val tag = CompoundTag()
            saveAdditional(tag, registries)
            tag
        }

    fun resetPlayback() {
        seek = 0.0
        audioBytesWritten = 0
        if (playing) setPlaying(false)
        signal.reset()
        show.reset()
    }
}