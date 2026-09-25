package com.flooferland.showbiz.blocks

import net.minecraft.core.*
import net.minecraft.core.particles.*
import net.minecraft.server.level.*
import net.minecraft.sounds.*
import net.minecraft.world.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.item.context.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.ServerPackets
import com.flooferland.showbiz.blocks.entities.ShowParserBlockEntity
import com.flooferland.showbiz.datagen.blocks.CustomBlockModel
import com.flooferland.showbiz.items.WandItem
import com.flooferland.showbiz.network.packets.ShowParserEditPacket
import com.flooferland.showbiz.types.IRedstoneExtras
import com.flooferland.showbiz.types.OwnerId
import com.flooferland.showbiz.utils.Extensions.applyChange

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

    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hitResult: BlockHitResult): InteractionResult? {
        if (level.isClientSide) return InteractionResult.SUCCESS
        if (player.isHolding { it.item is WandItem }) return InteractionResult.PASS
        player.openMenu(state.getMenuProvider(level, pos))
        return InteractionResult.SUCCESS
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return defaultBlockState()
            .setValue(FACING, context.horizontalDirection.opposite)
    }

    fun refreshOutputState(level: Level, pos: BlockPos, state: BlockState, entity: ShowParserBlockEntity) {
        val level = level as? ServerLevel ?: return
        if (entity.menuData.bitFilter.isEmpty()) {
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
        val shouldPower = entity.show.data.playing && entity.menuData.bitFilter.chartHasBit(entity.show.data.mapping) { entity.show.data.signal.frameHas(it) }
        if (powered != shouldPower) {
            val newState = state.setValue(POWERED, shouldPower)
            level.setBlockAndUpdate(pos, newState)
            updateNeighborsInFront(level, pos, newState)
        }
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
                val blockEntity = (packet.base.id as? OwnerId.BlockId)?.grabBlockEntity(player.serverLevel()) as? ShowParserBlockEntity ?: return@listen
                blockEntity.applyChange(true) {
                    blockEntity.menuData = packet.base
                }
            }
        }
    }
}