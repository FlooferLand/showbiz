package com.flooferland.showbiz.blocks

import net.minecraft.core.*
import net.minecraft.world.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.phys.*
import net.minecraft.world.phys.shapes.*
import com.flooferland.showbiz.ServerPackets
import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.blocks.entities.SpotlightBlockEntity
import com.flooferland.showbiz.items.WandItem
import com.flooferland.showbiz.network.packets.SpotlightEditPacket
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.types.OwnerId
import com.flooferland.showbiz.utils.Extensions.applyChange
import kotlin.jvm.optionals.getOrNull

class SpotlightBlock(props: Properties) : FacingEntityBlock(props) {
    override val codec = simpleCodec(::SpotlightBlock)!!
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.INVISIBLE
    override fun newBlockEntity(pos: BlockPos, state: BlockState) =
        ModBlocks.Spotlight.entityType!!.create(pos, state)!!

    override fun hasDynamicShape() = true
    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape? {
        var shape = when (state.getOptionalValue(FACING).getOrNull()) {
            Direction.NORTH -> Shapes.box(0.359375, 0.484375, 0.4375, 0.640625, 1.09375, 0.75)
            Direction.SOUTH -> Shapes.box(0.359375, 0.484375, 0.25, 0.640625, 1.09375, 0.5625)
            Direction.WEST -> Shapes.box(0.46875, 0.484375, 0.328125, 0.75, 1.09375, 0.640625)
            Direction.EAST -> Shapes.box(0.25, 0.484375, 0.34375, 0.53125, 1.09375, 0.65625)
            else -> super.getShape(state, level, pos, context)
        }
        (level.getBlockEntity(pos) as? SpotlightBlockEntity)?.let { entity ->
            if (entity.supportBelow && !entity.supportAbove)
                shape = shape.move(0.0, -0.5, 0.0)
        }
        return shape
    }

    override fun <T : BlockEntity?> getTicker(level: Level, state: BlockState, type: BlockEntityType<T>) =
        BlockEntityTicker<T> { level, pos, blockState, entity -> (entity as? SpotlightBlockEntity)?.tick(level, pos, blockState) }

    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hitResult: BlockHitResult): InteractionResult? {
        if (player.isHolding { it.item is WandItem }) return InteractionResult.PASS
        if (level.isClientSide) return InteractionResult.SUCCESS
        player.openMenu(state.getMenuProvider(level, pos))
        return InteractionResult.SUCCESS
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, movedByPiston: Boolean) {
        super.onPlace(state, level, pos, oldState, movedByPiston)
        if (level.isClientSide) return
        updateState(level, pos)
    }

    override fun neighborChanged(state: BlockState, level: Level, pos: BlockPos, neighborBlock: Block, neighborPos: BlockPos, movedByPiston: Boolean) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston)
        if (level.isClientSide) return
        updateState(level, pos)
    }

    fun updateState(level: Level, pos: BlockPos) {
        val entity = level.getBlockEntity(pos) as? SpotlightBlockEntity ?: return
        val signal = level.getSignal(pos.above(), Direction.UP)
        val supportAbove = Block.canSupportCenter(level, pos.above(), Direction.DOWN)
        val supportBelow = Block.canSupportCenter(level, pos.below(), Direction.UP)
        if (signal != entity.redstoneSignal || supportAbove != entity.supportAbove || supportBelow != entity.supportBelow) {
            entity.applyChange(true) {
                this.redstoneSignal = signal
                this.supportAbove = supportAbove
                this.supportBelow = supportBelow
            }
        }
    }

    companion object {
        init {
            ServerPackets.listen(SpotlightEditPacket.type) { packet, context ->
                val player = context.player() ?: return@listen
                val blockEntity = (packet.base.id as? OwnerId.BlockId)?.grabBlockEntity(player.serverLevel()) as? SpotlightBlockEntity ?: return@listen
                blockEntity.applyChange(true) {
                    blockEntity.applyPacket(packet)
                }
            }
        }
    }
}