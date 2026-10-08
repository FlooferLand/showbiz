package com.flooferland.showbiz.blocks

import net.minecraft.core.*
import net.minecraft.world.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.ServerPackets
import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.blocks.entities.CurtainControllerBlockEntity
import com.flooferland.showbiz.network.packets.editscreen.CurtainControllerEditPacket
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.types.OwnerId
import com.flooferland.showbiz.utils.Extensions.applyChange
import com.flooferland.showbiz.utils.Extensions.handleEditScreen

class CurtainControllerBlock(props: Properties) : FacingEntityBlock(props) {
    override val codec = simpleCodec(::CurtainControllerBlock)!!

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    override fun newBlockEntity(pos: BlockPos, state: BlockState) =
        ModBlocks.CurtainController.entityType!!.create(pos, state)!!

    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hitResult: BlockHitResult): InteractionResult {
        return handleEditScreen(state, level, pos, player, hitResult)
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