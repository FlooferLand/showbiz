package com.flooferland.showbiz.blocks

import com.flooferland.showbiz.Permissions.Companion.mayInteractAt
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.blocks.entities.SpeakerBlockEntity
import com.flooferland.showbiz.items.WandItem
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.types.AudioChannel
import com.flooferland.showbiz.utils.Extensions.applyChange
import com.flooferland.showbiz.utils.Extensions.generateHorizontal
import com.flooferland.showbiz.utils.Extensions.getCachedOrRotate
import com.flooferland.showbiz.utils.Sounds
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

class SpeakerBlock(props: Properties) : FacingEntityBlock(props) {
    override val codec = simpleCodec(::SpeakerBlock)!!
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    override fun newBlockEntity(pos: BlockPos, state: BlockState) =
        ModBlocks.Speaker.entityType!!.create(pos, state)!!

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        val facing = state.getValue(FACING)
        return shapes.getCachedOrRotate(facing)
    }

    override fun useItemOn(stack: ItemStack, state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hitResult: BlockHitResult): ItemInteractionResult? {
        return if (stack.item is BlockItem) ItemInteractionResult.FAIL else ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
    }

    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hitResult: BlockHitResult): InteractionResult? {
        val entity = level.getBlockEntity(pos) as? SpeakerBlockEntity ?: return InteractionResult.PASS
        if (!player.mayInteractAt(pos)) return InteractionResult.FAIL
        if (player.isHolding { it.item is WandItem }) return InteractionResult.FAIL
        if (level !is ServerLevel) return InteractionResult.SUCCESS

        val channels = AudioChannel.entries
        val new = channels.getOrNull((channels.indexOf(entity.channel) + 1) % channels.size) ?: entity.channel
        entity.applyChange(true) { channel = new }

        val message = Component.literal("Channel: ${new.name}")
        if (!Showbiz.config.audio.stereoAudio) {
            message.append(Component.literal(" (stereo is disabled)").withStyle(ChatFormatting.GRAY))
        }

        player.displayClientMessage(message, true)
        Sounds.serverPlayer(player).click()
        return InteractionResult.SUCCESS
    }

    companion object {
        val shapes: HashMap<Direction, VoxelShape> = run {
            var shape = Shapes.box(0.0, 0.0, 0.0625, 1.0, 1.0, 1.0);
            shape.generateHorizontal()
        }
    }
}