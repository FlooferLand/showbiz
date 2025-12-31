package com.flooferland.showbiz.utils

import net.minecraft.core.component.*
import net.minecraft.nbt.*
import net.minecraft.resources.*
import net.minecraft.world.item.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.phys.Vec3
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.cache.`object`.GeoBone

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

    //region GeckoLib
    fun GeoBone.getChildrenFlattened(): HashSet<GeoBone> {
        val bones = hashSetOf<GeoBone>(this)
        this.childBones.forEach { bones.addAll(it.getChildrenFlattened()) }
        return bones
    }

    fun BakedGeoModel.getAllBones(): HashSet<GeoBone> {
        val bones = hashSetOf<GeoBone>()
        topLevelBones.forEach {
            bones.add(it)
            bones.addAll(it.getChildrenFlattened())
        }
        return bones
    }
    //endregion

    //region Compound get functions, since these change for 1.21.5+
    fun CompoundTag.getOrNull(key: String)          = if (contains(key)) get(key) else null
    fun CompoundTag.getCompoundOrNull(key: String)  = if (contains(key)) getCompound(key) else null
    fun CompoundTag.getBooleanOrNull(key: String)   = if (contains(key)) getBoolean(key) else null
    fun CompoundTag.getByteOrNull(key: String)      = if (contains(key)) getByte(key) else null
    fun CompoundTag.getIntOrNull(key: String)       = if (contains(key)) getInt(key) else null
    fun CompoundTag.getLongOrNull(key: String)      = if (contains(key)) getLong(key) else null
    fun CompoundTag.getFloatOrNull(key: String)     = if (contains(key)) getFloat(key) else null
    fun CompoundTag.getDoubleOrNull(key: String)    = if (contains(key)) getDouble(key) else null
    fun CompoundTag.getStringOrNull(key: String)    = if (contains(key)) getString(key) else null
    fun CompoundTag.getIntArrayOrNull(key: String)  = if (contains(key)) getIntArray(key) else null
    fun CompoundTag.getByteArrayOrNull(key: String) = if (contains(key)) getByteArray(key) else null
    fun CompoundTag.getLongArrayOrNull(key: String) = if (contains(key)) getLongArray(key) else null
    fun CompoundTag.getUUIDOrNull(key: String)      = if (contains(key)) getUUID(key) else null
    //endregion

    fun Vec3.divide(factor: Double) = Vec3(x / factor, y / factor, z / factor)
}