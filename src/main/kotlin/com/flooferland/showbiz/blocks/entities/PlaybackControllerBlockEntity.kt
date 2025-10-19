package com.flooferland.showbiz.blocks.entities

import com.flooferland.bizlib.RawShowData
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.show.SignalFrame
import com.flooferland.showbiz.utils.Extensions.getBooleanOrNull
import com.flooferland.showbiz.utils.Extensions.getDoubleOrNull
import com.flooferland.showbiz.utils.Extensions.getIntArrayOrNull
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
    public var show: RawShowData? = null
    public var playing = false
    public var seek = 0.0
    public val signal = SignalFrame()
    public val audio = byteArrayOf()

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
        if (!playing || show == null) return
        seek += 1
        val seekInt = seek.roundToInt()
        var shouldUpdate = false

        // Signal
        run {
            // TODO: Fix signal playback, not working currently
            // Split (idk if this works)
            /*val start = seekInt * 2
            val end = (start + 2).coerceAtMost(show?.signal?.size ?: 0)
            val sig = show?.signal?.sliceArray(start until end) ?: intArrayOf()
            signal.setFrom(sig)*/

            // Combined
            /*val sig = show?.signal[seekInt]
            sig?.let { signal.setFromOne(it) }*/

            shouldUpdate = true
            println("bit: ${signal.arr()[0]} ${signal.arr()[1]}")
        }

        // Initializing audio
        if (audioLine == null || audioLine?.isOpen != true) {
            aFormat = AudioFormat(aSampleRate.toFloat(), aSampleBits, aChannels, true, false)
            audioLine = ShowbizUtils.startAudioDevice(aFormat!!, aBufferSize).getOrNull()
        }

        // Playing back audio
        if (bytesWritten < (show?.audio?.size ?: 0)) {
            var toWrite = aBufferSize
            toWrite -= toWrite % aFormat!!.frameSize // align to frame

            val remain = (show?.audio?.size ?: 0) - bytesWritten
            if (toWrite > remain) toWrite = remain

            if (toWrite > 0) {
                val chunk = show?.audio?.copyOfRange(bytesWritten, bytesWritten + toWrite)
                audioLine?.write(chunk, 0, chunk?.size ?: 0)
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
        tag.putIntArray("signalFrame", signal.arr())
        //tag.putByteArray("audioChunk", audio)
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
        signal.setFrom(tag.getIntArrayOrNull("signalFrame"))
        //audio = tag.getByteArrayOrNull("audioChunk") ?: byteArrayOf()
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
            // tag.putInt("signal", signal)
            tag
        }
}