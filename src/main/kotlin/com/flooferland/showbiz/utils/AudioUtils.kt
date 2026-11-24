package com.flooferland.showbiz.utils

import com.flooferland.showbiz.types.AudioChannel
import com.flooferland.showbiz.types.FriendlyAudioFormat
import kotlin.math.abs

object AudioUtils {
    /** Extracts a specific channel from a stereo PCM byte array */
    fun extract(bytes: ByteArray, format: FriendlyAudioFormat, channel: AudioChannel): ByteArray? {
        if (!format.stereo || bytes.isEmpty()) return null
        if (format.sampleBits != 16 || !format.signed) return null

        val frameSize = 4
        if (bytes.size % frameSize != 0) return null

        val output = ByteArray(bytes.size / 2)

        for (offset in bytes.indices step frameSize) {
            val left = read16(bytes, offset, format.bigEndian)
            val right = read16(bytes, offset + 2, format.bigEndian)
            val value = when (channel) {
                AudioChannel.Left -> left
                AudioChannel.Right -> right
                AudioChannel.Mono -> ((left + right) / 2)
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            }
            write16(output, offset / 2, value, format.bigEndian)
        }
        return output
    }

    fun volumePeak(bytes: ByteArray, format: FriendlyAudioFormat, startByte: Int = 0, endByte: Int = bytes.size): Float {
        if (bytes.isEmpty() || format.sampleBits != 16 || !format.signed) return 0f
        val step = if (format.stereo) 4 else 2
        val start = (startByte.coerceIn(0, bytes.size) / step) * step
        val end = (endByte.coerceIn(start, bytes.size) / step) * step
        if (end <= start) return 0f

        var peak = 0
        for (offset in start until end step 2) {
            val abs = abs(read16(bytes, offset, format.bigEndian))
            if (abs > peak) peak = abs
        }
        return (peak / Short.MAX_VALUE.toFloat()).coerceIn(0f, 1f)
    }

    /** @throws IndexOutOfBoundsException */
    private fun read16(bytes: ByteArray, offset: Int, bigEndian: Boolean): Int {
        val lo = bytes[offset].toInt() and 0xff
        val hi = bytes[offset + 1].toInt() and 0xff
        return (if (bigEndian) (lo shl 8) or hi else (hi shl 8) or lo).toShort().toInt()
    }

    /** @throws IndexOutOfBoundsException */
    private fun write16(bytes: ByteArray, offset: Int, value: Int, bigEndian: Boolean) {
        val sample = value.toShort().toInt()
        if (bigEndian) {
            bytes[offset] = (sample ushr 8).toByte()
            bytes[offset + 1] = sample.toByte()
        } else {
            bytes[offset] = sample.toByte()
            bytes[offset + 1] = (sample ushr 8).toByte()
        }
    }
}