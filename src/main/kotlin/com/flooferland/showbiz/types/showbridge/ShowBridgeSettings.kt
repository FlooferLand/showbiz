package com.flooferland.showbiz.types.showbridge

import net.minecraft.nbt.*
import com.flooferland.showbiz.types.ICompoundable

sealed class ShowBridgeSettings : ICompoundable {
    class TextList() : ShowBridgeSettings() {
        override fun save(tag: CompoundTag) {

        }
        override fun load(tag: CompoundTag) {

        }
    }
    class BinaryList() : ShowBridgeSettings() {
        override fun save(tag: CompoundTag) {

        }
        override fun load(tag: CompoundTag) {

        }
    }
}
