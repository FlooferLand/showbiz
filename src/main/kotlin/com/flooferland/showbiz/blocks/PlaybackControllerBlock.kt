package com.flooferland.showbiz.blocks

import com.flooferland.showbiz.blocks.entities.PlaybackControllerBlockEntity
import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity
import com.flooferland.showbiz.items.WandItem
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.registry.ModSounds
import com.flooferland.showbiz.registry.blocks.CustomBlockModel
import com.mojang.serialization.MapCodec
import net.minecraft.core.*
import net.minecraft.network.chat.*
import net.minecraft.sounds.*
import net.minecraft.world.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.item.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.level.block.state.properties.*
import net.minecraft.world.phys.*

class PlaybackControllerBlock(props: Properties) : BaseEntityBlock(props), CustomBlockModel {
    companion object {
        val playing: BooleanProperty = BooleanProperty.create("playing")!!
    }
    val codec: MapCodec<PlaybackControllerBlock> = simpleCodec(::PlaybackControllerBlock)

    init {
        registerDefaultState(stateDefinition.any().setValue(playing, false))
    }

    override fun codec(): MapCodec<out BaseEntityBlock> = codec
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return ModBlocks.PlaybackController.entity!!.create(pos, state)!!
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(playing)
    }

    override fun useItemOn(stack: ItemStack, state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hitResult: BlockHitResult): ItemInteractionResult? {
        if (level.isClientSide) return ItemInteractionResult.CONSUME
        if (stack.item is WandItem) {
            return ItemInteractionResult.FAIL
        }

        // Changing the state by hand
        val isOn = state.getValue(playing).not()
        player.playNotifySound((if (isOn) ModSounds.Select else ModSounds.Deselect).event, SoundSource.PLAYERS, 1.0f, 1.0f)

        // Updating the block entity
        val entity = level.getBlockEntity(pos)
        if (entity is PlaybackControllerBlockEntity) {
            if (entity.boundBot != null) {
                val botEntity = entity.boundBot?.let { level.getBlockEntity(it) }
                if (botEntity is StagedBotBlockEntity) {
                    botEntity.playing = isOn
                    entity.boundBot?.let { level.blockEntityChanged(it) }
                    botEntity.setChanged()
                    // TODO: For some reason teh block entity doesn't actually get updated.. Networking issue? I know its not GeckoLib related
                }
            } else {
                player.displayClientMessage(Component.literal("Not bound to any bot"), true)
            }
        }

        level.setBlockAndUpdate(pos, state.setValue(playing, isOn))
        return ItemInteractionResult.SUCCESS
    }

    override fun modelBlockStates(builder: CustomBlockModel.BlockStateBuilder) {
        builder.defaultState(playing, false)
        builder.bool(playing) {
            trueState(postfix = "on") {
                model { endTextureWith(name.postfix) }
            }
            falseState(postfix = "off") {
                model { endTextureWith(name.postfix) }
            }
        }
    }
}