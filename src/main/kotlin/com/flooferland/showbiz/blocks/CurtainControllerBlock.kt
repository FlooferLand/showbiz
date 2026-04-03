package com.flooferland.showbiz.blocks

import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.blocks.entities.CurtainControllerBlockEntity
import com.flooferland.showbiz.items.WandItem
import com.flooferland.showbiz.network.packets.CurtainControllerEditPacket
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.utils.Extensions.applyChange
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

class CurtainControllerBlock(props: Properties) : FacingEntityBlock(props) {
    override val codec = simpleCodec(::CurtainControllerBlock)!!

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    override fun newBlockEntity(pos: BlockPos, state: BlockState) =
        ModBlocks.CurtainControllerBlock.entityType!!.create(pos, state)!!

    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hitResult: BlockHitResult): InteractionResult {
        if (level.isClientSide) return InteractionResult.PASS
        if (player.isHolding { it.item is WandItem }) return InteractionResult.PASS
        player.openMenu(state.getMenuProvider(level, pos))
        return InteractionResult.SUCCESS
    }

    companion object {
        init {
            ServerPlayNetworking.registerGlobalReceiver(CurtainControllerEditPacket.type) { packet, context ->
                val player = context.player() ?: return@registerGlobalReceiver
                val blockEntity = player.serverLevel().getBlockEntity(packet.base.blockPos) as? CurtainControllerBlockEntity ?: return@registerGlobalReceiver
                blockEntity.applyChange(true) {
                    blockEntity.menuData = packet.base
                    blockEntity.bitFilterOpen = packet.bitFilterOpen
                    blockEntity.bitFilterClose = packet.bitFilterClose
                }
            }
        }
    }
}