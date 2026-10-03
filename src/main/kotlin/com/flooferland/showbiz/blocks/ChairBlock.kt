package com.flooferland.showbiz.blocks

import net.minecraft.core.*
import net.minecraft.world.*
import net.minecraft.world.entity.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.item.context.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.level.block.state.properties.*
import net.minecraft.world.level.entity.*
import net.minecraft.world.phys.*
import net.minecraft.world.phys.shapes.*
import com.flooferland.showbiz.entities.SeatEntity
import com.flooferland.showbiz.types.ISeatBlock
import com.flooferland.showbiz.utils.Extensions.rotateShape


class ChairBlock(properties: Properties) : Block(properties), ISeatBlock {
    init {
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH))
    }

    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hitResult: BlockHitResult): InteractionResult {
        if (level.isClientSide) return InteractionResult.SUCCESS

        val seats = level.getEntities(EntityTypeTest.forClass(SeatEntity::class.java), AABB.ofSize(pos.center, 0.8, 0.8, 0.8)) { _ -> true }
        seats.forEach { it.remove(Entity.RemovalReason.DISCARDED) }

        val seat = SeatEntity(level)
        val facing = state.getValue(FACING)
        seat.setPos(pos.center)
        seat.yRot = facing.toYRot()
        seat.xRot = 0f
        seat.setYBodyRot(facing.toYRot())
        seat.setYHeadRot(facing.toYRot())
        seat.setOldPosAndRot()
        player.startRiding(seat)
        level.addFreshEntity(seat)

        return InteractionResult.CONSUME
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return shape.rotateShape(state.getValue(FACING))
    }

    override fun rotate(state: BlockState, rotation: Rotation): BlockState =
        state.setValue(FACING, rotation.rotate(state.getValue(FACING)))
    override fun mirror(state: BlockState, mirror: Mirror): BlockState =
        state.rotate(mirror.getRotation(state.getValue(FACING)))
    override fun getStateForPlacement(context: BlockPlaceContext): BlockState =
        defaultBlockState().setValue(FACING, context.horizontalDirection.opposite)
    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
    }

    companion object {
        val FACING: DirectionProperty = HorizontalDirectionalBlock.FACING!!
        val shape: VoxelShape = run {
            var shape = Shapes.empty()
            shape = Shapes.join(shape, Shapes.box(0.75, 0.0, 0.75, 0.8125, 0.9375, 0.875), BooleanOp.OR)
            shape = Shapes.join(shape, Shapes.box(0.1875, 0.0, 0.75, 0.25, 0.9375, 0.875), BooleanOp.OR)
            shape = Shapes.join(shape, Shapes.box(0.24375, 0.571875, 0.74375, 0.75625, 0.959375, 0.88125), BooleanOp.OR)
            shape = Shapes.join(shape, Shapes.box(0.1875, 0.0, 0.25, 0.25, 0.125, 0.75), BooleanOp.OR)
            shape = Shapes.join(shape, Shapes.box(0.75, 0.0, 0.25, 0.8125, 0.125, 0.75), BooleanOp.OR)
            shape = Shapes.join(shape, Shapes.box(0.1875, 0.0, 0.125, 0.25, 0.375, 0.25), BooleanOp.OR)
            shape = Shapes.join(shape, Shapes.box(0.75, 0.0, 0.125, 0.8125, 0.375, 0.25), BooleanOp.OR)
            shape = Shapes.join(shape, Shapes.box(0.75, 0.375, 0.125, 0.8125, 0.5, 0.75), BooleanOp.OR)
            shape = Shapes.join(shape, Shapes.box(0.1875, 0.375, 0.125, 0.25, 0.5, 0.75), BooleanOp.OR)
            shape = Shapes.join(shape, Shapes.box(0.25, 0.375, 0.125, 0.75, 0.5, 0.875), BooleanOp.OR)
            shape = Shapes.join(shape, Shapes.box(0.24375, 0.43125, 0.115625, 0.75625, 0.50625, 0.878125), BooleanOp.OR)
            shape
        }
    }
}