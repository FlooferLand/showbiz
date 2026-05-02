package com.flooferland.showbiz.types.connection.data

import net.minecraft.nbt.*
import com.flooferland.showbiz.show.SignalFrame
import com.flooferland.showbiz.types.connection.ConnectionData

data class PackedShowData(
    var playing: Boolean = false,
    val signal: SignalFrame = SignalFrame(),
    var mapping: String? = null
) : ConnectionData<PackedShowData>("show") {
    override fun saveOrThrow(tag: CompoundTag) {
        tag.putBoolean("playing", playing)
        tag.putIntArray("signal", signal.save())
        mapping?.let { tag.putString("mapping", it) }
    }

    override fun loadOrThrow(tag: CompoundTag) {
        playing = tag.getBoolean("playing")
        signal.load(tag.getIntArray("signal"))
        mapping = tag.getString("mapping")
    }

    override fun tempReset() {
        signal.reset()
    }

    override fun merge(other: PackedShowData): Boolean {
        signal += other.signal
        playing = playing || other.playing
        if (mapping == null) mapping = other.mapping
        return true
    }
}
