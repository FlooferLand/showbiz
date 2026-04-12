package com.flooferland.showbiz.entities

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityDimensions
import net.minecraft.world.entity.Pose
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity
import com.flooferland.showbiz.registry.ModClientEntities
import com.flooferland.showbiz.types.AbstractBotPart
import com.flooferland.showbiz.types.BotPartId
import com.flooferland.showbiz.types.math.Vec3f

class BotPartEntity(level: Level, id: BotPartId = BotPartId.None, owner: StagedBotBlockEntity? = null) : AbstractBotPart(ModClientEntities.BotPart.type, level, id, owner) {
    override fun isInvulnerable() = true
    override fun shouldBeSaved() = false
    override fun shouldRender(x: Double, y: Double, z: Double) = true
    override fun defineSynchedData(builder: SynchedEntityData.Builder) = Unit
    override fun readAdditionalSaveData(compound: CompoundTag) = Unit
    override fun addAdditionalSaveData(compound: CompoundTag) = Unit

    override fun isPushable() = false
    override fun isPickable() = true
    override fun canBeCollidedWith() = false
    override fun canCollideWith(entity: Entity) = (entity is BotPartEntity)
    override fun getDimensions(pose: Pose): EntityDimensions =
        targetSize.let { EntityDimensions.fixed(it.x.toFloat(), it.y.toFloat()) }

    val colliding = mutableListOf<BotPartEntity>()
    var targetPos = Vec3.ZERO!!
    var targetSize = Vec3(0.1, 0.1, 0.1)

    init {
        if (id == BotPartId.None) remove(RemovalReason.DISCARDED)
    }

    override fun tick() {
        val level = level() ?: return

        refreshDimensions()
        val blockPos = owner?.blockPos!!.let { Vec3(it.x.toDouble(), it.y.toDouble(), it.z.toDouble()) }
        val newPos = blockPos.add(targetPos.add(0.0, 1.0, 0.0))
        setPos(newPos)

        val area = boundingBox
        val collisions = level.getEntitiesOfClass(BotPartEntity::class.java, area).filter { it != this }
        colliding.clear()
        colliding.addAll(collisions)
    }
}