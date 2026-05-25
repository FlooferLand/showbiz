package com.flooferland.showbiz.entities

import net.minecraft.nbt.*
import net.minecraft.network.syncher.*
import net.minecraft.sounds.*
import net.minecraft.world.*
import net.minecraft.world.damagesource.*
import net.minecraft.world.entity.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.registry.ModClientEntities
import com.flooferland.showbiz.registry.ModSounds
import com.flooferland.showbiz.types.collidepart.CollidePartId
import com.flooferland.showbiz.types.collidepart.ICollidePartInteractable
import kotlin.math.abs
import kotlin.math.sqrt

class CollidePartEntity(level: Level, initialPos: Vec3? = null, val id: CollidePartId = CollidePartId.None, val owner: ICollidePartInteractable? = null) : Entity(ModClientEntities.CollidePart.type, level) {
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
    override fun canCollideWith(entity: Entity) = (entity is CollidePartEntity) || (entity is Player)
    override fun canBeHitByProjectile() = true
    override fun getDimensions(pose: Pose): EntityDimensions =
        targetSize.let { EntityDimensions.fixed(it.x.toFloat(), it.y.toFloat()) }

    val colliding = mutableSetOf<Entity>()
    val used = mutableSetOf<Entity>()
    var targetPos = Vec3.ZERO!!
    var targetSize = Vec3(0.1, 0.1, 0.1)
    var lastHitTime: Int = 0
    var hitDirection = Vec3.ZERO!!
    var punched = false

    init {
        if (id == CollidePartId.None) remove(RemovalReason.DISCARDED)
        initialPos?.let {
            setPos(it)
            targetPos = it
        }
    }

    override fun playerTouch(player: Player) {
        if (boundingBox.intersects(player.boundingBox)) {
            colliding += player
        }
    }

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        colliding += player
        punched = true
        return InteractionResult.PASS
    }

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        val attacker = source.entity ?: return false
        if (attacker is Player) {
            punched = true
            colliding += attacker
        }
        return false
    }

    override fun tick() {
        val level = level() ?: return
        if ((owner is BlockEntity && owner.isRemoved) || owner == null) {
            remove(RemovalReason.DISCARDED)
        }

        refreshDimensions()
        setPos(targetPos)

        val boxCollisions = level.getEntitiesOfClass(CollidePartEntity::class.java, boundingBox).filter { it != this }
        colliding += boxCollisions.filter { it.position() != position() }  // Filter prevents a weird bug triggering all of them at once upon join
        used.removeIf { it !in colliding }

        val newCollisions = colliding.filter { it !in used }
        if (newCollisions.isNotEmpty()) run {
            val hitter = newCollisions.first()
            val hitDir = calculateHitDirection(hitter)
            if (id == CollidePartId.Cymbal) {
                val forceV = abs(hitDir.y.toFloat())
                val forceH = hitDir.horizontalDistance().toFloat()
                val force = forceH + (forceV * 4f)
                val strength = force * if (hitDir.y.toFloat() < 0.0f) 1.5f else 0.2f
                if (strength < 0.1f) return@run
                val hitFreq = ((tickCount - lastHitTime) / 8f).coerceIn(0.1f, 1.0f)
                val volume = (hitFreq * strength * 1.5f).coerceIn(0.05f, 1.0f)
                val pitch = 1f + (level.random.nextFloat() - level.random.nextFloat()) * 0.02f
                level.playLocalSound(x, y, z, ModSounds.Cymbal.event, SoundSource.BLOCKS, volume, pitch, false)
                used += newCollisions
            }

            lastHitTime = tickCount
            hitDirection = hitDir
        }
        colliding.clear()
        punched = false
    }

    fun calculateHitDirection(hitter: Entity): Vec3 {
        if (punched) return hitter.lookAngle.normalize()
        val force = hitter.deltaMovement
        val length = sqrt(force.x * force.x + force.y * force.y + force.z * force.z)
        return if (length > 0.01) {
            Vec3(force.x / length, force.y / length, force.z / length).scale(length * 3.0)
        } else { // Fallback in case hitter wasn't moving
            val dir = position().subtract(hitter.position()).normalize()
            Vec3(dir.x, dir.y, dir.z).scale(0.2)
        }
    }
}