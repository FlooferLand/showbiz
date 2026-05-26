package com.flooferland.showbiz.types.connection.data

import net.minecraft.nbt.*
import net.minecraft.network.*
import com.flooferland.showbiz.types.connection.ConnectionData
import com.flooferland.showbiz.utils.Extensions.getBooleanOrNull
import com.flooferland.showbiz.utils.Extensions.getIntOrNull

data class PackedControlData(
    var instruction: CompoundTag = CompoundTag(),
) : ConnectionData<PackedControlData>("control") {
    override fun encode(buf: FriendlyByteBuf) {
        buf.writeNbt(instruction)
    }

    override fun decode(buf: FriendlyByteBuf) {
        instruction = buf.readNbt() ?: CompoundTag()
    }

    override fun tempReset() {
        instruction = CompoundTag()
    }

    fun writeCurtain(open: Boolean) = instruction.putBoolean("curtain_open", open)
    fun readCurtain(): Boolean? = instruction.getBooleanOrNull("curtain_open")
    fun clearCurtain() = instruction.remove("curtain_open")

    fun writeShowSelect(id: Int) = instruction.putInt("show_select", id)
    fun readShowSelect(): Int? = instruction.getIntOrNull("show_select")
    fun clearShowSelect() = instruction.remove("show_select")
}
