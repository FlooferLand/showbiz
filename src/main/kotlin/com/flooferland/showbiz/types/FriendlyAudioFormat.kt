package com.flooferland.showbiz.types

import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioFormat.Encoding
import kotlin.math.roundToInt

/** Better than Java's stinky dinky 30 decade year old AudioFormat :tm: */
data class FriendlyAudioFormat(var sampleRate: Int = 44_100, var sampleBits: Int = 16, var stereo: Boolean = false, var signed: Boolean = true, var bigEndian: Boolean = false) : IUnsafeCompoundable {
    val mono: Boolean get() = !stereo
    val channels: Byte get() = if (stereo) 2 else 1

    override fun saveOrThrow(tag: CompoundTag) {
        tag.putInt("sample_rate", sampleRate)
        tag.putByte("sample_bits", sampleBits.toByte())
        tag.putBoolean("stereo", stereo)
        tag.putBoolean("signed", signed)
        tag.putBoolean("big_endian", bigEndian)
    }

    override fun loadOrThrow(tag: CompoundTag) {
        tag.getInt("sample_rate").also { sampleRate = it }
        tag.getByte("sample_bits").also { sampleBits = it.toInt() }
        tag.getBoolean("stereo").also { stereo = it }
        tag.getBoolean("signed").also { signed = it }
        tag.getBoolean("big_endian").also { bigEndian = it }
    }

    companion object {
        fun AudioFormat.toFriendly() = FriendlyAudioFormat(
            sampleRate = this.sampleRate.roundToInt(),
            sampleBits = this.sampleSizeInBits,
            stereo = this.channels == 2,
            signed = this.encoding == Encoding.PCM_SIGNED,
            bigEndian = this.isBigEndian
        )

        val codec = StreamCodec.of<FriendlyByteBuf, FriendlyAudioFormat>(
            { buf, format ->
                buf.writeInt(format.sampleRate)
                buf.writeByte(format.sampleBits)
                buf.writeBoolean(format.stereo)
                buf.writeBoolean(format.signed)
                buf.writeBoolean(format.bigEndian)
            },
            { buf ->
                FriendlyAudioFormat(
                    buf.readInt(),
                    buf.readByte().toInt(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean()
                )
            }
        )!!
    }
}