package com.flooferland.showbiz.blocks

import net.minecraft.ChatFormatting
import com.flooferland.showbiz.blocks.entities.ReelToReelBlockEntity
import com.flooferland.showbiz.datagen.blocks.CustomBlockModel
import com.flooferland.showbiz.items.ReelItem
import com.flooferland.showbiz.items.WandItem
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.utils.Extensions.applyChange
import com.flooferland.showbiz.utils.Extensions.markDirtyNotifyAll
import net.minecraft.core.*
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.item.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.level.block.state.properties.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.registry.ModSounds
import com.flooferland.showbiz.utils.Extensions.handItem

class ReelToReelBlock(props: Properties) : FacingEntityBlock(props), CustomBlockModel {
    override val codec = simpleCodec(::ReelToReelBlock)!!

    init {
        registerDefaultState(stateDefinition.any().setValue(PLAYING, false))
    }

    override fun <T : BlockEntity?> getTicker(level: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        BlockEntityTicker { _, _, _, entity -> (entity as? ReelToReelBlockEntity)?.tick() }

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun newBlockEntity(pos: BlockPos, state: BlockState) =
        ModBlocks.ReelToReel.entityType!!.create(pos, state)!!

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(PLAYING)
    }

    override fun useItemOn(heldStack: ItemStack, state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hitResult: BlockHitResult): ItemInteractionResult? {
        if (heldStack.item is WandItem) {
            return ItemInteractionResult.FAIL
        }

        // Hand stack
        if (player.getItemInHand(hand) != heldStack) return ItemInteractionResult.FAIL

        // Finding the BE
        val entity = level.getBlockEntity(pos)
        if (entity !is ReelToReelBlockEntity) return ItemInteractionResult.FAIL

        if (player.isCrouching || hitResult.direction == Direction.UP || heldStack.item is ReelItem) {
            // Adding / removing
            if (heldStack.item is ReelItem && entity.showData.isEmpty()) {
                val filename = ReelItem.getFilename(heldStack) ?: ""
                if (filename.isNotEmpty()) {
                    val stackCopy = heldStack.copy()
                    player.setItemInHand(hand, Items.AIR.defaultInstance)
                    player.displayClientMessage(Component.literal("Loading.."), true)
                    player.playNotifySound(ModSounds.ReelEnter.event, SoundSource.MASTER, 1f, 1f)

                    // Playback
                    if (player is ServerPlayer) {
                        entity.resetPlayback()
                        entity.showData.load(filename) { data ->
                            entity.markDirtyNotifyAll()
                            if (player.isRemoved) return@load
                            if (data == null) {  // Error happened, giving back the reel
                                player.handItem(stackCopy)
                                player.displayClientMessage(
                                    Component.translatable("message.showbiz.show_load_fail").withStyle(ChatFormatting.RED), true
                                )
                                player.inventoryMenu.broadcastChanges()
                                return@load
                            }
                            level.playSound(player, pos, ModSounds.ReelEnter.event, SoundSource.MASTER, 0.4f, 1.5f)
                            player.displayClientMessage(Component.empty(), true)
                            player.inventoryMenu.broadcastChanges()
                        }
                    }
                } else {
                    player.displayClientMessage(
                        Component.translatable("message.showbiz.empty_reel_warning").withStyle(ChatFormatting.RED), true
                    )
                }
            } else if (heldStack.isEmpty && !entity.showData.isEmpty()) {  // Removing
                if (player is ServerPlayer) {
                    val showName = entity.showData.name
                    showName?.let { player.setItemInHand(hand, ReelItem.makeItem(filename = showName)) }
                    player.playNotifySound(ModSounds.ReelExit.event, SoundSource.MASTER, 1f, 1f)
                    entity.applyChange(true) {
                        entity.setPlaying(false)
                        entity.showData.free()
                        player.inventoryMenu.broadcastChanges()
                    }
                }
            }
            level.setBlockAndUpdate(pos, state.setValue(PLAYING, false))
        } else if (!level.isClientSide) {
            // Pausing
            if (!entity.showData.isEmpty()) {
                var paused = entity.paused

                entity.applyChange(true) {
                    if (!entity.playing) {
                        entity.setPlaying(true)
                        entity.setPaused(false)
                    } else {
                        paused = !entity.paused
                        entity.setPaused(paused)
                    }
                }

                if (!paused)
                    player.playNotifySound(ModSounds.ReelPlay.event, SoundSource.MASTER, 1f, 1f)
                else
                    player.playNotifySound(ModSounds.ReelPlay.event, SoundSource.MASTER, 0.5f, 0.8f)
                return ItemInteractionResult.SUCCESS
            } else {
                player.playNotifySound(ModSounds.ReelPlay.event, SoundSource.MASTER, 0.4f, 0.4f)
            }
        }
        return ItemInteractionResult.SUCCESS
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, movedByPiston: Boolean) {
        super.onRemove(state, level, pos, newState, movedByPiston)
        Containers.dropContentsOnDestroy(state, newState, level, pos)
    }

    override fun modelBlockStates(builder: CustomBlockModel.BlockStateBuilder) {
        super<FacingEntityBlock>.modelBlockStates(builder)
        builder.defaultState(suffix = "off")
        builder.bool(PLAYING) {
            trueState(suffix = "on") {
                model { endTextureWith(name.suffix) }
            }
            falseState(suffix = "off") {
                model { endTextureWith(name.suffix) }
            }
        }
    }

    companion object {
        val PLAYING: BooleanProperty = BooleanProperty.create("playing")!!
    }
}