package com.flooferland.showbiz.entities.base

import com.flooferland.showbiz.Permissions.Companion.mayHurt
import com.flooferland.showbiz.utils.Extensions.handItem
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.DamageTypeTags
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.*
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

open class BasePropEntity(entityType: EntityType<out LivingEntity>, level: Level) : LivingEntity(entityType, level) {
    private var killDelayTicks = 0

    open fun onBreak(attacker: ServerPlayer) {}
    open fun makeItem(): ItemStack = ItemStack.EMPTY
    open fun drop() {
        val pos = position() ?: return
        val level = level() ?: return
        val itemStack = makeItem().also { if (it.isEmpty) return }
        val item = ItemEntity(level, pos.x, pos.y + 0.5, pos.z, itemStack)
        level.addFreshEntity(item)
    }
    open fun playPlaceSound(player: Player) {
        level().playSound(null, blockPosition(), SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 0.5f, 1.0f)
    }
    open fun playBreakSound(player: Player) {
        level().playSound(null, blockPosition(), SoundEvents.ARMOR_STAND_BREAK, SoundSource.BLOCKS, 0.3f, 1.0f)
        if (player.isCreative) {
            level().playSound(null, blockPosition(), SoundEvents.METAL_BREAK, SoundSource.BLOCKS, 0.5f, 1.0f)
        }
    }

    override fun tick() {
        super.tick()
        if (killDelayTicks > 0) {
            killDelayTicks -= 1
            if (killDelayTicks == 0) {
                remove(RemovalReason.DISCARDED)
                return
            }
        }
    }

    // Ugly base stuff
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

    override fun isInvulnerable() = false
    override fun isInvulnerableTo(source: DamageSource) =
        !source.`is`(DamageTypeTags.BYPASSES_INVULNERABILITY) &&
                (source.entity?.let { skipAttackInteraction(it) } ?: true)
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

    fun grab(player: Player): InteractionResult {
        if (killDelayTicks > 0 || isRemoved) return InteractionResult.PASS
        player.handItem(makeItem())
        remove(RemovalReason.DISCARDED)
        return InteractionResult.SUCCESS
    }
    override fun getPickResult() = makeItem()
    override fun hurt(source: DamageSource, amount: Float): Boolean {
        if (killDelayTicks > 0 || isRemoved) return false
        if (isInvulnerableTo(source)) return false
        val attacker = source.entity as? ServerPlayer ?: run {
            if (!level().isClientSide) remove(RemovalReason.KILLED)
            return true
        }

        if (amount > 0f) {
            playBreakSound(attacker)
            if (!attacker.isCreative) drop()
            onBreak(attacker)
            killDelayTicks = 2
        }
        return true
    }

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