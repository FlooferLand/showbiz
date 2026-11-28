package com.flooferland.showbiz.blocks

import com.flooferland.showbiz.ServerPackets
import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.blocks.entities.CurtainControllerBlockEntity
import com.flooferland.showbiz.network.packets.editscreen.CurtainControllerEditPacket
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.types.IRedstoneExtras
import com.flooferland.showbiz.types.OwnerId
import com.flooferland.showbiz.utils.Extensions.applyChange
import com.flooferland.showbiz.utils.Extensions.generateHorizontal
import com.flooferland.showbiz.utils.Extensions.getCachedOrRotate
import com.flooferland.showbiz.utils.Extensions.handleEditScreen
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.redstone.Redstone
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

class CurtainControllerBlock(props: Properties) : FacingEntityBlock(props), IRedstoneExtras {
    override val codec = simpleCodec(::CurtainControllerBlock)!!

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    override fun newBlockEntity(pos: BlockPos, state: BlockState) =
        ModBlocks.CurtainController.entityType!!.create(pos, state)!!

    override fun useItemOn(stack: ItemStack, state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hitResult: BlockHitResult): ItemInteractionResult? {
        return handleEditScreen(stack, state, level, pos, player, hand, hitResult)
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, movedByPiston: Boolean) {
        super.onPlace(state, level, pos, oldState, movedByPiston)
        if (level.isClientSide) return
        updateState(state, level, pos)
    }

    // Never knew this ran client-side.. Server signal is partly replicated tho
    override fun animateTick(state: BlockState, level: Level, pos: BlockPos, random: RandomSource) {
        val entity = level.getBlockEntity(pos) as? CurtainControllerBlockEntity ?: return
        if (entity.serverSignal == Redstone.SIGNAL_NONE) return
        if (level.gameTime % 10 != 0L) return

        val facing = state.getValue(FACING) ?: return
        val pos = pos.center.relative(facing.opposite, 0.4)
        level.addParticle(DustParticleOptions(DustParticleOptions.REDSTONE_PARTICLE_COLOR, 1f), pos.x, pos.y, pos.z,0.0, 0.0, 0.0)
    }

    override fun neighborChanged(state: BlockState, level: Level, pos: BlockPos, neighborBlock: Block, neighborPos: BlockPos, movedByPiston: Boolean) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston)
        if (level.isClientSide) return
        updateState(state, level, pos)
    }

    fun updateState(state: BlockState, level: Level, pos: BlockPos) {
        if (level !is ServerLevel) return
        val entity = level.getBlockEntity(pos) as? CurtainControllerBlockEntity ?: return
        val facing = state.getValue(FACING) ?: return

        // Redstone signal
        val signal = level.getSignal(pos.relative(facing.opposite), facing)
        val shouldOpen = signal > Redstone.SIGNAL_NONE
        if (signal != entity.serverSignal) {
            entity.applyChange(true) {
                entity.serverSignal = signal
                sendControl(shouldOpen)
            }
        }
    }

    override fun <T : BlockEntity?> getTicker(level: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        BlockEntityTicker { _, _, _, entity -> (entity as? CurtainControllerBlockEntity)?.tick() }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        val facing = state.getValue(FACING)
        return shapes.getCachedOrRotate(facing)
    }

    override fun wireShouldConnectTo(state: BlockState, direction: Direction): Boolean {
        val back = state.getValue(FACING).opposite
        return direction == back
    }

    companion object {
        val shapes: HashMap<Direction, VoxelShape> = run {
            var shape = Shapes.box(0.1875, 0.1875, 0.9375, 0.8125, 0.6875, 1.0)
            shape.generateHorizontal()
        }
        init {
            ServerPackets.listen(CurtainControllerEditPacket.type) { packet, context ->
                val player = context.player() ?: return@listen
                val entity = (packet.base.id as? OwnerId.BlockId)?.grabBlockEntity(player.serverLevel()) as? CurtainControllerBlockEntity ?: return@listen
                entity.applyChange(true) {
                    entity.menuData = packet.base
                    entity.bitFilterOpen = packet.bitFilterOpen
                    entity.bitFilterClose = packet.bitFilterClose
                    entity.defaultState = packet.defaultState
                    entity.speed = packet.speed
                    entity.serverSpeedDirty = true
                }
            }
        }
    }
}