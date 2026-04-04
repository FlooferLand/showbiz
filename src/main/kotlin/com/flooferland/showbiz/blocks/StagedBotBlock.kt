package com.flooferland.showbiz.blocks

import net.minecraft.core.*
import net.minecraft.sounds.SoundSource
import net.minecraft.world.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.item.context.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.phys.*
import net.minecraft.world.phys.shapes.*
import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity
import com.flooferland.showbiz.network.packets.BotListSelectPacket
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.registry.ModItems
import com.flooferland.showbiz.registry.ModSounds
import com.flooferland.showbiz.types.GigaDirectionProperty
import com.flooferland.showbiz.utils.Extensions.applyChange
import com.mojang.serialization.MapCodec
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import kotlin.math.roundToInt

class StagedBotBlock(props: Properties) : BaseEntityBlock(props) {
    val codec: MapCodec<StagedBotBlock> = simpleCodec(::StagedBotBlock)

    init {
        registerDefaultState(stateDefinition.any().setValue(facing, GigaDirectionProperty.Enum.North))
    }

    override fun codec(): MapCodec<out BaseEntityBlock> = codec
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    override fun getOcclusionShape(state: BlockState, level: BlockGetter, pos: BlockPos) =
        Shapes.box(-0.5, 0.0, -0.5, 1.5, 4.0, 1.5)!!

    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hitResult: BlockHitResult): InteractionResult {
        if (player.isHolding(ModItems.Wand.item)) return InteractionResult.PASS
        if (player.isHolding { s -> !s.isEmpty }) return InteractionResult.PASS
        player.openMenu(state.getMenuProvider(level, pos))
        return super.useWithoutItem(state, level, pos, player, hitResult)
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return ModBlocks.StagedBot.entityType!!.create(pos, state)!!
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(facing)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        if (context.level.isClientSide) return defaultBlockState().setValue(facing, GigaDirectionProperty.Enum.North)

        val valueInt = ((((context.rotation + 180.0) * 8) / 360) + 180f).roundToInt() % 8
        val value = GigaDirectionProperty.values.getOrNull(valueInt) ?: GigaDirectionProperty.Enum.North
        return defaultBlockState().setValue(facing, value)
    }

    override fun <T : BlockEntity?> getTicker(level: Level, state: BlockState, type: BlockEntityType<T>) =
        BlockEntityTicker<T> { level, pos, blockState, entity -> (entity as? StagedBotBlockEntity)?.tick(level, pos, blockState) }

    companion object {
        val facing = GigaDirectionProperty("facing")

        init {
            ServerPlayNetworking.registerGlobalReceiver(BotListSelectPacket.type) { packet, ctx ->
                val blockEntity = ctx.player().level().getBlockEntity(packet.blockPos) as? StagedBotBlockEntity ?: return@registerGlobalReceiver
                blockEntity.applyChange(true) {
                    botId = packet.id
                }
                ctx.player().playNotifySound(ModSounds.Select.event, SoundSource.PLAYERS, 0.1f, 1.4f)
            }
        }
    }
}