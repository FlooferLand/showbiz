package com.flooferland.showbiz.types.connection.data

import net.minecraft.network.*
import net.minecraft.network.codec.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.show.SignalFrame
import com.flooferland.showbiz.types.connection.ConnectionData

data class PackedShowData(
    var playing: Boolean = false,
    val signal: SignalFrame = SignalFrame(),
    var mapping: String? = null
) : ConnectionData<PackedShowData>("show") {
    override fun encode(buf: FriendlyByteBuf) {
        signal.saveTo(buf)
        buf.writeBoolean(playing)
        buf.writeNullable(mapping, ByteBufCodecs.STRING_UTF8)
    }

    override fun decode(buf: FriendlyByteBuf) {
        signal.loadFrom(buf)
        playing = buf.readBoolean()
        mapping = buf.readNullable(ByteBufCodecs.STRING_UTF8)
    }

    override fun tempReset() {
        signal.reset()
        playing = false
    }

    override fun merge(other: PackedShowData): Boolean {
        signal += other.signal
        playing = playing || other.playing
        mapping = other.mapping
        if (mapping !in Showbiz.charts.ids)
            mapping = null
        return true
    }
}
