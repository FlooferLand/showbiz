package com.flooferland.showbiz.blocks

import net.minecraft.core.*
import net.minecraft.world.item.context.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.level.block.state.properties.*
import net.minecraft.world.phys.shapes.*
import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.blocks.entities.ShowSelectorBlockEntity
import com.flooferland.showbiz.registry.ModBlocks

class ShowSelectorBlock(props: Properties) : FacingEntityBlock(props) {
    override val codec = simpleCodec(::ShowSelectorBlock)!!

    init { registerDefaultState(stateDefinition.any().setValue(WALL_MOUNTED, false)) }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(WALL_MOUNTED)
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext) =
        Shapes.create(0.4, 0.0, 0.4, 0.6, 0.8, 0.6)!!
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.INVISIBLE
    override fun newBlockEntity(pos: BlockPos, state: BlockState) =
        ModBlocks.ShowSelector.entityType!!.create(pos, state)!!

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return super.getStateForPlacement(context)
            // TODO: ADd the wallmounted show selector back
            // .setValue(WALL_MOUNTED, context.clickedFace.axis.isHorizontal)
    }
    override fun <T : BlockEntity?> getTicker(level: Level, state: BlockState, type: BlockEntityType<T>) =
        BlockEntityTicker<T> { level, pos, blockState, entity -> (entity as? ShowSelectorBlockEntity)?.tick(level, pos, blockState) }

    companion object {
        val WALL_MOUNTED: BooleanProperty = BooleanProperty.create("wall_mounted")
    }
}