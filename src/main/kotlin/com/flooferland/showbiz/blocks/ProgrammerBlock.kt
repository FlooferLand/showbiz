package com.flooferland.showbiz.blocks

import com.flooferland.showbiz.Permissions.Companion.mayInteractWith
import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.blocks.entities.ProgrammerBlockEntity
import com.flooferland.showbiz.datagen.blocks.CustomBlockModel
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.registry.ModItems
import com.flooferland.showbiz.types.entity.PlayerProgrammingData
import com.flooferland.showbiz.utils.Extensions.generateHorizontal
import com.flooferland.showbiz.utils.Extensions.getCachedOrRotate
import com.flooferland.showbiz.utils.Sounds
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape


class ProgrammerBlock(props: Properties) : FacingEntityBlock(props), CustomBlockModel {
    override val codec = simpleCodec(::ProgrammerBlock)!!
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    override fun newBlockEntity(pos: BlockPos, state: BlockState) =
        ModBlocks.Programmer.entityType!!.create(pos, state)!!

    override fun canSurvive(state: BlockState, level: LevelReader, pos: BlockPos): Boolean {
        return canSupportCenter(level, pos.below(), Direction.UP)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, movedByPiston: Boolean) {
        val entity = level.getBlockEntity(pos) as? ProgrammerBlockEntity
        entity?.let { entity ->
            entity.operators.forEach { PlayerProgrammingData.resetPlayerState(it) }
        }
        super.onRemove(state, level, pos, newState, movedByPiston)
    }

    override fun updateShape(state: BlockState, direction: Direction, neighborState: BlockState, level: LevelAccessor, pos: BlockPos, neighborPos: BlockPos): BlockState {
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true)
            return Blocks.AIR.defaultBlockState()
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos)
    }

    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hitResult: BlockHitResult): InteractionResult {
        if (player.isHolding(ModItems.Wand.item)) return InteractionResult.PASS
        val data = PlayerProgrammingData.getFromPlayer(player)
        val blockEntity = level.getBlockEntity(pos) as? ProgrammerBlockEntity ?: return InteractionResult.FAIL
        if (!player.mayInteractWith(blockEntity)) return InteractionResult.PASS
        if (data.active && data.blockPos != pos) {
            player.displayClientMessage(Component.literal("You're already programming another terminal!"), true)
            return InteractionResult.FAIL
        }

        data.active = !data.active
        if (data.active) {
            data.blockPos = pos
            blockEntity.operators.add(player)
            player.displayClientMessage(Component.literal("Entering programming mode"), true)
            Sounds.player(player).enter()
        } else {
            data.blockPos = null
            blockEntity.operators.remove(player)
            player.displayClientMessage(Component.literal("Exiting programming mode"), true)
            Sounds.player(player).exit()
        }
        data.saveToPlayer(player)

        return InteractionResult.SUCCESS
    }

    override fun <T : BlockEntity?> getTicker(level: Level, state: BlockState, type: BlockEntityType<T>) =
        BlockEntityTicker<T> { level, pos, blockState, entity -> (entity as? ProgrammerBlockEntity)?.tick(level, pos, blockState) }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        val facing = state.getValue(FACING)
        return shapes.getCachedOrRotate(facing)
    }

    companion object {
        val shapes: HashMap<Direction, VoxelShape> = run {
            var shape = Shapes.empty()
            shape = Shapes.join(shape, Shapes.box(0.0, 0.0, 0.5, 1.0, 0.3125, 1.0), BooleanOp.OR);
            shape = Shapes.join(shape, Shapes.box(0.125, 0.3546875, 0.546875, 0.5, 0.671875, 0.9515625000000001), BooleanOp.OR);
            shape = Shapes.join(shape, Shapes.box(0.0625, 0.375, 0.4375, 0.5625, 0.8125, 0.625), BooleanOp.OR);
            shape = Shapes.join(shape, Shapes.box(0.590625, 0.3125, 0.625, 0.909375, 0.4375, 0.8578125), BooleanOp.OR);
            shape = Shapes.join(shape, Shapes.box(0.121875, 0.3125, 0.625, 0.503125, 0.4375, 0.9203125), BooleanOp.OR);
            shape = Shapes.join(shape, Shapes.box(0.5625, 0.375, 0.4375, 0.9375, 0.6875, 0.625), BooleanOp.OR);
            shape = Shapes.join(shape, Shapes.box(0.0625, 0.3125, 0.5, 0.5625, 0.375, 0.625), BooleanOp.OR);
            shape = Shapes.join(shape, Shapes.box(0.5625, 0.3125, 0.5, 0.9375, 0.375, 0.625), BooleanOp.OR);
            shape = Shapes.join(shape, Shapes.box(0.59375, 0.346875, 0.528125, 0.9, 0.5484375, 0.8734375), BooleanOp.OR);
            shape.generateHorizontal()
        }
    }
}