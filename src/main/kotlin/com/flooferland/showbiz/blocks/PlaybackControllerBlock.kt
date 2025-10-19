package com.flooferland.showbiz.blocks

import com.flooferland.bizlib.formats.RshowFormat
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.blocks.entities.PlaybackControllerBlockEntity
import com.flooferland.showbiz.items.WandItem
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.registry.ModSounds
import com.flooferland.showbiz.registry.blocks.CustomBlockModel
import com.flooferland.showbiz.utils.Extensions.applyChange
import com.mojang.serialization.MapCodec
import net.minecraft.core.*
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
import java.nio.file.Files

class PlaybackControllerBlock(props: Properties) : BaseEntityBlock(props), CustomBlockModel {
    companion object {
        val playing: BooleanProperty = BooleanProperty.create("playing")!!
    }
    val codec: MapCodec<PlaybackControllerBlock> = simpleCodec(::PlaybackControllerBlock)

    init {
        registerDefaultState(stateDefinition.any().setValue(playing, false))
    }

    override fun <T : BlockEntity?> getTicker(level: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        BlockEntityTicker({ _, _, _, entity -> (entity as? PlaybackControllerBlockEntity)?.tick() })

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

        if (stack.isEmpty) {
            // Changing the state by hand
            val isOn = state.getValue(playing).not()
            player.playNotifySound((if (isOn) ModSounds.Select else ModSounds.Deselect).event, SoundSource.PLAYERS, 1.0f, 1.0f)

            // Updating the block entity
            val entity = level.getBlockEntity(pos)
            if (entity is PlaybackControllerBlockEntity)
                entity.applyChange(true) {
                    playing = isOn
                    if (!isOn) entity.seek = 0.0
                    if (show == null) {
                        val format = RshowFormat()
                        val stream = Files.newInputStream(PlaybackControllerBlockEntity.TEST_FILE)
                        Showbiz.log.info("Loading show '${PlaybackControllerBlockEntity.TEST_FILE.fileName}'")
                        show = runCatching { format.read(stream) }.getOrNull()
                        Showbiz.log.info("Show loaded! (audioData=${show?.audio?.size}, signalData=${show?.signal?.size})")
                        stream.close()
                    }
                }
            level.setBlockAndUpdate(pos, state.setValue(playing, isOn))
            return ItemInteractionResult.SUCCESS
        }

        return ItemInteractionResult.FAIL
    }

    override fun modelBlockStates(builder: CustomBlockModel.BlockStateBuilder) {
        builder.defaultState(playing, true)
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