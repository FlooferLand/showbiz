package com.flooferland.showbiz.types.connection.data

import net.minecraft.nbt.*
import com.flooferland.showbiz.show.SignalFrame
import com.flooferland.showbiz.types.connection.ConnectionData

data class PackedShowData(
    var playing: Boolean = false,
    val signal: SignalFrame = SignalFrame(),
    var mapping: String? = null
) : ConnectionData("show") {
    override fun saveOrThrow(tag: CompoundTag) {
        tag.putBoolean("playing", playing)
        tag.putIntArray("signal", signal.save())
        mapping?.let { tag.putString("mapping", mapping) }
    }

    override fun loadOrThrow(tag: CompoundTag) {
        playing = tag.getBoolean("playing")
        signal.load(tag.getIntArray("signal"))
        mapping = tag.getString("mapping")
    }
}
