package com.flooferland.showbiz.types

import net.minecraft.network.*
import net.minecraft.network.codec.*
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioFormat.Encoding
import kotlin.math.roundToInt

/** Better than Java's stinky dinky 30 decade year old AudioFormat :tm: */
data class FriendlyAudioFormat(var sampleRate: Int = 44_100, var sampleBits: Int = 16, var stereo: Boolean = false, var signed: Boolean = true, var bigEndian: Boolean = false) : IPacketable {
    val mono: Boolean get() = !stereo
    val channels: Byte get() = if (stereo) 2 else 1

    override fun encode(buf: FriendlyByteBuf) {
        buf.writeInt(sampleRate)
        buf.writeInt(sampleBits)
        buf.writeBoolean(stereo)
        buf.writeBoolean(signed)
        buf.writeBoolean(bigEndian)
    }

    override fun decode(buf: FriendlyByteBuf) {
        sampleRate = buf.readInt()
        sampleBits = buf.readInt()
        stereo = buf.readBoolean()
        signed = buf.readBoolean()
        bigEndian = buf.readBoolean()
        if (sampleRate == 0) reset()
    }

    fun reset() {
        sampleRate = 44_100
        sampleBits = 16
        stereo = false
        signed = true
        bigEndian = false
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