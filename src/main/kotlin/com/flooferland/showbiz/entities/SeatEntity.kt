package com.flooferland.showbiz.entities

import net.minecraft.world.*
import net.minecraft.world.entity.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.level.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.entities.base.BasePropEntity
import com.flooferland.showbiz.registry.ModLivingEntities
import com.flooferland.showbiz.types.ISeatBlock

class SeatEntity(level: Level) : BasePropEntity(ModLivingEntities.Seat.type, level) {
    init {
        refreshDimensions()
    }

    override fun tick() {
        super.tick()
        if (level().isClientSide) return

        if (passengers.isEmpty() || passengers.all { it.isRemoved }) {
            remove(RemovalReason.DISCARDED)
        }
        if (level().getBlockState(blockPosition()).block !is ISeatBlock) {
            remove(RemovalReason.DISCARDED)
        }
    }

    override fun rideTick() {
        super.rideTick()
    }

    override fun couldAcceptPassenger() = true
    override fun getPassengerAttachmentPoint(entity: Entity, dimensions: EntityDimensions, partialTick: Float): Vec3 {
        return Vec3.ZERO
    }
    override fun getDismountLocationForPassenger(passenger: LivingEntity): Vec3? {
        return position()
    }

    override fun isPickable() = false
    override fun isAttackable() = false
    override fun getDefaultDimensions(pose: Pose): EntityDimensions =
        EntityDimensions.fixed(0.1f, 0.1f)

    override fun interact(player: Player, hand: InteractionHand): InteractionResult? {
        return InteractionResult.PASS
    }
}