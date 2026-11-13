package com.flooferland.showbiz.entities

import com.flooferland.showbiz.components.PropComponent
import com.flooferland.showbiz.entities.base.BasePropEntity
import com.flooferland.showbiz.registry.ModComponents
import com.flooferland.showbiz.registry.ModItems
import com.flooferland.showbiz.registry.ModLivingEntities
import com.flooferland.showbiz.types.ResourceId
import com.flooferland.showbiz.utils.Extensions.getStringOrNull
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntityDimensions
import net.minecraft.world.entity.Pose
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

class PropEntity(level: Level, defaultItem: ItemStack) : BasePropEntity(ModLivingEntities.Prop.type, level), GeoEntity {
    constructor(level: Level) : this(level, ModItems.Prop.item.defaultInstance!!)

    val cache = GeckoLibUtil.createInstanceCache(this)!!
    override fun getAnimatableInstanceCache() = cache
    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {}

    var propId: ResourceId? = null
        set(value) {
            field = value
            if (level() is ServerLevel) updatePersistentData { it.putString("prop_id", value?.toString() ?: "") }
        }

    val defaultItem = defaultItem.copyWithCount(1)!!
    init {
        this.propId = defaultItem.get(ModComponents.Prop.type)?.id?.let { ResourceId.of(it) }
        refreshDimensions()
        updatePersistentData { addAdditionalSaveData(it) }
    }

    override fun getDefaultDimensions(pose: Pose): EntityDimensions = EntityDimensions.fixed(0.3f, 0.3f)

    override fun makeItem(): ItemStack = defaultItem.copy().also { stack ->
        propId?.let { stack.set(ModComponents.Prop.type, PropComponent(it.toLocation())) }
    }

    override fun addAdditionalSaveData(tag: CompoundTag) {
        tag.putString("prop_id", propId?.toString() ?: "")
        super.addAdditionalSaveData(tag)
    }
    override fun readAdditionalSaveData(tag: CompoundTag) {
        propId = tag.getStringOrNull("prop_id")?.let { if (it.isNotBlank()) ResourceId.of(it) else null }
        super.readAdditionalSaveData(tag)
    }
}