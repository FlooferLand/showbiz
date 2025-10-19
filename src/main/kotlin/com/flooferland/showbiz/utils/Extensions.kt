package com.flooferland.showbiz.utils

import net.minecraft.core.component.*
import net.minecraft.nbt.*
import net.minecraft.resources.*
import net.minecraft.world.item.*
import net.minecraft.world.level.block.entity.*

@Suppress("unused")
object Extensions {
    fun ResourceLocation.blockPath(): ResourceLocation {
        return this.withPrefix("block/");
    }
    fun ResourceLocation.itemPath(): ResourceLocation {
        return this.withPrefix("item/");
    }
    fun <T> ItemStack.applyComponent(type: DataComponentType<T>, comp: T) {
        this.applyComponents(DataComponentPatch.builder().set(type, comp!!).build())
    }

    /** Calls setChanged and sendBlockUpdated */
    fun BlockEntity.markDirtyNotifyAll() {
        setChanged()
        level?.sendBlockUpdated(this.blockPos, blockState, blockState, 0)
    }

    @DslMarker annotation class BlockEntityApplyDsl;
    @BlockEntityApplyDsl
    fun <T: BlockEntity> T.applyChange(rerender: Boolean, change: T.() -> Unit) {
        change(this)
        markDirtyNotifyAll()
    }

    //region Compound get functions, since these change for 1.21.5+
    fun CompoundTag.getBooleanOrNull(string: String): Boolean? =
        if (contains(string)) getBoolean(string) else null
    fun CompoundTag.getByteOrNull(string: String): Byte? =
        if (contains(string)) getByte(string) else null
    fun CompoundTag.getIntOrNull(string: String): Int? =
        if (contains(string)) getInt(string) else null
    fun CompoundTag.getFloatOrNull(string: String): Float? =
        if (contains(string)) getFloat(string) else null
    fun CompoundTag.getDoubleOrNull(string: String): Double? =
        if (contains(string)) getDouble(string) else null
    fun CompoundTag.getStringOrNull(string: String): String? =
        if (contains(string)) getString(string) else null
    fun CompoundTag.getIntArrayOrNull(string: String): IntArray? =
        if (contains(string)) getIntArray(string) else null
    fun CompoundTag.getByteArrayOrNull(string: String): ByteArray? =
        if (contains(string)) getByteArray(string) else null
    //endregion
}