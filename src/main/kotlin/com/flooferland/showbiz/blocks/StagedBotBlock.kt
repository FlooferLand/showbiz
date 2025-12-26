package com.flooferland.showbiz.blocks

import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.registry.ModItems
import com.flooferland.showbiz.utils.Extensions.applyChange
import com.mojang.serialization.MapCodec
import net.minecraft.core.*
import net.minecraft.network.chat.*
import net.minecraft.sounds.*
import net.minecraft.world.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.item.context.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.level.block.state.properties.*
import net.minecraft.world.phys.*

class StagedBotBlock(props: Properties) : BaseEntityBlock(props) {
    companion object {
        val facing = BlockStateProperties.HORIZONTAL_FACING!!
    }
    val codec: MapCodec<StagedBotBlock> = simpleCodec(::StagedBotBlock)

    init {
        registerDefaultState(stateDefinition.any().setValue(facing, Direction.NORTH))
    }

    override fun codec(): MapCodec<out BaseEntityBlock> = codec
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hitResult: BlockHitResult): InteractionResult {
        if (level.isClientSide) return InteractionResult.PASS
        if (player.isHolding(ModItems.Wand.item)) return InteractionResult.PASS
        if (player.isHolding({ s -> !s.isEmpty })) return InteractionResult.PASS

        val entity = level.getBlockEntity(pos) as? StagedBotBlockEntity
        if (entity != null) {
            val oldId = entity.botId
            entity.applyChange(true) {
                val ids = Showbiz.bots.keys.sorted()
                val index = ids.indexOf(oldId)
                val newId = if (index == -1 || index == ids.lastIndex) ids.firstOrNull() else ids[index + 1]
                newId?.let { entity.botId = newId }
            }
            player.playNotifySound(SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.PLAYERS, 1.0f, 1.0f)
            player.displayClientMessage(Component.literal("Switched to animatronic ${entity.botId}!"), true)
            return InteractionResult.SUCCESS
        }
        return super.useWithoutItem(state, level, pos, player, hitResult)
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return ModBlocks.StagedBot.entity!!.create(pos, state)!!
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(facing)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        return defaultBlockState().setValue(facing, context.horizontalDirection.opposite)
    }

    override fun <T : BlockEntity?> getTicker(level: Level, state: BlockState, type: BlockEntityType<T>) =
        BlockEntityTicker<T> { level, pos, blockState, entity -> (entity as? StagedBotBlockEntity)?.tick(level, pos, blockState) }
}