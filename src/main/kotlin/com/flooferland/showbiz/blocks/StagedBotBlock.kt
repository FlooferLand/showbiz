package com.flooferland.showbiz.blocks

import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.types.GigaDirectionProperty
import com.flooferland.showbiz.utils.Extensions.handleEditScreen
import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.phys.BlockHitResult
import kotlin.math.roundToInt

class StagedBotBlock(props: Properties) : BaseEntityBlock(props) {
    val codec: MapCodec<StagedBotBlock> = simpleCodec(::StagedBotBlock)

    init {
        registerDefaultState(stateDefinition.any().setValue(facing, GigaDirectionProperty.Enum.North))
    }

    override fun codec(): MapCodec<out BaseEntityBlock> = codec
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hitResult: BlockHitResult): InteractionResult {
        return handleEditScreen(state, level, pos, player, hitResult)
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return ModBlocks.StagedBot.entityType!!.create(pos, state)!!
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(facing)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        if (context.level.isClientSide) return defaultBlockState().setValue(facing, GigaDirectionProperty.Enum.North)

        val valueInt = ((((context.rotation + 180.0) * 8) / 360) + 180f).roundToInt() % 8
        val value = GigaDirectionProperty.values.getOrNull(valueInt) ?: GigaDirectionProperty.Enum.North
        return defaultBlockState().setValue(facing, value)
    }

    override fun <T : BlockEntity?> getTicker(level: Level, state: BlockState, type: BlockEntityType<T>) =
        BlockEntityTicker<T> { level, pos, blockState, entity -> (entity as? StagedBotBlockEntity)?.tick(level, pos, blockState) }

    companion object {
        val facing = GigaDirectionProperty("facing")
    }
}