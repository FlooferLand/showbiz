package com.flooferland.showbiz.audio

import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.utils.ShowbizUtils
import net.minecraft.client.*
import net.minecraft.world.phys.*
import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL11.*
import org.lwjgl.openal.ALC10.alcGetCurrentContext
import javax.sound.sampled.AudioFormat

// TODO: Should probably avoid sending stereo audio to this cuz an extra channel means extra network bandwidth
//       OpenAL can only do mono (without exploding)

/** Low-level AL11 audio handling, because Minecraft's built-in audio streaming system is very dumb and undocumented */
class Source(val javaFormat: AudioFormat, var position: Vec3? = null) {
    val buffers = IntArray(4)
    val format = run {
        val (bits, channels) = javaFormat.let { Pair(it.sampleSizeInBits, it.channels) }
        return@run when (bits) {
            8 if channels == 1 -> AL_FORMAT_MONO8
            8 if channels == 2 -> AL_FORMAT_STEREO8
            16 if channels == 1 -> AL_FORMAT_MONO16
            16 if channels == 2 -> AL_FORMAT_STEREO16
            else -> error("Unsupported format: $javaFormat")
        }
    }
    val maxDistance = 16f
    var source = 0
    var nextBufIndex = 0
    var context = alcGetCurrentContext()

    private fun getState(): Int {
        if (!isValidSource()) return -1
        return handleAL { alGetSourcei(source, AL_SOURCE_STATE) }
    }
    private fun isValidSource() = source != 0 && alIsSource(source)

    fun isOpen() = isValidSource()

    fun open() {
        if (isValidSource()) close()
        context = handleAL { alcGetCurrentContext() }
        source = handleAL { alGenSources() }
        handleAL { alSourcei(source, AL_LOOPING, AL_FALSE) }
        handleAL { alDistanceModel(AL_LINEAR_DISTANCE) }
        handleAL { alSourcef(source, AL_MAX_DISTANCE, maxDistance) }
        handleAL { alSourcef(source, AL_REFERENCE_DISTANCE, 0f) }
        handleAL { alGenBuffers(buffers) }
    }
    fun close() {
        if (handleAL { alIsSource(source) }) {
            if (getState() == AL_PLAYING) {
                handleAL { alSourceStop(source) }
            }
            handleAL { alDeleteSources(source) }
            handleAL { alDeleteBuffers(buffers) }
        }
        source = 0
    }
    fun write(chunk: ByteArray, sampleRate: Int) {
        if (!isValidSource() || !isValidContext()) return
        updateAttenuation()
        updatePosition()

        // Cleaning buffers
        val processed = handleAL { alGetSourcei(source, AL_BUFFERS_PROCESSED) }
        if (processed > 0) {
            val buf = BufferUtils.createIntBuffer(processed)
            handleAL { alSourceUnqueueBuffers(source, buf) }
        }

        val targetFormat = javaFormat.let {
            AudioFormat(
                it.sampleRate,
                it.sampleSizeInBits,
                1,
                true,
                it.isBigEndian
            )
        }
        val chunk = if (javaFormat.channels == 2) ShowbizUtils.convertAudio(chunk, javaFormat, targetFormat) else chunk
        val format = when (format) {
            AL_FORMAT_STEREO16 -> AL_FORMAT_MONO16
            AL_FORMAT_STEREO8 -> AL_FORMAT_MONO8
            else -> format
        }

        val bufId = buffers[nextBufIndex++ and (buffers.size - 1)]
        val buf = BufferUtils.createByteBuffer(chunk.size)
        buf.put(chunk)
        buf.flip()
        handleAL { alBufferData(bufId, format, buf, sampleRate) }
        handleAL { alSourceQueueBuffers(source, bufId) }
        if (getState() != AL_PLAYING) {
            handleAL { alSourcePlay(source) }
        }
    }

    fun updatePosition() {
        val mc = Minecraft.getInstance() ?: return
        val camera = mc.gameRenderer?.mainCamera ?: return

        val camPos = camera.position
        handleAL { alListener3f(AL_POSITION, camPos.x.toFloat(), camPos.y.toFloat(), camPos.z.toFloat()) }

        val look = camera.lookVector
        val up = camera.upVector
        handleAL { alListenerfv(AL_ORIENTATION, floatArrayOf(look.x, look.y, look.z, up.x, up.y, up.z)) }

        val pos = position
        if (pos != null) {
            handleAL { alSourcei(source, AL_SOURCE_RELATIVE, AL_FALSE) }
            handleAL { alSource3f(source, AL_POSITION, pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat()) }
        } else {
            handleAL { alSourcei(source, AL_SOURCE_RELATIVE, AL_TRUE) }
            handleAL { alSource3f(source, AL_POSITION, 0F, 0F, 0F) }
        }
    }

    fun updateAttenuation() {
        handleAL { alDistanceModel(AL_LINEAR_DISTANCE) }
        handleAL { alSourcef(source, AL_MAX_DISTANCE, maxDistance) }
        handleAL { alSourcef(source, AL_REFERENCE_DISTANCE, maxDistance / 2f) }
    }

    private fun isValidContext(): Boolean {
        val current = alcGetCurrentContext()
        if (current == 0L) return false
        if (current != context) {
            context = current

            // Re-opening in case the context changed
            if (source != 0) {
                close()
                open()
            }
        }
        return true
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
        Showbiz.log.error("OpenAL error \"${formatAlError(error)}\": ${stack}")
        return true
    }

    private fun formatAlError(error: Int) = when (error) {
        AL_INVALID -> "Invalid"
        AL_INVALID_OPERATION -> "Invalid operation"
        AL_INVALID_ENUM -> "Invalid enum"
        AL_INVALID_NAME -> "Invalid name"
        AL_INVALID_VALUE -> "Invalid value"
        else -> "Error ID '$error'"
    }
}
