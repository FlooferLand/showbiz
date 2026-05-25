package com.flooferland.showbiz.audio

import net.minecraft.world.phys.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.network.packets.PlaybackChunkPacket
import com.flooferland.showbiz.types.FriendlyAudioFormat
import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL11.*
import org.lwjgl.openal.ALC10.alcGetCurrentContext

// TODO: Should probably avoid sending stereo audio to this cuz an extra channel means extra network bandwidth
//       OpenAL can only do mono (without exploding)

/** Low-level AL11 audio handling, because Minecraft's built-in audio streaming system is very dumb and undocumented */
class Source(val friendlyFormat: FriendlyAudioFormat, var position: Vec3? = null) {
    val format = run {
        val (bits, stereo, mono) = friendlyFormat.let { Triple(it.sampleBits, it.stereo, it.mono) }
        return@run when (bits) {
            8 if mono -> AL_FORMAT_MONO8
            8 if stereo -> AL_FORMAT_STEREO8
            16 if mono -> AL_FORMAT_MONO16
            16 if stereo -> AL_FORMAT_STEREO16
            else -> error("Unsupported format: $friendlyFormat")
        }
    }
    val maxDistance = 32f
    val paused get() = getState() == AL_PAUSED
    var lastReceivedChunkId = -1
    var source = 0
    var sourceContext = 0L

    private fun getState(): Int {
        if (!isValidSource()) return -1
        return handleAL { alGetSourcei(source, AL_SOURCE_STATE) }
    }
    private fun isValidSource() = source != 0 && handleALBasic { alIsSource(source) } && alcGetCurrentContext() == sourceContext

    fun isOpen() = isValidSource()
    fun open() {
        if (isValidSource()) close()
        source = handleAL { alGenSources() }
        sourceContext = handleAL { alcGetCurrentContext() }
        handleAL { alSourcei(source, AL_LOOPING, AL_FALSE) }
        handleAL { alDistanceModel(AL_LINEAR_DISTANCE) }
        handleAL { alSourcef(source, AL_MAX_DISTANCE, maxDistance) }
        handleAL { alSourcef(source, AL_REFERENCE_DISTANCE, maxDistance / 2f) }
        lastReceivedChunkId = -1
    }
    fun close() {
        if (isValidSource()) {
            if (getState() == AL_PLAYING) handleALBasic { alSourcePause(source) }
            handleALBasic { alSourceStop(source) }

            val queued = handleALBasic { alGetSourcei(source, AL_BUFFERS_QUEUED) }
            if (queued > 0) {
                val buffers = BufferUtils.createIntBuffer(queued)
                handleALBasic { alSourceUnqueueBuffers(source, buffers) }
                handleALBasic { alDeleteBuffers(buffers) }
            }

            handleALBasic { alSourcei(source, AL_BUFFER, 0) }
            handleALBasic { alDeleteSources(source) }
            alGetError()
        }
        lastReceivedChunkId = -1
        source = 0
    }
    fun write(packet: PlaybackChunkPacket) {
        if (!isValidSource()) return
        if (paused) return
        updatePosition()

        // Checking chunk order
        if (packet.id <= lastReceivedChunkId && packet.id > 1) {
            Showbiz.log.warn("Found out of order audio chunk (${packet.id} <= $lastReceivedChunkId)")
        }
        lastReceivedChunkId = packet.id

        // Hard reset
        handleAL { alSourceStop(source) }

        // Cleaning buffers
        val processedCount = handleAL { alGetSourcei(source, AL_BUFFERS_PROCESSED) }
        if (processedCount > 0) {
            val oldBuffers = BufferUtils.createIntBuffer(processedCount)
            handleAL { alSourceUnqueueBuffers(source, oldBuffers) }
            handleAL { alDeleteBuffers(oldBuffers) }
        }

        // Creating a new buffer
        val newBufId = handleAL { alGenBuffers() }
        val data = BufferUtils.createByteBuffer(packet.audioChunk.size)
            .put(packet.audioChunk)
            .flip()
        handleAL { alBufferData(newBufId, format, data, packet.format.sampleRate) }

        // Sets the buffer
        // !! Will fail if the window is resized, for whatever reason, so manual error handling re-creates the source and everything.
        if (alGetError() != AL_NO_ERROR) {
            source = 0
            open()
            return
        }

        handleAL { alSourceQueueBuffers(source, newBufId) }
        if (getState() != AL_PLAYING && !paused) {
            handleAL { alSourcePlay(source) }
        }
    }
    fun pause() {
        if (!isValidSource()) return
        if (getState() == AL_PLAYING) {
            handleAL { alSourcePause(source) }
        }
    }
    fun resume() {
        if (!isValidSource()) return
        if (getState() == AL_PAUSED) {
            handleAL { alSourcePlay(source) }
        }
    }

    fun updatePosition() {
        val pos = position
        if (pos != null) {
            handleAL { alSourcei(source, AL_SOURCE_RELATIVE, AL_FALSE) }
            handleAL { alSource3f(source, AL_POSITION, pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat()) }
        } else {
            handleAL { alSourcei(source, AL_SOURCE_RELATIVE, AL_TRUE) }
            handleAL { alSource3f(source, AL_POSITION, 0F, 0F, 0F) }
        }
    }

    private fun <T> handleALBasic(block: () -> T): T = handleAL(logOnly = true, block)
    private fun <T> handleAL(logOnly: Boolean = false, block: () -> T): T {
        val res = block()
        val error = checkAlError()
        if (error != null && !logOnly) {
            close()
            open()
        }
        return res
    }

    private fun checkAlError(): String? {
        val error = alGetError()
        if (error == AL_NO_ERROR) return null
        val stack = Thread.currentThread().stackTrace.slice(3..7).joinToString("\n")
        val formatted = formatAlError(error)
        Showbiz.log.error("OpenAL error \"$formatted\": $stack")
        return formatted
    }

    private fun formatAlError(error: Int) = when (error) {
        AL_NO_ERROR -> "No error"
        AL_INVALID -> "Invalid"
        AL_INVALID_OPERATION -> "Invalid operation"
        AL_INVALID_ENUM -> "Invalid enum"
        AL_INVALID_NAME -> "Invalid name"
        AL_INVALID_VALUE -> "Invalid value"
        else -> "OpenAL error ID '$error'"
    }
}
