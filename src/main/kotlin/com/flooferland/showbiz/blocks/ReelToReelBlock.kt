package com.flooferland.showbiz.blocks

import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.blocks.entities.ReelToReelBlockEntity
import com.flooferland.showbiz.items.ReelItem
import com.flooferland.showbiz.items.WandItem
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.registry.ModComponents
import com.flooferland.showbiz.registry.ModSounds
import com.flooferland.showbiz.datagen.blocks.CustomBlockModel
import com.flooferland.showbiz.utils.Extensions.applyChange
import com.flooferland.showbiz.utils.Extensions.markDirtyNotifyAll
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
import net.minecraft.world.level.storage.loot.*
import net.minecraft.world.level.storage.loot.parameters.*
import net.minecraft.world.phys.*

class ReelToReelBlock(props: Properties) : FacingEntityBlock(props) {
    val codec: MapCodec<ReelToReelBlock> = simpleCodec(::ReelToReelBlock)
    override fun codec() = codec

    init {
        registerDefaultState(stateDefinition.any().setValue(PLAYING, false))
    }

    override fun <T : BlockEntity?> getTicker(level: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        BlockEntityTicker({ _, _, _, entity -> (entity as? ReelToReelBlockEntity)?.tick() })

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun newBlockEntity(pos: BlockPos, state: BlockState) =
        ModBlocks.ReelToReel.entity!!.create(pos, state)!!

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(PLAYING)
    }

    override fun useItemOn(stack: ItemStack, state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hitResult: BlockHitResult): ItemInteractionResult? {
        if (level.isClientSide) return ItemInteractionResult.CONSUME
        if (stack.item is WandItem) {
            return ItemInteractionResult.FAIL
        }

        // Hand stack
        if (player.getItemInHand(hand) != stack) return ItemInteractionResult.FAIL

        // Finding the BE
        val entity = level.getBlockEntity(pos)
        if (entity !is ReelToReelBlockEntity) return ItemInteractionResult.FAIL

        if (!entity.show.isEmpty() && !player.isCrouching) {
            val isOn = !entity.playing
            entity.applyChange(true) {
                entity.setPlaying(isOn)
            }
            level.setBlockAndUpdate(pos, state.setValue(PLAYING, isOn))
            return ItemInteractionResult.SUCCESS
        }

        // Adding / removing
        var sound = ModSounds.Deselect
        if (stack.item is ReelItem) {
            val filename = stack.components.get(ModComponents.FileName.type)
            if (filename != null) {
                player.setItemInHand(hand, Items.AIR.defaultInstance)

                // Playback
                entity.resetPlayback()
                entity.show.load(filename) {
                    entity.markDirtyNotifyAll()
                }
                sound = ModSounds.Select
            }
        } else if (stack.isEmpty && !entity.show.isEmpty()) {
            val showName = entity.show.name
            showName?.let { player.setItemInHand(hand, ReelItem.makeItem(filename = showName)) }

            // Playback
            entity.applyChange(true) {
                entity.resetPlayback()
            }
            sound = ModSounds.Deselect
        }
        level.setBlockAndUpdate(pos, state.setValue(PLAYING, false))
        player.playNotifySound(sound.event, SoundSource.PLAYERS, 1.0f, 1.0f)
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
        builder.defaultState(PLAYING, true)
        builder.bool(PLAYING) {
            trueState(postfix = "on") {
                model { endTextureWith(name.postfix) }
            }
            falseState(postfix = "off") {
                model { endTextureWith(name.postfix) }
            }
        }
    }

    companion object {
        val PLAYING: BooleanProperty = BooleanProperty.create("playing")!!
    }
}