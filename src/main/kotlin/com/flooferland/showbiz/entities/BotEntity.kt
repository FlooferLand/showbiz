package com.flooferland.showbiz.entities

import net.minecraft.nbt.*
import net.minecraft.network.syncher.*
import net.minecraft.world.entity.*
import net.minecraft.world.level.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.registry.ModEntities
import com.flooferland.showbiz.types.IBot
import com.flooferland.showbiz.types.ResourceId
import com.flooferland.showbiz.types.connection.ConnectionManager
import com.flooferland.showbiz.types.connection.IConnectable
import com.flooferland.showbiz.types.connection.PortDirection
import com.flooferland.showbiz.types.connection.data.PackedShowData
import com.flooferland.showbiz.utils.Extensions.getStringOrNull
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

// TODO: Figure out if calls to connectionChanged and entity accessors are even needed

class BotEntity(level: Level, botId: ResourceId? = null) : Entity(ModEntities.Bot.type, level), GeoEntity, IConnectable, IBot {
    constructor(level: Level) : this(level, null)
    val cache = GeckoLibUtil.createInstanceCache(this)!!
    override fun getAnimatableInstanceCache() = cache
    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar?) = Unit

    override val connectionManager = ConnectionManager(this)
    override val show = connectionManager.port("show", PackedShowData(), PortDirection.In, autoUseReceived = false) { received ->
        pendingShow.merge(received)
    }

    override var botId: ResourceId? = botId
    override val botLevel: Level? get() = level()
    override val botPos: Vec3 get() = position()
    override val botRemoved get() = isRemoved

    private val pendingShow = PackedShowData()

    override fun getDimensions(pose: Pose): EntityDimensions {
        return EntityDimensions.fixed(1.0f, 2.0f)
    }

    init {
        refreshDimensions()
    }

    override fun isInvulnerable() = true
    override fun isPushable() = false
    override fun isPickable() = true
    override fun isAttackable() = true
    override fun canBeCollidedWith() = true
    override fun canBeHitByProjectile() = true

    override fun tick() {
        super.tick()
        if (!level().isClientSide) {
            show.data.tempReset()
            show.data.merge(pendingShow)
            pendingShow.tempReset()
        }
    }

    fun updatePersistentData(block: (CompoundTag) -> Unit) {
        val tag = entityData.get(persistentDataAccessor).copy()
        block(tag)
        entityData.set(persistentDataAccessor, tag)
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        builder.define(persistentDataAccessor, CompoundTag().also { it.putString("bot_id", botId?.toString() ?: "") })
    }

    override fun onSyncedDataUpdated(dataAccessor: EntityDataAccessor<*>) {
        super.onSyncedDataUpdated(dataAccessor)
        if (dataAccessor == persistentDataAccessor && level().isClientSide) {
            val tag = entityData.get(persistentDataAccessor)
            readAdditionalSaveData(tag)
        }
    }

    override fun connectionChanged() {
        if (!level().isClientSide) updatePersistentData { connectionManager.save(it) }
    }

    override fun addAdditionalSaveData(tag: CompoundTag) {
        tag.putString("bot_id", botId?.toString() ?: "")
        connectionManager.save(tag)
    }
    override fun readAdditionalSaveData(tag: CompoundTag) {
        botId = tag.getStringOrNull("bot_id")?.let { if (it.isNotBlank()) ResourceId.of(it) else null }
        if (!level().isClientSide) {  // Preventing calling onSyncedDataUpdated again
            entityData.set(persistentDataAccessor, tag)
        }
        connectionManager.load(tag)
    }

    companion object {
        val persistentDataAccessor = SynchedEntityData.defineId(BotEntity::class.java, EntityDataSerializers.COMPOUND_TAG)!!
    }
}