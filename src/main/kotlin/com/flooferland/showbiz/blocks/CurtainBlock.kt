package com.flooferland.showbiz.blocks

import net.minecraft.core.*
import net.minecraft.core.component.DataComponents
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.DyeItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.DyedItemColor
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import com.flooferland.showbiz.blocks.entities.CurtainBlockEntity
import com.flooferland.showbiz.items.WandItem
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.utils.Extensions.applyChange

class CurtainBlock(props: Properties) : BaseEntityBlock(props) {
    val codec = simpleCodec(::CurtainBlock)!!
    override fun codec() = codec

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    override fun newBlockEntity(pos: BlockPos, state: BlockState) =
        ModBlocks.CurtainBlock.entityType!!.create(pos, state)!!

    override fun useItemOn(stack: ItemStack, state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hitResult: BlockHitResult) =
        useItem(stack, state, level, pos)
    fun useItem(stack: ItemStack, state: BlockState, level: Level, pos: BlockPos): ItemInteractionResult {
        if (stack.item is DyeItem) {
            val blockEntity = level.getBlockEntity(pos) as? CurtainBlockEntity
            val color = (stack.item as DyeItem).dyeColor
            blockEntity?.applyChange(true) {
                blockEntity.color = color.textureDiffuseColor
            }
            return ItemInteractionResult.SUCCESS
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
    }

    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hitResult: BlockHitResult) =
        use(state, level, pos, player, hitResult)
    fun use(state: BlockState, level: Level, pos: BlockPos, player: Player, hitResult: BlockHitResult): InteractionResult {
        val blockEntity = level.getBlockEntity(pos) as? CurtainBlockEntity ?: return InteractionResult.FAIL
        if (player.isHolding { it.item is WandItem }) return InteractionResult.FAIL
        blockEntity.applyChange(true) {
            blockEntity.setCurtains(!blockEntity.isOpen)
        }
        return InteractionResult.SUCCESS
    }

    fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos): VoxelShape {
        val blockEntity = level.getBlockEntity(pos) as? CurtainBlockEntity
        val isOpen = blockEntity?.isOpen ?: false
        return Shapes.create(0.0, 0.0 + (if (isOpen) 0.5 else 0.0), 0.0, 1.0, 1.0, 1.0)
    }
    override fun hasDynamicShape() = true
    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext) =
        getShape(state, level, pos)
    override fun getCollisionShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext) =
        Shapes.block()!!
}