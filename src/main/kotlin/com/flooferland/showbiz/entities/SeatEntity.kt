package com.flooferland.showbiz.entities

import net.minecraft.core.*
import net.minecraft.server.level.*
import net.minecraft.world.*
import net.minecraft.world.entity.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.entity.vehicle.*
import net.minecraft.world.level.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.blocks.ChairBlock
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

    override fun removePassenger(passenger: Entity) {
        super.removePassenger(passenger)

        val level = level() as? ServerLevel ?: return
        val blockPos = blockPosition()
        val state = level.getBlockState(blockPos)
        (state.block as? ChairBlock)?.setTucked(state, level, blockPos, false)
    }

    override fun canControlVehicle() = false
    override fun couldAcceptPassenger() = true
    override fun getPassengerAttachmentPoint(entity: Entity, dimensions: EntityDimensions, partialTick: Float): Vec3 {
        return Vec3.ZERO
    }
    override fun getDismountLocationForPassenger(passenger: LivingEntity): Vec3? {
        val level = level() as? ServerLevel ?: return super.getDismountLocationForPassenger(passenger)
        val facing = direction
        val blockPos = blockPosition()
        val contenders = listOf(
            blockPos.relative(facing.clockWise),
            blockPos.relative(facing.counterClockWise),
            blockPos.relative(facing.opposite)
        )
        var valid: BlockPos? = null
        for (pos in contenders) {
            val floor = level.getBlockState(pos.below())
            if (floor.isAir) continue
            if (passenger.type.isBlockDangerous(floor)) continue
            if (!DismountHelper.canDismountTo(level, passenger, AABB.ofSize(pos.center, 1.0, 1.0, 1.0))) continue
            valid = pos
            break
        }
        if (valid != null) {
            return blockPos.bottomCenter.lerp(valid.bottomCenter, 0.5)
        } else {
            return blockPos.bottomCenter.relative(facing, 0.1)
        }
    }

    override fun isPickable() = false
    override fun isAttackable() = false
    override fun getDefaultDimensions(pose: Pose): EntityDimensions =
        EntityDimensions.fixed(0.1f, 0.1f)

    override fun interact(player: Player, hand: InteractionHand): InteractionResult? {
        return InteractionResult.PASS
    }
}