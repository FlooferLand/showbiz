package com.flooferland.showbiz.entities

import net.minecraft.nbt.*
import net.minecraft.network.syncher.*
import net.minecraft.sounds.*
import net.minecraft.world.*
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.level.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity
import com.flooferland.showbiz.registry.ModClientEntities
import com.flooferland.showbiz.registry.ModSounds
import com.flooferland.showbiz.types.AbstractBotPart
import com.flooferland.showbiz.types.InteractPartId

class BotPartEntity(level: Level, id: InteractPartId = InteractPartId.None, owner: StagedBotBlockEntity? = null) : AbstractBotPart(ModClientEntities.BotPart.type, level, id, owner) {
    override fun isInvulnerable() = true
    override fun shouldBeSaved() = false
    override fun shouldRender(x: Double, y: Double, z: Double) = true
    override fun defineSynchedData(builder: SynchedEntityData.Builder) = Unit
    override fun readAdditionalSaveData(compound: CompoundTag) = Unit
    override fun addAdditionalSaveData(compound: CompoundTag) = Unit

    override fun isPushable() = false
    override fun isPickable() = true
    override fun isAttackable() = true
    override fun canBeCollidedWith() = false
    override fun canCollideWith(entity: Entity) = (entity is BotPartEntity) || (entity is Player)
    override fun canBeHitByProjectile() = true
    override fun getDimensions(pose: Pose): EntityDimensions =
        targetSize.let { EntityDimensions.fixed(it.x.toFloat(), it.y.toFloat()) }

    val colliding = mutableSetOf<Entity>()
    val used = mutableSetOf<Entity>()
    var targetPos = Vec3.ZERO!!
    var targetSize = Vec3(0.1, 0.1, 0.1)

    init {
        if (id == InteractPartId.None) remove(RemovalReason.DISCARDED)
    }

    override fun playerTouch(player: Player) {
        if (boundingBox.intersects(player.boundingBox)) {
            colliding += player
        }
    }

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        colliding += player
        return InteractionResult.PASS
    }

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        val attacker = source.entity ?: return false
        if (attacker is Player) {
            colliding += attacker
        }
        return false
    }

    override fun tick() {
        val level = level() ?: return

        refreshDimensions()
        setPos(targetPos)

        val boxCollisions = level.getEntitiesOfClass(BotPartEntity::class.java, boundingBox).filter { it != this }
        colliding += boxCollisions.filter { it.position() != position() }  // Filter prevents a weird bug triggering all of them at once upon join
        used.removeIf { it !in colliding }

        val newCollisions = colliding.filter { it !in used }
        if (newCollisions.isNotEmpty()) {
            if (Showbiz.log.isDebugEnabled) {
                Showbiz.log.debug("{} Hit {}", id, colliding.joinToString(transform = { it.id.toString() }))
            }
            if (id == InteractPartId.RolfeCymbal) {
                level.playLocalSound(x, y, z, ModSounds.Cymbal.event, SoundSource.BLOCKS, 1.5f, 1f, false)
                used += newCollisions
            }
        }
        colliding.clear()
    }
}