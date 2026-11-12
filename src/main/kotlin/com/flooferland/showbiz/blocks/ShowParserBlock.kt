package com.flooferland.showbiz.blocks

import com.flooferland.showbiz.ServerPackets
import com.flooferland.showbiz.blocks.entities.ShowParserBlockEntity
import com.flooferland.showbiz.datagen.blocks.CustomBlockModel
import com.flooferland.showbiz.network.packets.editscreen.ShowParserEditPacket
import com.flooferland.showbiz.types.IRedstoneExtras
import com.flooferland.showbiz.types.OwnerId
import com.flooferland.showbiz.utils.Extensions.applyChange
import com.flooferland.showbiz.utils.Extensions.handleEditScreen
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DiodeBlock
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3

class ShowParserBlock(properties: BlockBehaviour.Properties) : DiodeBlock(properties), EntityBlock, IRedstoneExtras, CustomBlockModel {
    val codec = simpleCodec(::ShowParserBlock)!!

    override fun getDelay(state: BlockState) = 0
    override fun codec() = codec

    init {
        registerDefaultState(
            stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false)
        )
    }

    override fun modelBlockStates(builder: CustomBlockModel.BlockStateBuilder) {
        super.modelBlockStates(builder)
        builder.defaultState(suffix = "off")
        builder.bool(POWERED) {
            trueState(suffix = "on") {}
            falseState(suffix = "off") {}
        }
    }

    override fun useItemOn(stack: ItemStack, state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hitResult: BlockHitResult): ItemInteractionResult? {
        return handleEditScreen(state, level, pos, player, hand, hitResult)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return defaultBlockState()
            .setValue(FACING, context.horizontalDirection.opposite)
    }

    fun refreshOutputState(level: Level, pos: BlockPos, state: BlockState, entity: ShowParserBlockEntity) {
        val level = level as? ServerLevel ?: return
        if (entity.bitFilter.isEmpty() && entity.mode == ShowParserBlockEntity.Mode.Bits) {
            if (level.gameTime % 40 == 0L) {
                val facing = state.getValue(FACING)
                val backward = Vec3(facing.normal.x.toDouble(), facing.normal.y.toDouble(), facing.normal.z.toDouble())
                val center = pos.center.add(backward.scale(0.35)).subtract(0.0, 0.1, 0.0)
                level.sendParticles(ParticleTypes.SMOKE, center.x, center.y, center.z, 5, 0.05, 0.05, 0.05, 0.02)
                level.playSound(null, pos, SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.BLOCKS, 0.1f, 1.0f)
            }
            return
        }

        val powered = state.getValue(POWERED)
        val shouldPower = shouldPower(entity)
        if (powered != shouldPower) {
            val newState = state.setValue(POWERED, shouldPower)
            level.setBlockAndUpdate(pos, newState)
            updateNeighborsInFront(level, pos, newState)
        }
    }

    fun shouldPower(entity: ShowParserBlockEntity): Boolean = with(entity) {
        when (entity.mode) {
            ShowParserBlockEntity.Mode.Bits -> show.data.playing && bitFilter.chartHasBit(show.data.mapping) { show.data.signal.frameHas(it) }
            ShowParserBlockEntity.Mode.Playing -> show.data.playing
        }.let { if (invert) !it else it }
    }

    override fun triggerEvent(state: BlockState, level: Level, pos: BlockPos, id: Int, param: Int): Boolean {
        super.triggerEvent(state, level, pos, id, param)
        val blockEntity = level.getBlockEntity(pos)
        return blockEntity != null && blockEntity.triggerEvent(id, param)
    }

    override fun getRenderShape(state: BlockState) = RenderShape.MODEL

    override fun isSignalSource(state: BlockState) = true

    override fun shouldTurnOn(level: Level, pos: BlockPos, state: BlockState): Boolean {
        return state.getValue(POWERED)
    }

    override fun wireShouldConnectTo(state: BlockState, direction: Direction): Boolean {
        val facing = state.getValue(FACING)
        return direction == facing
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState) = ShowParserBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(level: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? {
        if (level.isClientSide) return null
        return BlockEntityTicker { level, pos, state, entity ->
            if (entity is ShowParserBlockEntity && level is ServerLevel) {
                refreshOutputState(level, pos, state, entity)
            }
        }
    }

    override fun getMenuProvider(state: BlockState, level: Level, pos: BlockPos): MenuProvider? {
        return level.getBlockEntity(pos) as? MenuProvider
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING, POWERED)
    }

    companion object {
        init {
            ServerPackets.listen(ShowParserEditPacket.type) { packet, context ->
                val player = context.player() ?: return@listen
                val entity = (packet.base.id as? OwnerId.BlockId)?.grabBlockEntity(player.serverLevel()) as? ShowParserBlockEntity ?: return@listen
                entity.applyChange(true) {
                    entity.menuData = packet.base
                    entity.bitFilter.set(packet.bitFilter)
                    entity.mode = packet.mode
                    entity.invert = packet.invert
                }
            }
        }
    }
}