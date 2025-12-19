package com.flooferland.showbiz.types

import net.minecraft.nbt.*

/** Can be written or read from an NBT compound.
 * # UNSAFE, CAN THROW!!
 */
interface IUnsafeCompoundable {
    /** Saves data to a tag */
    @Throws fun saveOrThrow(tag: CompoundTag)

    /** Loads data from a tag */
    @Throws fun loadOrThrow(tag: CompoundTag)
}