package com.flooferland.showbiz.blocks

import net.minecraft.core.*
import net.minecraft.network.chat.*
import net.minecraft.world.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.item.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.phys.*
import net.minecraft.world.phys.shapes.*
import com.flooferland.showbiz.Permissions.Companion.mayInteractAt
import com.flooferland.showbiz.Permissions.Companion.mayInteractWith
import com.flooferland.showbiz.blocks.entities.CurtainBlockEntity
import com.flooferland.showbiz.items.WandItem
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.utils.Extensions.applyChange
import com.flooferland.showbiz.utils.Sounds

class CurtainBlock(props: Properties) : BaseEntityBlock(props) {
    val codec = simpleCodec(::CurtainBlock)!!
    override fun codec() = codec

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    override fun newBlockEntity(pos: BlockPos, state: BlockState) =
        ModBlocks.CurtainBlock.entityType!!.create(pos, state)!!

    public override fun useItemOn(stack: ItemStack, state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hitResult: BlockHitResult): ItemInteractionResult {
        if (stack.item is DyeItem && player.mayInteractAt(pos)) {
            val blockEntity = level.getBlockEntity(pos) as? CurtainBlockEntity
            val color = (stack.item as DyeItem).dyeColor
            blockEntity?.applyChange(true) {
                blockEntity.color = color.textureDiffuseColor
            }
            return ItemInteractionResult.SUCCESS
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
    }

    public override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hitResult: BlockHitResult): InteractionResult {
        val entity = level.getBlockEntity(pos) as? CurtainBlockEntity ?: return InteractionResult.FAIL
        if (!player.mayInteractWith(entity)) return InteractionResult.FAIL
        if (player.isHolding { it.item is WandItem }) return InteractionResult.FAIL

        if (player.isCrouching) {
            val modifier = if ((hitResult.location.y - pos.y) < if (entity.isOpen) 0.7 else 0.5) -1 else 1
            var groupSet = entity.group + modifier
            if (groupSet > entity.groupMax) {
                groupSet = 0
            } else if (groupSet < 0) {
                groupSet = entity.groupMax
            }
            groupSet = groupSet.coerceIn(0, entity.groupMax)
            if (entity.group != groupSet) {
                entity.applyChange(true) { group = groupSet }
            }
            Sounds.click(player)
            player.displayClientMessage(Component.literal("[-] interact down, [+] interact up"), true)
        } else {
            entity.setCurtains(!entity.isOpen)
        }
        return InteractionResult.SUCCESS
    }

    override fun <T : BlockEntity?> getTicker(level: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        BlockEntityTicker { _, _, _, entity -> (entity as? CurtainBlockEntity)?.tick() }


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