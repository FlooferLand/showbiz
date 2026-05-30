package com.flooferland.showbiz.blocks

import net.minecraft.core.*
import net.minecraft.sounds.*
import net.minecraft.world.*
import net.minecraft.world.entity.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.item.*
import net.minecraft.world.item.context.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.level.block.state.properties.*
import net.minecraft.world.level.storage.loot.*
import net.minecraft.world.level.storage.loot.parameters.*
import net.minecraft.world.phys.*
import net.minecraft.world.phys.shapes.*
import com.flooferland.showbiz.blocks.entities.PlushBlockEntity
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.registry.ModSounds
import com.flooferland.showbiz.utils.Extensions.applyChange

// TODO: Migrated to PlushEntity. Should be removed.
class PlushBlock(properties: Properties) : BaseEntityBlock(properties) {
    init {
        registerDefaultState(stateDefinition.any().setValue(ROTATION, 0))
    }

    val shape = Shapes.box(0.125, 0.0, 0.1875, 0.875, 0.8, 0.8125)!!
    val codec = simpleCodec(::PlushBlock)!!
    override fun codec() = codec
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.INVISIBLE
    override fun newBlockEntity(pos: BlockPos, state: BlockState) =
        ModBlocks.Plush.entityType!!.create(pos, state)!!

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return shape.move(0.0, getSittingOffset(level, pos), 0.0)
    }
    override fun <T : BlockEntity?> getTicker(level: Level, state: BlockState, type: BlockEntityType<T>) =
        BlockEntityTicker<T> { level, pos, blockState, entity -> (entity as? PlushBlockEntity)?.tick(level, pos, blockState) }

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
            blockEntity.itemStack = stack.copyWithCount(1)
        }
    }

    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hitResult: BlockHitResult): InteractionResult {
        level.playSound(player, pos, ModSounds.Honk.event, SoundSource.BLOCKS, 1.0f, 1.0f)
        return InteractionResult.SUCCESS
    }

    override fun getDrops(state: BlockState, params: LootParams.Builder): List<ItemStack> {
        val blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) as? PlushBlockEntity ?: return listOf()
        return listOf(blockEntity.itemStack)
    }

    override fun getCloneItemStack(level: LevelReader, pos: BlockPos, state: BlockState): ItemStack =
        (level.getBlockEntity(pos) as? PlushBlockEntity)?.itemStack ?: super.getCloneItemStack(level, pos, state)

    companion object {
        val ROTATION = BlockStateProperties.ROTATION_16!!

        fun getSittingOffset(level: BlockGetter, pos: BlockPos): Double {
            val belowPos = pos.below()
            val below = level.getBlockState(belowPos) ?: return 0.0
            if (below.isAir) return 0.0
            val belowShape = below.getShape(level, belowPos)

            if (belowShape.isEmpty) return 0.0
            val underShape = below.getShape(level, belowPos)
            if (underShape.isEmpty) return 0.0
            val seatHeight = underShape.toAabbs()
                .filter { it.minX <= 0.5 && it.maxX >= 0.5 && it.minZ <= 0.5 && it.maxZ >= 0.5 }
                .maxOfOrNull { it.maxY }
                ?: underShape.bounds().maxY.coerceAtMost(1.0)
            return seatHeight - 1.0
        }
    }
}