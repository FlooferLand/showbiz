package com.flooferland.showbiz.blocks

import net.minecraft.core.*
import net.minecraft.world.item.context.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.level.block.state.properties.*
import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.registry.ModBlocks

class MonitorBlock(props: Properties) : FacingEntityBlock(props) {
    override val codec = simpleCodec(::MonitorBlock)!!
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    override fun newBlockEntity(pos: BlockPos, state: BlockState) =
        ModBlocks.Monitor.entityType!!.create(pos, state)!!

    init { registerDefaultState(stateDefinition.any().setValue(HANGED, false)) }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(HANGED)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        val above = context.level.getBlockState(context.clickedPos.above())
        return super.getStateForPlacement(context)
            .setValue(HANGED, !above.isAir)
    }

    // Generator is bugged
    /*override fun modelBlockStates(builder: CustomBlockModel.BlockStateBuilder) {
        super.modelBlockStates(builder)
        builder.bool(HANGED) {
            trueState(suffix = "hanged") {}
            falseState() {}
        }
    }*/

    companion object {
        val HANGED = BooleanProperty.create("hanged")!!
    }
}