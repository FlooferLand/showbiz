package com.flooferland.showbiz.entities

import com.flooferland.showbiz.Permissions.Companion.mayHurt
import com.flooferland.showbiz.entities.base.BasePropEntity
import com.flooferland.showbiz.registry.ModComponents
import com.flooferland.showbiz.registry.ModItems
import com.flooferland.showbiz.registry.ModLivingEntities
import com.flooferland.showbiz.types.ResourceId
import com.flooferland.showbiz.utils.Extensions.getStringOrNull
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EntityDimensions
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
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
    private var itemStack = this.defaultItem

    init {
        this.propId = getComp()?.id?.let { ResourceId.of(it) }
        refreshDimensions()
        updatePersistentData { addAdditionalSaveData(it) }
    }

    override fun getDefaultDimensions(pose: Pose): EntityDimensions = EntityDimensions.fixed(0.3f, 0.3f)
    override fun getPickResult() = makeItem()

    fun getComp() = itemStack.copy().get(ModComponents.Prop.type)
    fun makeItem() = itemStack.copyWithCount(1)!!

    fun drop() {
        val pos = position() ?: return
        val level = level() ?: return
        val item = ItemEntity(level, pos.x, pos.y + 0.5, pos.z, makeItem())
        level.addFreshEntity(item)
    }

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        val attacker = source.entity as? Player ?: return false
        if (!attacker.mayHurt(this) || isInvulnerableTo(source)) return false
        fun playSound(event: SoundEvent) {
            level().playSound(null, blockPosition(), event, SoundSource.BLOCKS, 1.0f, 1.0f)
        }

        if (attacker.isCreative && amount > 0f) {
            playSound(SoundEvents.ARMOR_STAND_BREAK)
            playSound(SoundEvents.ANVIL_HIT)
            remove(RemovalReason.DISCARDED)
        } else {
            playSound(SoundEvents.ARMOR_STAND_BREAK)
            drop()
        }
        return true
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