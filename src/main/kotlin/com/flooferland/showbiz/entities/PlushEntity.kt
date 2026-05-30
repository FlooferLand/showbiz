package com.flooferland.showbiz.entities

import net.minecraft.nbt.*
import net.minecraft.network.syncher.*
import net.minecraft.server.level.*
import net.minecraft.sounds.*
import net.minecraft.world.*
import net.minecraft.world.damagesource.*
import net.minecraft.world.entity.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.item.*
import net.minecraft.world.level.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.items.PlushItem
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.registry.ModEntities
import com.flooferland.showbiz.registry.ModSounds
import com.flooferland.showbiz.utils.Extensions.getOrNull
import com.flooferland.showbiz.utils.Extensions.handItem
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil
import kotlin.jvm.optionals.getOrNull

class PlushEntity(level: Level, itemStack: ItemStack) : Entity(ModEntities.Plush.type, level), GeoEntity {
    constructor(level: Level) : this(level, defaultItem)
    val cache = GeckoLibUtil.createInstanceCache(this)!!
    override fun getAnimatableInstanceCache() = cache
    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar?) = Unit

    override fun getDimensions(pose: Pose): EntityDimensions {
        return EntityDimensions.fixed(0.4f, 0.75f)
    }

    var itemStack: ItemStack
        get() = entityData.get(itemStackAccessor)
        set(value) { entityData.set(itemStackAccessor, value) }

    init {
        this.itemStack = itemStack
        refreshDimensions()
    }

    override fun isInvulnerable() = true
    override fun isPushable() = false
    override fun isPickable() = true
    override fun isAttackable() = true
    override fun canBeCollidedWith() = true
    override fun canBeHitByProjectile() = true
    override fun getPickResult() = itemStack.copy()!!

    fun grab(player: Player): InteractionResult {
        remove(RemovalReason.DISCARDED)
        player.handItem(itemStack.copyWithCount(1))
        return InteractionResult.SUCCESS
    }

    override fun interactAt(player: Player, vec: Vec3, hand: InteractionHand): InteractionResult {
        val level = level() as? ServerLevel ?: return InteractionResult.SUCCESS

        // Stacking plushies
        if (player.mainHandItem.item is PlushItem && hand == InteractionHand.MAIN_HAND) {
            val entity = PlushEntity(level)
            entity.setPos(position().add(vec))
            entity.yRot = 180f + player.yRot
            entity.itemStack = player.mainHandItem
            level.addFreshEntity(entity)
            player.setItemInHand(hand, ItemStack.EMPTY)
            return InteractionResult.SUCCESS
        }

        // Grabbing the plushie
        if (player.isCrouching)
            return grab(player)

        // Boop!
        level().playSound(null, blockPosition(), ModSounds.Honk.event, SoundSource.BLOCKS, 1.0f, 1.0f)
        return InteractionResult.SUCCESS
    }

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        level().playSound(null, blockPosition(), ModSounds.Honk.event, SoundSource.BLOCKS, 1.0f, 1.0f)
        val attacker = source.entity ?: return false
        if (attacker is Player) grab(attacker)
        return false
    }

    override fun tick() {
        super.tick()
        setDeltaMovement(0.0, -0.2, 0.0)
        move(MoverType.SELF, deltaMovement)
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        builder.define(itemStackAccessor, ItemStack.EMPTY)
    }
    override fun readAdditionalSaveData(tag: CompoundTag) {
        val registryAccess = level()?.registryAccess() ?: return
        itemStack = tag.getOrNull("item")?.let { ItemStack.parse(registryAccess, it).getOrNull() } ?: defaultItem
    }
    override fun addAdditionalSaveData(tag: CompoundTag) {
        val registryAccess = level()?.registryAccess() ?: return
        if (itemStack != ItemStack.EMPTY)
            itemStack.save(registryAccess)?.let { tag.put("item", it) }
    }

    companion object {
        val itemStackAccessor = SynchedEntityData.defineId(PlushEntity::class.java, EntityDataSerializers.ITEM_STACK)!!
        val defaultItem = ModBlocks.Plush.item.defaultInstance!!
    }
}