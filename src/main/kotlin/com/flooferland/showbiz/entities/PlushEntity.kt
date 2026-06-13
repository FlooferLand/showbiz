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
import com.flooferland.showbiz.registry.ModComponents
import com.flooferland.showbiz.registry.ModEntities
import com.flooferland.showbiz.registry.ModItems
import com.flooferland.showbiz.registry.ModSounds
import com.flooferland.showbiz.utils.Extensions.getOrNull
import com.flooferland.showbiz.utils.Extensions.handItem
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil
import kotlin.jvm.optionals.getOrNull

class PlushEntity(level: Level, defaultItem: ItemStack) : Entity(ModEntities.Plush.type, level), GeoEntity {
    constructor(level: Level) : this(level, ModItems.Plush.item.defaultInstance!!)
    val cache = GeckoLibUtil.createInstanceCache(this)!!
    override fun getAnimatableInstanceCache() = cache
    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar?) = Unit

    override fun getDimensions(pose: Pose): EntityDimensions {
        return EntityDimensions.fixed(0.4f, 0.75f)
    }

    val defaultItem = defaultItem.copyWithCount(1)!!
    private var itemStack = this.defaultItem
    fun updateItemStack(stack: ItemStack) {
        this.itemStack = stack
        if (!level().isClientSide)
            entityData.set(itemStackAccessor, stack)
    }

    init {
        refreshDimensions()
        updateItemStack(itemStack)
    }

    override fun isInvulnerable() = true
    override fun isPushable() = false
    override fun isPickable() = true
    override fun isAttackable() = true
    override fun canBeCollidedWith() = true
    override fun canBeHitByProjectile() = true
    override fun getPickResult() = itemStack.copy()!!

    fun getPlushComp() = itemStack.copy().get(ModComponents.Plush.type)

    fun grab(player: Player): InteractionResult {
        player.handItem(itemStack.copyWithCount(1))
        remove(RemovalReason.DISCARDED)
        return InteractionResult.SUCCESS
    }

    override fun interactAt(player: Player, vec: Vec3, hand: InteractionHand): InteractionResult {
        val level = level() as? ServerLevel ?: return InteractionResult.SUCCESS

        // Stacking plushies
        val stack = player.getItemInHand(hand).copyWithCount(1)
        val item = stack.item
        if (item is PlushItem && hand == InteractionHand.MAIN_HAND) {
            item.place(level, player, stack, position().add(vec))
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
        val attacker = source.entity ?: return false
        fun playSound(sound: ModSounds) {
            level().playSound(null, blockPosition(), sound.event, SoundSource.BLOCKS, 1.0f, 1.0f)
        }
        if (attacker !is Player) {
            playSound(ModSounds.Honk)
            return false
        }

        if (attacker.isCreative && amount > 0f) {
            playSound(ModSounds.HonkBye.event)
            remove(RemovalReason.DISCARDED)
        } else {
            playSound(ModSounds.Honk.event)
            grab(attacker)
        }

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

    override fun onSyncedDataUpdated(dataAccessor: EntityDataAccessor<*>) {
        super.onSyncedDataUpdated(dataAccessor)
        if (dataAccessor == itemStackAccessor && level().isClientSide) {
            itemStack = entityData.get(itemStackAccessor)
        }
    }
    override fun readAdditionalSaveData(tag: CompoundTag) {
        val registryAccess = level()?.registryAccess() ?: return
        itemStack = tag.getOrNull("item")?.let { ItemStack.parse(registryAccess, it).getOrNull() } ?: defaultItem
        updateItemStack(itemStack)
    }
    override fun addAdditionalSaveData(tag: CompoundTag) {
        val registryAccess = level()?.registryAccess() ?: return
        if (!itemStack.isEmpty)
            itemStack.save(registryAccess)?.let { tag.put("item", it) }
    }

    companion object {
        val itemStackAccessor = SynchedEntityData.defineId(PlushEntity::class.java, EntityDataSerializers.ITEM_STACK)!!
    }
}