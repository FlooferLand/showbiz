package com.flooferland.showbiz.blocks

import net.minecraft.core.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.*
import net.minecraft.world.phys.shapes.*
import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.blocks.entities.ShowSelectorBlockEntity
import com.flooferland.showbiz.registry.ModBlocks

class ShowSelectorBlock(props: Properties) : FacingEntityBlock(props) {
    override val codec = simpleCodec(::ShowSelectorBlock)!!
    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext) =
        Shapes.create(0.4, 0.0, 0.4, 0.6, 0.8, 0.6)!!
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.INVISIBLE
    override fun newBlockEntity(pos: BlockPos, state: BlockState) =
        ModBlocks.ShowSelector.entityType!!.create(pos, state)!!

    override fun <T : BlockEntity?> getTicker(level: Level, state: BlockState, type: BlockEntityType<T>) =
        BlockEntityTicker<T> { level, pos, blockState, entity -> (entity as? ShowSelectorBlockEntity)?.tick(level, pos, blockState) }
}