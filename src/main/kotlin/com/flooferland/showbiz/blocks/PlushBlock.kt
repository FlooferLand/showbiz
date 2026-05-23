package com.flooferland.showbiz.blocks

import net.minecraft.core.*
import net.minecraft.world.entity.*
import net.minecraft.world.item.*
import net.minecraft.world.item.context.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.level.block.state.properties.*
import net.minecraft.world.level.storage.loot.*
import net.minecraft.world.level.storage.loot.parameters.*
import net.minecraft.world.phys.shapes.*
import com.flooferland.showbiz.blocks.entities.PlushBlockEntity
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.utils.Extensions.applyChange

class PlushBlock(properties: Properties) : BaseEntityBlock(properties) {
    init {
        registerDefaultState(stateDefinition.any().setValue(ROTATION, 0))
    }

    val shape = Shapes.box(0.125, 0.0, 0.1875, 0.875, 0.8, 0.8125)!!
    val codec = simpleCodec(::PlushBlock)!!
    override fun codec() = codec
    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext) = shape
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.INVISIBLE
    override fun newBlockEntity(pos: BlockPos, state: BlockState) =
        ModBlocks.Plush.entityType!!.create(pos, state)!!

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(ROTATION)
    }
    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return super.getStateForPlacement(context)!!
            .setValue(ROTATION, RotationSegment.convertToSegment(context.getRotation()))
    }

    override fun setPlacedBy(level: Level, pos: BlockPos, state: BlockState, placer: LivingEntity?, stack: ItemStack) {
        val blockEntity = level.getBlockEntity(pos) as? PlushBlockEntity ?: return
        blockEntity.applyChange(true) {
            blockEntity.stack = stack
        }
    }

    override fun getDrops(state: BlockState, params: LootParams.Builder): List<ItemStack> {
        val blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) as? PlushBlockEntity ?: return listOf()
        return blockEntity.stack?.let { listOf(it) } ?: listOf()
    }

    override fun getCloneItemStack(level: LevelReader, pos: BlockPos, state: BlockState): ItemStack =
        (level.getBlockEntity(pos) as? PlushBlockEntity)?.stack ?: super.getCloneItemStack(level, pos, state)

    override fun getOcclusionShape(state: BlockState, level: BlockGetter, pos: BlockPos) = Shapes.empty()!!

    companion object {
        val ROTATION = BlockStateProperties.ROTATION_16!!
    }
}