package com.flooferland.showbiz.types

import net.minecraft.nbt.*

/** Can be written or read from an NBT compound */
interface ICompoundable {
    /** Saves data to a tag */
    fun save(tag: CompoundTag)

    /** Loads data from a tag */
    fun load(tag: CompoundTag)
}