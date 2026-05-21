package com.flooferland.showbiz.blocks

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.*
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.level.block.state.properties.*
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParam
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import com.flooferland.showbiz.blocks.entities.PlushBlockEntity
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.utils.Extensions.applyChange

class PlushBlock(properties: Properties) : BaseEntityBlock(properties) {
    init {
        registerDefaultState(stateDefinition.any().setValue(ROTATION, 0))
    }

    val codec = simpleCodec(::PlushBlock)!!
    override fun codec() = codec
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