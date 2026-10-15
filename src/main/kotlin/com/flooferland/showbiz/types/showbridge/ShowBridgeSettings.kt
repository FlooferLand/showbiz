package com.flooferland.showbiz.types.showbridge

import net.minecraft.nbt.*
import com.flooferland.showbiz.types.ICompoundable
import com.flooferland.showbiz.utils.Extensions.getStringOrNull

sealed class ShowBridgeSettings : ICompoundable {
    class TextList(var separator: String = ",") : ShowBridgeSettings() {
        override fun save(tag: CompoundTag) {
            tag.putString("separator", separator)
        }
        override fun load(tag: CompoundTag) {
            tag.getStringOrNull("separator")?.let { separator = it }
        }
    }
    class BinaryList() : ShowBridgeSettings() {
        override fun save(tag: CompoundTag) {

        }
        override fun load(tag: CompoundTag) {

        }
    }
}
