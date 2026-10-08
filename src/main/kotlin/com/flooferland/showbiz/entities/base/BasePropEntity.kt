package com.flooferland.showbiz.entities.base

import net.minecraft.nbt.*
import net.minecraft.network.syncher.*
import net.minecraft.world.damagesource.*
import net.minecraft.world.effect.*
import net.minecraft.world.entity.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.item.*
import net.minecraft.world.level.*
import com.flooferland.showbiz.Permissions.Companion.mayHurt

open class BasePropEntity(entityType: EntityType<out LivingEntity>, level: Level) : LivingEntity(entityType, level) {
    override fun isPushable() = false
    override fun isPickable() = true
    override fun isAttackable() = true
    override fun fireImmune() = true
    override fun canBeCollidedWith() = true
    override fun canBeHitByProjectile() = true
    override fun canBeAffected(effect: MobEffectInstance) = false
    override fun canBeSeenAsEnemy() = false
    override fun getDefaultDimensions(pose: Pose): EntityDimensions = EntityDimensions.fixed(0.1f, 0.1f)
    override fun showVehicleHealth() = false

    override fun isInvulnerableTo(source: DamageSource) =
        source.entity?.let { skipAttackInteraction(it) } ?: true
    override fun skipAttackInteraction(entity: Entity) =
        (entity as? Player)?.let { !it.mayHurt(this) } ?: true

    // region | LivingEntity stuff
    override fun getMainArm() = HumanoidArm.RIGHT
    override fun getArmorSlots() = listOf<ItemStack>()
    override fun getItemBySlot(slot: EquipmentSlot): ItemStack = ItemStack.EMPTY
    override fun setItemSlot(slot: EquipmentSlot, stack: ItemStack) {}
    override fun isNoGravity() = true
    override fun isPushedByFluid() = false
    override fun knockback(strength: Double, x: Double, z: Double) {}
    override fun getXRot() = 0f
    // endregion

    fun updatePersistentData(block: (CompoundTag) -> Unit) {
        val tag = entityData.get(persistentDataAccessor).copy()
        block(tag)
        entityData.set(persistentDataAccessor, tag)
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(persistentDataAccessor, CompoundTag())
    }

    override fun onSyncedDataUpdated(dataAccessor: EntityDataAccessor<*>) {
        super.onSyncedDataUpdated(dataAccessor)
        if (dataAccessor == persistentDataAccessor && level().isClientSide) {
            val tag = entityData.get(persistentDataAccessor)
            readAdditionalSaveData(tag)
        }
    }

    override fun addAdditionalSaveData(tag: CompoundTag) {}
    override fun readAdditionalSaveData(tag: CompoundTag) {
        if (!level().isClientSide) entityData.set(persistentDataAccessor, tag)
    }
    companion object {
        val persistentDataAccessor = SynchedEntityData.defineId(BasePropEntity::class.java, EntityDataSerializers.COMPOUND_TAG)!!
    }
}