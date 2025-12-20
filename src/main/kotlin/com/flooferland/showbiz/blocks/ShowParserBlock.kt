package com.flooferland.showbiz.blocks

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.blocks.entities.ShowParserBlockEntity
import com.flooferland.showbiz.items.WandItem
import com.flooferland.showbiz.network.packets.ShowParserDataPacket
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.utils.Extensions.applyChange
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

class ShowParserBlock(properties: BlockBehaviour.Properties) : FacingEntityBlock(properties) {
    override val codec = simpleCodec(::ShowParserBlock)!!
    val shape = Shapes.create(0.05, 0.0, 0.05, 0.95, 0.1, 0.95)!!

    override fun getShape(state: BlockState?, level: BlockGetter?, pos: BlockPos?, context: CollisionContext?) = shape
    override fun getCollisionShape(state: BlockState?, level: BlockGetter?, pos: BlockPos?, context: CollisionContext?) = shape

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return ModBlocks.ShowParser.entity!!.create(pos, state)!!
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
        val blockEntity = level.getBlockEntity(pos) as? ShowParserBlockEntity ?: return 0
        val on = when (direction) {
            facing ->
                blockEntity.show.data.signal.raw.any { blockEntity.bitFilter.contains(it) }
            facing.opposite ->
                blockEntity.show.data.playing
            else -> false
        }
        return if (on) 15 else 0
    }

    override fun getDirectSignal(state: BlockState, level: BlockGetter, pos: BlockPos, direction: Direction): Int {
        return getSignal(state, level, pos, direction)
    }

    companion object {
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