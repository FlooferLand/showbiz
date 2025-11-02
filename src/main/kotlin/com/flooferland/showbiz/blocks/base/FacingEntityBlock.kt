package com.flooferland.showbiz.blocks.base

import com.flooferland.showbiz.datagen.blocks.CustomBlockModel
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.DirectionProperty

/** A base block entity with the `FACING` property and block state model generation */
abstract class FacingEntityBlock(props: Properties) : BaseEntityBlock(props), CustomBlockModel {
    override fun modelBlockStates(builder: CustomBlockModel.BlockStateBuilder) {
        builder.facing(FACING)
    }

    //region Block state stuff
    override fun rotate(state: BlockState, rotation: Rotation): BlockState =
        state.setValue(FACING, rotation.rotate(state.getValue(FACING)))
    override fun mirror(state: BlockState, mirror: Mirror): BlockState =
        state.rotate(mirror.getRotation(state.getValue(FACING)))
    init { registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)) }
    override fun getStateForPlacement(context: BlockPlaceContext): BlockState =
        defaultBlockState().setValue(FACING, context.horizontalDirection.opposite)

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
    }
    companion object {
        val FACING: DirectionProperty = HorizontalDirectionalBlock.FACING!!
    }
    //endregion
}