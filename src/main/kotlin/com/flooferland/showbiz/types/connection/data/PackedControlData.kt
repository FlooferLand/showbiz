package com.flooferland.showbiz.types.connection.data

import net.minecraft.nbt.*
import com.flooferland.showbiz.types.connection.ConnectionData
import com.flooferland.showbiz.utils.Extensions.getBooleanOrNull

data class PackedControlData(
    var instruction: CompoundTag = CompoundTag(),
) : ConnectionData("control") {
    override fun saveOrThrow(tag: CompoundTag) {
        tag.put("instruction", instruction)
    }

    override fun loadOrThrow(tag: CompoundTag) {
        instruction = tag.getCompound("instruction")
    }

    fun writeCurtain(open: Boolean) = instruction.putBoolean("curtain_open", open)
    fun readCurtain(): Boolean? = instruction.getBooleanOrNull("curtain_open")
    fun clearCurtain() = instruction.remove("curtain_open")
}
