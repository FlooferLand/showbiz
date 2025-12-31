package com.flooferland.showbiz.blocks

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.blocks.entities.ShowParserBlockEntity
import com.flooferland.showbiz.datagen.blocks.CustomBlockModel
import com.flooferland.showbiz.items.WandItem
import com.flooferland.showbiz.network.packets.ShowParserDataPacket
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.utils.Extensions.applyChange
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

class ShowParserBlock(properties: BlockBehaviour.Properties) : FacingEntityBlock(properties) {
    override val codec = simpleCodec(::ShowParserBlock)!!
    val shape = Shapes.create(0.05, 0.0, 0.05, 0.95, 0.1, 0.95)!!

    init {
        registerDefaultState(
            defaultBlockState()
                .setValue(SIGNAL_POWERED, false)
                .setValue(PLAYING_POWERED, false)
        )
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(SIGNAL_POWERED)
        builder.add(PLAYING_POWERED)
    }

    override fun modelBlockStates(builder: CustomBlockModel.BlockStateBuilder) {
        super.modelBlockStates(builder)
        builder.defaultState(suffix = "off")
        builder.bool(SIGNAL_POWERED, PLAYING_POWERED) {
            trueState(suffix = "on") {}
            falseState(suffix = "off") {}
        }
        builder.bool(SIGNAL_POWERED) {
            trueState(suffix = "signal") {}
        }
        builder.bool(PLAYING_POWERED) {
            trueState(suffix = "playing") {}
        }
    }

    override fun getShape(state: BlockState?, level: BlockGetter?, pos: BlockPos?, context: CollisionContext?) = shape
    override fun getCollisionShape(state: BlockState?, level: BlockGetter?, pos: BlockPos?, context: CollisionContext?) = shape

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return ModBlocks.ShowParser.entityType!!.create(pos, state)!!
    }

    override fun getRenderShape(state: BlockState?) = RenderShape.MODEL

    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hitResult: BlockHitResult): InteractionResult? {
        if (level.isClientSide) return InteractionResult.PASS
        if (player.isHolding { it.item is WandItem }) return InteractionResult.PASS
        player.openMenu(state.getMenuProvider(level, pos))
        return InteractionResult.SUCCESS
    }

    override fun isSignalSource(state: BlockState) = true

    override fun getSignal(state: BlockState, level: BlockGetter, pos: BlockPos, direction: Direction): Int {
        val direction = direction.opposite
        val facing = state.getValue(FacingEntityBlock.FACING)
        val on = when (direction) {
            facing -> state.getValue(SIGNAL_POWERED)
            facing.opposite -> state.getValue(PLAYING_POWERED)
            else -> false
        }
        return if (on) 15 else 0
    }

    override fun <T : BlockEntity?> getTicker(level: Level, state: BlockState, type: BlockEntityType<T>) =
        BlockEntityTicker<T> { level, pos, blockState, entity -> (entity as? ShowParserBlockEntity)?.tick(level, pos, blockState) }

    override fun getDirectSignal(state: BlockState, level: BlockGetter, pos: BlockPos, direction: Direction): Int {
        return getSignal(state, level, pos, direction)
    }

    companion object {
        val SIGNAL_POWERED = BooleanProperty.create("powered_signal")!!
        val PLAYING_POWERED = BooleanProperty.create("powered_playing")!!

        init {
            ServerPlayNetworking.registerGlobalReceiver(ShowParserDataPacket.type) { packet, context ->
                val player = context.player() ?: return@registerGlobalReceiver
                val blockEntity = player.serverLevel().getBlockEntity(packet.blockPos) as? ShowParserBlockEntity ?: return@registerGlobalReceiver
                blockEntity.applyChange(true) {
                    blockEntity.bitFilter = packet.bitFilter
                }
            }
        }
    }
}