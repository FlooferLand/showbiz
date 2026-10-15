package com.flooferland.showbiz.blocks

import net.minecraft.core.*
import net.minecraft.world.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.ServerPackets
import com.flooferland.showbiz.blocks.entities.ShowBridgeBlockEntity
import com.flooferland.showbiz.network.packets.editscreen.ShowBridgeEditPacket
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.types.OwnerId
import com.flooferland.showbiz.utils.Extensions.applyChange
import com.flooferland.showbiz.utils.Extensions.handleEditScreen
import com.mojang.serialization.MapCodec

class ShowBridgeBlock(properties: Properties) : BaseEntityBlock(properties) {
    val codec: MapCodec<ShowBridgeBlock> = simpleCodec(::ShowBridgeBlock)
    override fun codec() = codec
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    override fun newBlockEntity(pos: BlockPos, state: BlockState) =
        ModBlocks.ShowBridge.entityType!!.create(pos, state)!!

    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hitResult: BlockHitResult): InteractionResult {
        return handleEditScreen(state, level, pos, player, hitResult)
    }

    override fun <T : BlockEntity?> getTicker(level: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        BlockEntityTicker { _, _, _, entity -> (entity as? ShowBridgeBlockEntity)?.tick() }

    companion object {
        init {
            ServerPackets.listen(ShowBridgeEditPacket.type) { packet, context ->
                val player = context.player() ?: return@listen
                val entity = (packet.base.id as? OwnerId.BlockId)?.grabBlockEntity(player.serverLevel()) as? ShowBridgeBlockEntity ?: return@listen
                entity.applyChange(true) {
                    menuData = packet.base
                    serverProfile = packet.serverProfile
                    playerProfiles[player.uuid] = packet.playerProfile
                }
            }
        }
    }
}