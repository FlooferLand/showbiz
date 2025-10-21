package com.flooferland.showbiz.blocks.entities

import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.show.ShowData
import com.flooferland.showbiz.show.SignalFrame
import com.flooferland.showbiz.utils.Extensions.getBooleanOrNull
import com.flooferland.showbiz.utils.Extensions.getByteArrayOrNull
import com.flooferland.showbiz.utils.Extensions.getDoubleOrNull
import com.flooferland.showbiz.utils.Extensions.markDirtyNotifyAll
import com.flooferland.showbiz.utils.ShowbizUtils
import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.network.protocol.game.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.SourceDataLine
import kotlin.io.path.Path
import kotlin.math.roundToInt

class PlaybackControllerBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.PlaybackController.entity!!, pos, blockState) {
    public var show: ShowData = ShowData()
    public var playing = false
    public var seek = 0.0
    public val signal = SignalFrame()

    private val tickDelta = (50 * 0.001) // ms
    private val fps = 60  // Tied to rshw

    // -- Each song's metadata should be extracted manually for now (I like using foobar2000 for this)
    private val aSampleRate = 44100
    private val aSampleBits = 16
    private val aChannels = 2
    private val aFrameMs = 50 * 4
    private val aBufferSize = (aSampleRate * aFrameMs) / 1_000
    private var aFormat: AudioFormat? = null
    private var audioLine: SourceDataLine? = null

    private var bytesWritten: Int = 0

    companion object {
        val TEST_FILE = run {
        //  Path("${Showbiz.MOD_ID}/shows/1 - Mouth.rshw")
            Path("D:/Animatronics/Creative Engineering/Show tapes/Catch a Wave.rshw")
        }
    }

    fun tick() {
        if (!playing || show.isEmpty()) return
        var shouldUpdate = false

        seek += tickDelta
        val seekInt = (seek * fps).roundToInt()

        // Signal
        run {
            // TODO: Figure out how rshw playback works and write signal playback

            val entry = show.signal.getOrNull(seekInt)
            entry?.let {
                signal.load(entry)
                shouldUpdate = true
            }
        }

        // Initializing audio
        if (audioLine == null || audioLine?.isOpen != true) {
            aFormat = AudioFormat(aSampleRate.toFloat(), aSampleBits, aChannels, true, false)
            audioLine = ShowbizUtils.startAudioDevice(aFormat!!, aBufferSize).getOrNull()
        }

        // Playing back audio
        if (bytesWritten < show.audio.size) {
            var toWrite = aBufferSize
            toWrite -= toWrite % aFormat!!.frameSize // align to frame

            val remain = show.audio.size - bytesWritten
            if (toWrite > remain) toWrite = remain

            if (toWrite > 0) {
                val chunk = show.audio.copyOfRange(bytesWritten, bytesWritten + toWrite)
                audioLine?.write(chunk, 0, chunk.size)
                bytesWritten += toWrite
                shouldUpdate = true
            }
        }

        // Updating the block entity (sends network packet)
        if (shouldUpdate) markDirtyNotifyAll()
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)

        // Save other
        tag.putBoolean("playing", playing)
        tag.putDouble("seek", seek)
        tag.putByteArray("signalFrame", signal.save())
        if (!playing) {
            audioLine?.close()
            audioLine = null
        }
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)

        // Load other
        playing = tag.getBooleanOrNull("playing") ?: false
        seek = tag.getDoubleOrNull("seek") ?: 0.0
        signal.load(tag.getByteArrayOrNull("signalFrame"))
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
}