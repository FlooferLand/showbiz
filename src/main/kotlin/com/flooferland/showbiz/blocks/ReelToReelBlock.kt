package com.flooferland.showbiz.blocks

import com.flooferland.showbiz.Permissions
import com.flooferland.showbiz.Permissions.Companion.mayInteractWith
import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.blocks.entities.ReelToReelBlockEntity
import com.flooferland.showbiz.datagen.blocks.CustomBlockModel
import com.flooferland.showbiz.items.ReelItem
import com.flooferland.showbiz.items.WandItem
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.registry.ModSounds
import com.flooferland.showbiz.utils.Extensions.applyChange
import com.flooferland.showbiz.utils.Extensions.generateHorizontal
import com.flooferland.showbiz.utils.Extensions.getCachedOrRotate
import com.flooferland.showbiz.utils.Extensions.setInventoryChanged
import com.flooferland.showbiz.utils.Sounds
import com.flooferland.showbiz.utils.tc
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Containers
import net.minecraft.world.InteractionHand
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.redstone.Redstone
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

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
        if (heldStack.item is WandItem) return ItemInteractionResult.FAIL

        // Hand stack
        if (player.getItemInHand(hand) != heldStack) return ItemInteractionResult.FAIL

        // Finding the BE
        val entity = level.getBlockEntity(pos)
        if (entity !is ReelToReelBlockEntity) return ItemInteractionResult.FAIL
        if (!player.mayInteractWith(entity)) return ItemInteractionResult.FAIL

        val sounds = Sounds.player(player)

        if (player.isCrouching || hitResult.direction == Direction.UP || heldStack.item is ReelItem) {
            // Adding / removing
            if (heldStack.item is ReelItem && entity.showData.isNotLoaded() && Permissions.SwitchReels.testAndNotify(player)) {
                // Adding a reel
                val filename = ReelItem.getFilename(heldStack) ?: ""
                if (filename.isNotEmpty()) {
                    sounds.play(ModSounds.ReelEnter)

                    // Playback
                    if (player is ServerPlayer) {
                        player.setItemInHand(hand, Items.AIR.defaultInstance)
                        player.setInventoryChanged()
                        entity.resetPlayback()
                    }
                    entity.loadShow(heldStack) { data ->
                        if (data != null) {
                            sounds.play(ModSounds.ReelEnter, volume = 0.4f, pitch = 1.5f)
                        }
                        player.displayClientMessage(Component.empty(), true)
                        player.setInventoryChanged()
                    }
                } else {
                    player.displayClientMessage(
                        tc("message", "empty_reel_warning").withStyle(ChatFormatting.RED), true
                    )
                }
            } else if (heldStack.isEmpty && Permissions.SwitchReels.testAndNotify(player)) { // Removing
                if (entity.showData.loading) {
                    player.displayClientMessage(Component.literal("Cancelled loading"), true)
                }
                val stack = entity.getItem(0)
                player.setItemInHand(hand, stack)
                sounds.play(ModSounds.ReelExit)
                entity.removeItem(0, 1)
                player.setInventoryChanged()
            }
            level.setBlockAndUpdate(pos, state.setValue(PLAYING, false))
        } else if (player is ServerPlayer && Permissions.ControlPlayback.testAndNotify(player)) {
            // Pausing / unpausing
            if (!entity.isEmpty) {
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
                    sounds.play(ModSounds.ReelPlay)
                else
                    sounds.play(ModSounds.ReelPlay, volume = 0.5f, pitch = 0.8f)
                return ItemInteractionResult.SUCCESS
            } else {
                sounds.play(ModSounds.ReelPlay, volume = 0.6f, pitch = 0.2f)
            }
        }
        return ItemInteractionResult.SUCCESS
    }

    override fun isSignalSource(state: BlockState) = true
    override fun getSignal(state: BlockState, level: BlockGetter, pos: BlockPos, direction: Direction): Int {
        if (direction.axis == Direction.Axis.Y) return Redstone.SIGNAL_NONE
        val entity = level.getBlockEntity(pos) as? ReelToReelBlockEntity ?: return Redstone.SIGNAL_NONE
        return if (entity.playing) Redstone.SIGNAL_MAX else Redstone.SIGNAL_NONE
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, movedByPiston: Boolean) {
        Containers.dropContentsOnDestroy(state, newState, level, pos)
        super.onRemove(state, level, pos, newState, movedByPiston)
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

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        val facing = state.getValue(FACING)
        return shapes.getCachedOrRotate(facing)
    }

    companion object {
        val PLAYING: BooleanProperty = BooleanProperty.create("playing")!!
        val shapes: HashMap<Direction, VoxelShape> = run {
            val shape = Shapes.box(0.0625, 0.0625, 0.375, 0.9375, 0.875, 0.6875)
            shape.generateHorizontal()
        }
    }
}