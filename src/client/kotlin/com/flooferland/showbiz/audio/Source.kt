package com.flooferland.showbiz.audio

import com.flooferland.showbiz.Showbiz
import net.minecraft.world.phys.*
import com.flooferland.showbiz.network.packets.PlaybackChunkPacket
import com.flooferland.showbiz.types.FriendlyAudioFormat
import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL11.*

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
    val maxDistance = 18f
    var source = 0
    var lastReceivedChunkId = -1

    private fun getState(): Int {
        if (!isValidSource()) return -1
        return handleAL { alGetSourcei(source, AL_SOURCE_STATE) }
    }
    private fun isValidSource() = source != 0 && alIsSource(source)

    fun isOpen() = isValidSource()
    fun open() {
        if (isValidSource()) close()
        source = handleAL { alGenSources() }
        handleAL { alSourcei(source, AL_LOOPING, AL_FALSE) }
        handleAL { alDistanceModel(AL_LINEAR_DISTANCE) }
        handleAL { alSourcef(source, AL_MAX_DISTANCE, maxDistance) }
        handleAL { alSourcef(source, AL_REFERENCE_DISTANCE, maxDistance / 2f) }
        lastReceivedChunkId = -1
    }
    fun close() {
        if (!handleAL { alIsSource(source) }) return
        handleAL { alSourceStop(source) }

        val queued = handleAL { alGetSourcei(source, AL_BUFFERS_QUEUED) }
        if (queued > 0) {
            val buffers = BufferUtils.createIntBuffer(queued)
            handleAL { alSourceUnqueueBuffers(source, buffers) }
            handleAL { alDeleteBuffers(buffers) }
        }

        handleAL { alSourcei(source, AL_BUFFER, 0) }
        handleAL { alDeleteSources(source) }
        lastReceivedChunkId = -1
        source = 0
    }
    fun write(packet: PlaybackChunkPacket) {
        if (!isValidSource()) return
        updatePosition()

        // Checking chunk order
        if (packet.id <= lastReceivedChunkId && packet.id > 1) {
            Showbiz.log.warn("Found out of order audio chunk (${packet.id} <= $lastReceivedChunkId)")
        }
        lastReceivedChunkId = packet.id

        // Hard reset
        handleAL { alSourceStop(source) }

        // Cleaning buffers
        val queued = handleAL { alGetSourcei(source, AL_BUFFERS_QUEUED) }
        if (queued > 0) {
            val oldBuffers = BufferUtils.createIntBuffer(queued)
            handleAL { alSourceUnqueueBuffers(source, oldBuffers) }
            alDeleteBuffers(oldBuffers)
        }
        handleAL { alSourcei(source, AL_BUFFER, 0) }

        // Creating a new buffer
        val newBufId = handleAL { alGenBuffers() }
        val data = BufferUtils.createByteBuffer(packet.audioChunk.size)
            .put(packet.audioChunk)
            .flip()
        alBufferData(newBufId, format, data, packet.format.sampleRate)

        // Sets the buffer
        // !! Will fail if the window is resized, for whatever reason, so manual error handling re-creates the source and everything.
        if (alGetError() != AL_NO_ERROR) {
            close()
            open()
            return
        }

        handleAL { alSourceQueueBuffers(source, newBufId) }
        if (getState() != AL_PLAYING) {
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

    private fun <T> handleAL(block: () -> T): T {
        val res = block()
        checkAlError()
        return res
    }

    private fun checkAlError(): Boolean {
        val error = alGetError()
        if (error == AL_NO_ERROR) {
            return false
        }
        val stack = Thread.currentThread().stackTrace.slice(3..7).joinToString("\n")
        Showbiz.log.error("OpenAL error \"${formatAlError(error)}\": $stack")
        return true
    }

    private fun formatAlError(error: Int) = when (error) {
        AL_NO_ERROR -> "No error"
        AL_INVALID -> "Invalid"
        AL_INVALID_OPERATION -> "Invalid operation"
        AL_INVALID_ENUM -> "Invalid enum"
        AL_INVALID_NAME -> "Invalid name"
        AL_INVALID_VALUE -> "Invalid value"
        else -> "Error ID '$error'"
    }
}
