package com.flooferland.showbiz.blocks

import net.minecraft.ChatFormatting
import com.flooferland.showbiz.blocks.entities.ReelToReelBlockEntity
import com.flooferland.showbiz.datagen.blocks.CustomBlockModel
import com.flooferland.showbiz.items.ReelItem
import com.flooferland.showbiz.items.WandItem
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.registry.ModComponents
import com.flooferland.showbiz.utils.Extensions.applyChange
import com.flooferland.showbiz.utils.Extensions.markDirtyNotifyAll
import com.mojang.serialization.MapCodec
import net.minecraft.core.*
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundSource
import net.minecraft.world.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.item.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.level.block.state.properties.*
import net.minecraft.world.level.storage.loot.*
import net.minecraft.world.level.storage.loot.parameters.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.registry.ModSounds
import com.flooferland.showbiz.utils.Extensions.handItem

class ReelToReelBlock(props: Properties) : BaseEntityBlock(props), CustomBlockModel {
    val codec: MapCodec<ReelToReelBlock> = simpleCodec(::ReelToReelBlock)
    override fun codec() = codec

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
        if (level.isClientSide) return ItemInteractionResult.CONSUME
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
            if (heldStack.item is ReelItem) {
                val filename = heldStack.components.get(ModComponents.FileName.type)
                if (filename != null) {
                    val stackCopy = heldStack.copy()
                    player.setItemInHand(hand, Items.AIR.defaultInstance)
                    player.displayClientMessage(Component.literal("Loading.."), true)
                    player.playNotifySound(ModSounds.ReelEnter.event, SoundSource.MASTER, 1f, 1f)

                    // Playback
                    entity.resetPlayback()
                    entity.show.load(filename) { data ->
                        entity.markDirtyNotifyAll()
                        if (player.isRemoved) return@load
                        if (data == null) {  // Error happened, giving back the reel
                            player.handItem(stackCopy)
                            player.displayClientMessage(Component.literal("Failed to load show. Check the server logs.").withStyle(ChatFormatting.RED), true)
                            return@load
                        }
                        player.playNotifySound(ModSounds.ReelEnter.event, SoundSource.MASTER, 0.4f, 1.5f)
                        player.displayClientMessage(Component.empty(), true)
                    }
                }
            } else if (heldStack.isEmpty && !entity.show.isEmpty()) {  // Removing
                val showName = entity.show.name
                showName?.let { player.setItemInHand(hand, ReelItem.makeItem(filename = showName)) }
                player.playNotifySound(ModSounds.ReelExit.event, SoundSource.MASTER, 1f, 1f)
                entity.applyChange(true) {
                    entity.setPlaying(false)
                }
            }
            level.setBlockAndUpdate(pos, state.setValue(PLAYING, false))
        } else {
            // Pausing
            if (!entity.show.isEmpty()) {
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

    // TODO: Make drops work
    override fun getDrops(state: BlockState, params: LootParams.Builder): List<ItemStack> {
        val entity = (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) as? ReelToReelBlockEntity) ?: return super.getDrops(state, params)

        val name = entity.show.name
        if (name != null && !entity.show.isEmpty()) {
            return mutableListOf(ReelItem.makeItem(filename = name))
        }

        return super.getDrops(state, params)
    }

    override fun modelBlockStates(builder: CustomBlockModel.BlockStateBuilder) {
        super.modelBlockStates(builder)
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