package com.flooferland.showbiz.utils

import com.flooferland.showbiz.types.AudioChannel
import com.flooferland.showbiz.types.FriendlyAudioFormat

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

    private fun read16(bytes: ByteArray, offset: Int, bigEndian: Boolean): Int {
        val lo = bytes[offset].toInt() and 0xff
        val hi = bytes[offset + 1].toInt() and 0xff
        return (if (bigEndian) (lo shl 8) or hi else (hi shl 8) or lo).toShort().toInt()
    }

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