package com.flooferland.showbiz.blocks

import net.minecraft.core.*
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.*
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.*
import com.flooferland.showbiz.blocks.entities.CurtainBlockEntity
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.utils.Extensions.applyChange

class CurtainShadowBlock(props: Properties) : Block(props) {
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.INVISIBLE

    fun getParent(level: BlockGetter, pos: BlockPos): BlockPos? {
        var found: BlockPos? = null
        for (y in 0..CurtainBlockEntity.MAX_LENGTH) {
            val pos = pos.above(y)
            if (level.getBlockEntity(pos) as? CurtainBlockEntity != null) {
                found = pos
                break
            }
        }
        return found
    }

    override fun neighborChanged(state: BlockState, level: Level, pos: BlockPos, neighborBlock: Block, neighborPos: BlockPos, movedByPiston: Boolean) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston)
        if (getParent(level, pos) == null)
            level.removeBlock(pos, false)
    }

    override fun getCollisionShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext) =
        Shapes.empty()!!

    override fun useItemOn(stack: ItemStack, state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hitResult: BlockHitResult): ItemInteractionResult {
        val parentPos = getParent(level, pos) ?: return ItemInteractionResult.FAIL
        val parentState = level.getBlockState(parentPos) ?: state
        return (ModBlocks.CurtainBlock.block as CurtainBlock).useItem(stack, parentState, level, parentPos)
    }

    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hitResult: BlockHitResult): InteractionResult {
        val parentPos = getParent(level, pos) ?: return InteractionResult.FAIL
        val parentState = level.getBlockState(parentPos) ?: state
        return (ModBlocks.CurtainBlock.block as CurtainBlock).use(parentState, level, parentPos, player, hitResult)
    }

    override fun destroy(level: LevelAccessor, pos: BlockPos, state: BlockState) {
        // level.setBlock(pos, ModBlocks.CurtainBlockShadow.block.defaultBlockState(), 3)
        val blockEntity = getParent(level, pos)?.let { level.getBlockEntity(it) as? CurtainBlockEntity } ?: return
        blockEntity.applyChange(true) {
            blockEntity.openCurtains()
        }
    }

    override fun spawnDestroyParticles(level: Level, player: Player, pos: BlockPos, state: BlockState) {
        val parentPos = getParent(level, pos) ?: pos
        val data = BLOCK_STATE_REGISTRY.getId(ModBlocks.CurtainBlock.block.defaultBlockState())
        level.levelEvent(player, LevelEvent.PARTICLES_DESTROY_BLOCK, parentPos, data)
    }

    override fun getCloneItemStack(level: LevelReader, pos: BlockPos, state: BlockState) =
        ModBlocks.CurtainBlock.item.defaultInstance!!
}