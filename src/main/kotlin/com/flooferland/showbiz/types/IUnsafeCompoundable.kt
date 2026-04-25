package com.flooferland.showbiz.types

import net.minecraft.nbt.*

/** Can be written or read from an NBT compound.
 * # UNSAFE, CAN THROW!!
 */
interface IUnsafeCompoundable : ICompoundable {
    /** Saves data to a tag */
    @Throws fun saveOrThrow(tag: CompoundTag)

    /** Loads data from a tag */
    @Throws fun loadOrThrow(tag: CompoundTag)

    override fun save(tag: CompoundTag) {
        runCatching { saveOrThrow(tag) }
    }
    override fun load(tag: CompoundTag) {
        runCatching { loadOrThrow(tag) }
    }
}