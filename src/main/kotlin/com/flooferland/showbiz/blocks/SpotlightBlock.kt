package com.flooferland.showbiz.blocks

import net.minecraft.core.*
import net.minecraft.world.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.blocks.entities.SpotlightBlockEntity
import com.flooferland.showbiz.items.WandItem
import com.flooferland.showbiz.network.packets.SpotlightEditPacket
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.utils.Extensions.applyChange
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

class SpotlightBlock(props: Properties) : FacingEntityBlock(props) {
    override val codec = simpleCodec(::SpotlightBlock)!!
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.INVISIBLE
    override fun newBlockEntity(pos: BlockPos, state: BlockState) =
        ModBlocks.SpotlightBlock.entityType!!.create(pos, state)!!

    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hitResult: BlockHitResult): InteractionResult? {
        if (level.isClientSide) return InteractionResult.PASS
        if (player.isHolding { it.item is WandItem }) return InteractionResult.PASS
        player.openMenu(state.getMenuProvider(level, pos))
        return InteractionResult.SUCCESS
    }

    companion object {
        init {
            ServerPlayNetworking.registerGlobalReceiver(SpotlightEditPacket.type) { packet, context ->
                val player = context.player() ?: return@registerGlobalReceiver
                val blockEntity = player.serverLevel().getBlockEntity(packet.blockPos) as? SpotlightBlockEntity ?: return@registerGlobalReceiver
                blockEntity.applyChange(true) {
                    blockEntity.bitFilter = packet.bitFilter
                    blockEntity.turn = packet.turn
                }
            }
        }
    }
}