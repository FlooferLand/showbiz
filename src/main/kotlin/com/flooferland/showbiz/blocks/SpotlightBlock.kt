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
import com.flooferland.showbiz.utils.Extensions.applyChange
import kotlin.jvm.optionals.getOrNull

class SpotlightBlock(props: Properties) : FacingEntityBlock(props) {
    override val codec = simpleCodec(::SpotlightBlock)!!
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.INVISIBLE
    override fun newBlockEntity(pos: BlockPos, state: BlockState) =
        ModBlocks.Spotlight.entityType!!.create(pos, state)!!

    override fun hasDynamicShape() = true
    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape? {
        val facing = state.getOptionalValue(FACING).getOrNull() ?: return Shapes.block()

        var shape = Shapes.empty()
        shape = if (facing == Direction.NORTH || facing == Direction.SOUTH) {
            Shapes.join(shape, Shapes.box(0.375, 0.9375, 0.265625, 0.625, 1.0, 0.765625), BooleanOp.OR)
        } else {
            Shapes.join(shape, Shapes.box(0.25, 0.9375, 0.390625, 0.75, 1.0, 0.640625), BooleanOp.OR);
        }
        return shape
    }

    override fun <T : BlockEntity?> getTicker(level: Level, state: BlockState, type: BlockEntityType<T>) =
        BlockEntityTicker<T> { level, pos, blockState, entity -> (entity as? SpotlightBlockEntity)?.tick(level, pos, blockState) }

    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hitResult: BlockHitResult): InteractionResult? {
        if (level.isClientSide) return InteractionResult.PASS
        if (player.isHolding { it.item is WandItem }) return InteractionResult.PASS
        player.openMenu(state.getMenuProvider(level, pos))
        return InteractionResult.SUCCESS
    }

    override fun neighborChanged(state: BlockState, level: Level, pos: BlockPos, neighborBlock: Block, neighborPos: BlockPos, movedByPiston: Boolean) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston)
        if (level.isClientSide) return

        val entity = level.getBlockEntity(pos) as? SpotlightBlockEntity ?: return
        val signal = level.getSignal(pos.above(), Direction.UP)
        if (signal != entity.redstoneSignal)
            entity.applyChange(true) { redstoneSignal = signal }
    }

    companion object {
        init {
            ServerPackets.listen(SpotlightEditPacket.type) { packet, context ->
                val player = context.player() ?: return@listen
                val blockEntity = player.serverLevel().getBlockEntity(packet.base.blockPos) as? SpotlightBlockEntity ?: return@listen
                blockEntity.applyChange(true) {
                    blockEntity.applyPacket(packet)
                }
            }
        }
    }
}