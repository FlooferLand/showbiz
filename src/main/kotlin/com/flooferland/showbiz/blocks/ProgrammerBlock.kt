package com.flooferland.showbiz.blocks

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
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
import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.blocks.entities.ProgrammerBlockEntity
import com.flooferland.showbiz.blocks.entities.ShowSelectorBlockEntity
import com.flooferland.showbiz.components.OptionBlockPos
import com.flooferland.showbiz.datagen.blocks.CustomBlockModel
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.registry.ModComponents
import com.flooferland.showbiz.registry.ModItems
import com.flooferland.showbiz.registry.ModPlayerSynchedData
import com.flooferland.showbiz.types.entity.PlayerProgrammingData
import com.flooferland.showbiz.utils.Extensions.applyComponent

class ProgrammerBlock(props: Properties) : FacingEntityBlock(props), CustomBlockModel {
    override val codec = simpleCodec(::ProgrammerBlock)!!
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    override fun newBlockEntity(pos: BlockPos, state: BlockState) =
        ModBlocks.Programmer.entityType!!.create(pos, state)!!

    override fun canSurvive(state: BlockState, level: LevelReader, pos: BlockPos): Boolean {
        return canSupportCenter(level, pos.below(), Direction.UP)
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
        if (data.active && data.blockPos != pos) {
            player.displayClientMessage(Component.literal("You're already programming another terminal!"), true)
            return InteractionResult.FAIL
        }

        data.active = !data.active
        if (data.active) {
            data.blockPos = pos
            blockEntity.operators.add(player)
            player.displayClientMessage(Component.literal("Entering programming mode"), true)
        } else {
            data.blockPos = null
            blockEntity.operators.remove(player)
            player.displayClientMessage(Component.literal("Exiting programming mode"), true)
        }
        data.saveToPlayer(player)

        return InteractionResult.SUCCESS
    }

    override fun <T : BlockEntity?> getTicker(level: Level, state: BlockState, type: BlockEntityType<T>) =
        BlockEntityTicker<T> { level, pos, blockState, entity -> (entity as? ProgrammerBlockEntity)?.tick(level, pos, blockState) }
}