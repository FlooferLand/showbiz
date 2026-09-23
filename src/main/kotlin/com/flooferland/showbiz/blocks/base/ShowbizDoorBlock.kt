package com.flooferland.showbiz.blocks.base

import net.minecraft.core.*
import net.minecraft.server.level.*
import net.minecraft.sounds.*
import net.minecraft.util.*
import net.minecraft.world.*
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.targeting.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.level.gameevent.*
import net.minecraft.world.level.pathfinder.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.registry.ModSetTypes

class ShowbizDoorBlock(properties: Properties) : DoorBlock(ModSetTypes.ShowbizWood.type, properties.noOcclusion()) {
    override fun isPathfindable(state: BlockState, pathComputationType: PathComputationType) = true

    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hitResult: BlockHitResult): InteractionResult? {
        setOpenButBetter(state, level, pos, player, state.getValue(OPEN).not())
        level.scheduleTick(pos, this, 1)
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    override fun neighborChanged(state: BlockState, level: Level, pos: BlockPos, neighborBlock: Block, neighborPos: BlockPos, movedByPiston: Boolean) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston)
        level.scheduleTick(pos, this, 1)
    }

    override fun tick(state: BlockState, level: ServerLevel, pos: BlockPos, random: RandomSource) {
        val position = pos.center
        val forward = state.getValue(FACING).opposite
        val center = pos.center.relative(forward, 0.5)
        val other1 = forward.clockWise.step().mul(0.3f)
        val other2 = forward.counterClockWise.step().mul(0.3f)
        val bounds = AABB.ofSize(center, 0.5, 2.0, 0.5)
        val entity = level.getNearestEntity(
            LivingEntity::class.java,
            TargetingConditions.forNonCombat(),
            null,
            position.x, position.y, position.z,
            bounds
                .expandTowards(other1.x.toDouble(), other1.y.toDouble(), other1.z.toDouble())
                .expandTowards(other2.x.toDouble(), other2.y.toDouble(), other2.z.toDouble())
        )

        val open = state.getValue(OPEN)
        if (entity == null && open) {
            val backward = forward.opposite.step().mul(2.0f)
            val forward = forward.step().mul(2.0f)
            val entity = level.getNearestEntity(
                LivingEntity::class.java,
                TargetingConditions.forNonCombat(),
                null,
                position.x, position.y, position.z,
                bounds
                    .inflate(1.5)
                    .expandTowards(forward.x.toDouble(), forward.y.toDouble(), forward.z.toDouble())
                    .expandTowards(backward.x.toDouble(), backward.y.toDouble(), backward.z.toDouble())
            )
            if (entity == null) setOpenButBetter(state, level, pos, entity, false)
        } else if (entity != null && !open) {
            setOpenButBetter(state, level, pos, entity, true)
        }
        level.scheduleTick(pos, this, 1)
    }

    fun setOpenButBetter(state: BlockState, level: Level, pos: BlockPos, entity: Entity?, open: Boolean): Boolean {
        var state = state
        state = state.setValue(OPEN, open)
        level.setBlockAndUpdate(pos, state)
        level.gameEvent(entity, if (open) GameEvent.BLOCK_OPEN else GameEvent.BLOCK_CLOSE, pos)
        playDoorSound(entity, level, pos, open)
        return true
    }
    fun playDoorSound(entity: Entity?, level: Level, pos: BlockPos, isOpening: Boolean) {
        val crouching = entity?.isCrouching ?: false
        level.playSound(
            null,
            pos,
            if (isOpening) type().doorOpen() else type().doorClose(),
            SoundSource.BLOCKS,
            if (crouching) 0.3f else 1.0f,
            level.getRandom().nextFloat() * 0.1f + 0.9f
        )
    }
}
