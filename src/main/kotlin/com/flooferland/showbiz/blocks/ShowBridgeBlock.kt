package com.flooferland.showbiz.blocks

import com.flooferland.showbiz.ServerPackets
import com.flooferland.showbiz.blocks.entities.ShowBridgeBlockEntity
import com.flooferland.showbiz.network.packets.editscreen.ShowBridgeEditPacket
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.types.OwnerId
import com.flooferland.showbiz.types.showbridge.SafeSerialPort
import com.flooferland.showbiz.utils.Extensions.applyChange
import com.flooferland.showbiz.utils.Extensions.handleEditScreen
import com.mojang.serialization.MapCodec
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult

class ShowBridgeBlock(properties: Properties) : BaseEntityBlock(properties) {
    val codec: MapCodec<ShowBridgeBlock> = simpleCodec(::ShowBridgeBlock)
    override fun codec() = codec
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    override fun newBlockEntity(pos: BlockPos, state: BlockState) =
        ModBlocks.ShowBridge.entityType!!.create(pos, state)!!

    override fun useItemOn(stack: ItemStack, state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hitResult: BlockHitResult): ItemInteractionResult? {
        return handleEditScreen(state, level, pos, player, hand, hitResult)
    }

    override fun <T : BlockEntity?> getTicker(level: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        BlockEntityTicker { _, _, _, entity -> (entity as? ShowBridgeBlockEntity)?.tick() }

    companion object {
        init {
            ServerPlayerEvents.LEAVE.register { _ -> SafeSerialPort.closeAll() }
            ServerPackets.listen(ShowBridgeEditPacket.type) { packet, context ->
                val player = context.player() ?: return@listen
                val entity = (packet.base.id as? OwnerId.BlockId)?.grabBlockEntity(player.serverLevel()) as? ShowBridgeBlockEntity ?: return@listen
                entity.applyChange(true) {
                    menuData = packet.base
                    serverProfile = packet.serverProfile
                    playerProfiles[player.uuid] = packet.playerProfile

                    serialPorts.values.forEach { it.close() }
                    serialPorts.clear()
                }
            }
        }
    }
}