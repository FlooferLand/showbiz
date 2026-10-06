package com.flooferland.showbiz.blocks

import net.minecraft.core.*
import net.minecraft.server.level.*
import net.minecraft.sounds.*
import net.minecraft.world.*
import net.minecraft.world.entity.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.item.*
import net.minecraft.world.item.context.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.level.block.state.properties.*
import net.minecraft.world.level.entity.*
import net.minecraft.world.level.gameevent.*
import net.minecraft.world.phys.*
import net.minecraft.world.phys.shapes.*
import com.flooferland.showbiz.entities.SeatEntity
import com.flooferland.showbiz.types.ISeatBlock
import com.flooferland.showbiz.utils.Extensions.generateHorizontal
import com.flooferland.showbiz.utils.Extensions.getCachedOrRotate


class ChairBlock(props: Properties) : Block(props.dynamicShape()), ISeatBlock {
    init {
        registerDefaultState(
            stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(TUCKED, false)
        )
    }

    override fun useWithoutItem(state: BlockState, level: Level, blockPos: BlockPos, player: Player, hitResult: BlockHitResult): InteractionResult {
        val facing = state.getValue(FACING)
        val tucked = state.getValue(TUCKED)

        // Tucking/untucking the chair (very cool feature I saw in Paladin's Furniture)
        if (player.isCrouching) {
            setTucked(state, level, blockPos, !tucked)
            return InteractionResult.sidedSuccess(level.isClientSide)
        }
        val level = level as? ServerLevel ?: return InteractionResult.SUCCESS
        val pos = blockPos.center.relative(facing, -0.1 + if (tucked) TUCK_OFFSET else 0.0)

        // Cleaning up all seat entities
        val seats = level.getEntities(EntityTypeTest.forClass(SeatEntity::class.java), AABB.ofSize(pos, 0.9, 0.9, 0.9)) { _ -> true }
        seats.forEach { it.remove(Entity.RemovalReason.DISCARDED) }

        // Seating the entity
        val entity: Entity = (player.mainHandItem.item as? SpawnEggItem)?.let {
            val itemStack = player.mainHandItem
            val entityType = it.getType(player.mainHandItem)
            val entity = entityType.spawn(level, itemStack, player, blockPos, MobSpawnType.SPAWN_EGG, true, true)
            entity?.also { itemStack.shrink(1); level.gameEvent(player, GameEvent.ENTITY_PLACE, pos) }
        } ?: player
        val seat = SeatEntity(level)
        seat.setPos(pos)
        seat.yRot = facing.toYRot()
        seat.xRot = 0f
        seat.setYBodyRot(facing.toYRot())
        seat.setYHeadRot(facing.toYRot())
        seat.setOldPosAndRot()
        entity.startRiding(seat)
        level.addFreshEntity(seat)

        return InteractionResult.CONSUME
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        val facing = state.getValue(FACING)
        var shape = shapes.getCachedOrRotate(facing)
        if (state.getValue(TUCKED)) {
            shape = facing.step().let { shape.move(it.x.toDouble() * TUCK_OFFSET, it.y.toDouble() * TUCK_OFFSET, it.z.toDouble() * TUCK_OFFSET) }
        }
        return shape
    }

    override fun rotate(state: BlockState, rotation: Rotation): BlockState =
        state.setValue(FACING, rotation.rotate(state.getValue(FACING)))
    override fun mirror(state: BlockState, mirror: Mirror): BlockState =
        state.rotate(mirror.getRotation(state.getValue(FACING)))
    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        val facing = context.horizontalDirection
        return defaultBlockState()
            .setValue(FACING, facing)
            .setValue(TUCKED, (context.player?.isCrouching ?: false) && canTuck(facing, context.level, context.clickedPos))
    }
    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
        builder.add(TUCKED)
    }

    fun setTucked(state: BlockState, level: Level, blockPos: BlockPos, tucked: Boolean) {
        if (state.block !is ChairBlock) return
        val shouldTuck = tucked && canTuck(state.getValue(FACING), level, blockPos)
        level.setBlockAndUpdate(blockPos, state.setValue(TUCKED, shouldTuck))
        if (state.getValue(TUCKED) != shouldTuck) {
            level.playSound(null, blockPos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.2f, if (tucked) 0.8f else 0.6f)
        }
    }

    fun canTuck(facing: Direction, level: BlockGetter, pos: BlockPos): Boolean {
        val npos = pos.relative(facing)
        val neighbour = level.getBlockState(npos)
        return !Block.isFaceFull(neighbour.getVisualShape(level, npos, CollisionContext.empty()), facing.opposite)
    }

    companion object {
        const val TUCK_OFFSET = 6 / 16.0
        val FACING: DirectionProperty = HorizontalDirectionalBlock.FACING
        val TUCKED: BooleanProperty = BooleanProperty.create("tucked")
        val shapes: HashMap<Direction, VoxelShape> = run {
            var shape = Shapes.empty()
            shape = Shapes.join(shape, Shapes.box(0.75, 0.0, 0.75, 0.8125, 0.9375, 0.875), BooleanOp.OR)
            shape = Shapes.join(shape, Shapes.box(0.1875, 0.0, 0.75, 0.25, 0.9375, 0.875), BooleanOp.OR)
            shape = Shapes.join(shape, Shapes.box(0.1875, 0.0, 0.25, 0.25, 0.125, 0.75), BooleanOp.OR)
            shape = Shapes.join(shape, Shapes.box(0.75, 0.0, 0.25, 0.8125, 0.125, 0.75), BooleanOp.OR)
            shape = Shapes.join(shape, Shapes.box(0.1875, 0.0, 0.125, 0.25, 0.375, 0.25), BooleanOp.OR)
            shape = Shapes.join(shape, Shapes.box(0.75, 0.0, 0.125, 0.8125, 0.375, 0.25), BooleanOp.OR)
            shape = Shapes.join(shape, Shapes.box(0.1875, 0.375, 0.125, 0.8125, 0.5, 0.875), BooleanOp.OR)
            shape = Shapes.join(shape, Shapes.box(0.24375, 0.571875, 0.74375, 0.75625, 0.959375, 0.88125), BooleanOp.OR)
            shape.generateHorizontal()
        }
    }
}