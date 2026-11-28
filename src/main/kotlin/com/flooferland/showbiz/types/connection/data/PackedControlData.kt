package com.flooferland.showbiz.types.connection.data

import com.flooferland.showbiz.types.connection.ConnectionData
import com.flooferland.showbiz.utils.Extensions.getBooleanOrNull
import com.flooferland.showbiz.utils.Extensions.getCompoundOrNull
import com.flooferland.showbiz.utils.Extensions.getFloatOrNull
import com.flooferland.showbiz.utils.Extensions.getIntOrNull
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf

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

    fun clearCurtain() {
        instruction.remove("curtain")
    }
    fun writeCurtain(open: Boolean?, speed: Float) {
        instruction.put("curtain", CompoundTag().also { tag ->
            open?.let { tag.putBoolean("open", it) }
            tag.putFloat("speed", speed)
        })
    }
    fun readCurtain(): Pair<Boolean?, Float>? {
        return instruction.getCompoundOrNull("curtain")?.let { tag ->
            val open = tag.getBooleanOrNull("open")
            val speed = tag.getFloatOrNull("speed") ?: return@let null
            return@let Pair(open, speed)
        }
    }

    fun clearShowSelect() = instruction.remove("show_select")
    fun writeShowSelect(id: Int) = instruction.putInt("show_select", id)
    fun readShowSelect(): Int? = instruction.getIntOrNull("show_select")
}
