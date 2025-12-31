package com.flooferland.showbiz.entities

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageType
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityDimensions
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import com.flooferland.showbiz.network.packets.ModelPartInteractPacket
import com.flooferland.showbiz.registry.ModClientEntities
import com.flooferland.showbiz.types.IModelPartInteractable
import com.flooferland.showbiz.types.Vec3f
import com.flooferland.showbiz.utils.Extensions.divide
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

/**
 * Similar to Minecraft's InteractionEntity / DisplayEntity. <br/>
 * Simple light-weight entityType to register physical interactions. <br/>
 * Spawned and filled out by [com.flooferland.showbiz.types.ModelPartInstance]
 */
class ModelPartEntity(level: Level, val name: String? = null, pos: Vec3? = null, var size: Vec3? = null, val parent: BlockPos? = null) : Entity(ModClientEntities.ModelPart.type, level) {
    override fun defineSynchedData(builder: SynchedEntityData.Builder) = Unit
    override fun readAdditionalSaveData(compound: CompoundTag?) = Unit
    override fun addAdditionalSaveData(compound: CompoundTag) = Unit

    init {
        val pos = pos?.divide(8.0)
        size?.let { size = size?.divide(8.0) }
        if (parent != null && pos != null) {
            setPos(
                parent.center.x + pos.x,
                parent.bottomCenter.y + pos.y,
                parent.center.z + pos.z
            )
        }
        refreshDimensions()
    }

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        println("Interact bawwa")
        val level = level() as? ClientLevel ?: return InteractionResult.PASS
        val blockEntity = level.getBlockEntity(parent) as? IModelPartInteractable ?: return InteractionResult.PASS
        if (name == null || parent == null) return InteractionResult.PASS
        if (!blockEntity.getInteractionMapping().containsKey(name)) return InteractionResult.PASS

        val packet = ModelPartInteractPacket(name, parent, player.uuid)
        ClientPlayNetworking.send(packet)
        return InteractionResult.SUCCESS
    }

    override fun getName(): Component = name?.let { Component.literal(it) } ?: super.name

    override fun isPickable() = true

    override fun canBeHitByProjectile() = true

    override fun isInvulnerable() = true
    override fun hurt(source: DamageSource, amount: Float): Boolean {
        val entity = source.directEntity
        if (source.type().msgId == "player" && entity is Player) {
            interact(entity, InteractionHand.MAIN_HAND)
        }
        return false
    }

    override fun shouldRender(x: Double, y: Double, z: Double) = true

    override fun shouldBeSaved() = false

    override fun getDimensions(pose: Pose): EntityDimensions =
        size?.let { EntityDimensions.fixed(it.x.toFloat(), it.y.toFloat()) } ?: EntityDimensions.fixed(0.1f, 0.1f)
}