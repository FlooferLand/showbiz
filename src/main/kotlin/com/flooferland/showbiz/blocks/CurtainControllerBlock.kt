package com.flooferland.showbiz.blocks

import com.flooferland.showbiz.ServerPackets
import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.blocks.entities.CurtainControllerBlockEntity
import com.flooferland.showbiz.network.packets.editscreen.CurtainControllerEditPacket
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.types.OwnerId
import com.flooferland.showbiz.utils.Extensions.applyChange
import com.flooferland.showbiz.utils.Extensions.handleEditScreen
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult

class CurtainControllerBlock(props: Properties) : FacingEntityBlock(props) {
    override val codec = simpleCodec(::CurtainControllerBlock)!!

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    override fun newBlockEntity(pos: BlockPos, state: BlockState) =
        ModBlocks.CurtainController.entityType!!.create(pos, state)!!

    override fun useItemOn(stack: ItemStack, state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hitResult: BlockHitResult): ItemInteractionResult? {
        return handleEditScreen(state, level, pos, player, hand, hitResult)
    }

    companion object {
        init {
            ServerPackets.listen(CurtainControllerEditPacket.type) { packet, context ->
                val player = context.player() ?: return@listen
                val blockEntity = (packet.base.id as? OwnerId.BlockId)?.grabBlockEntity(player.serverLevel()) as? CurtainControllerBlockEntity ?: return@listen
                blockEntity.applyChange(true) {
                    blockEntity.menuData = packet.base
                    blockEntity.bitFilterOpen = packet.bitFilterOpen
                    blockEntity.bitFilterClose = packet.bitFilterClose
                }
            }
        }
    }
}