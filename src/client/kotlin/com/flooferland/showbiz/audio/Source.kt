package com.flooferland.showbiz.audio

import com.flooferland.showbiz.Showbiz
import net.minecraft.client.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.types.FriendlyAudioFormat
import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL11.*

// TODO: Should probably avoid sending stereo audio to this cuz an extra channel means extra network bandwidth
//       OpenAL can only do mono (without exploding)

/** Low-level AL11 audio handling, because Minecraft's built-in audio streaming system is very dumb and undocumented */
class Source(val friendlyFormat: FriendlyAudioFormat, var position: Vec3? = null) {
    val buffers = IntArray(8)
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
    var nextBufIndex = 0

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
        handleAL { alGenBuffers(buffers) }
    }
    fun close() {
        if (handleAL { alIsSource(source) }) {
            handleAL { alSourceStop(source) }
            handleAL { alSourcei(source, AL_BUFFER, 0) }
            handleAL { alDeleteSources(source) }
            handleAL { alDeleteBuffers(buffers) }
        }
        source = 0
    }
    fun write(chunk: ByteArray, sampleRate: Int) {
        if (!isValidSource()) return
        updatePosition()

        // Cleaning buffers
        val processed = handleAL { alGetSourcei(source, AL_BUFFERS_PROCESSED) }
        if (processed > 0) {
            val buf = BufferUtils.createIntBuffer(processed)
            handleAL { alSourceUnqueueBuffers(source, buf) }
        }

        val bufId = buffers[nextBufIndex++ and (buffers.size - 1)]
        val buf = BufferUtils.createByteBuffer(chunk.size)
        buf.put(chunk)
        buf.flip()

        // Sets the buffer
        // !! Will fail if the window is resized, for whatever reason, so manual error handling re-creates the source and everything.
        alBufferData(bufId, format, buf, sampleRate)
        if (alGetError() != AL_NO_ERROR) {
            close()
            open()
            return
        }

        handleAL { alSourceQueueBuffers(source, bufId) }
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
