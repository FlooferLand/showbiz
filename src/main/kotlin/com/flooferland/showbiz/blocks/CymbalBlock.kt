package com.flooferland.showbiz.blocks

import net.minecraft.core.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.phys.shapes.*
import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.blocks.entities.CymbalBlockEntity
import com.flooferland.showbiz.registry.ModBlocks

class CymbalBlock(properties: Properties) : FacingEntityBlock(properties) {
    override val codec = simpleCodec(::CymbalBlock)!!
    val shape = Shapes.box(0.46875, 0.1875, 0.4375, 0.53125, 1.6875, 0.5)!!

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.INVISIBLE
    override fun newBlockEntity(pos: BlockPos, state: BlockState) =
        ModBlocks.Cymbal.entityType!!.create(pos, state)!!
    override fun <T : BlockEntity?> getTicker(level: Level, state: BlockState, type: BlockEntityType<T>) =
        BlockEntityTicker<T> { level, pos, blockState, entity -> (entity as? CymbalBlockEntity)?.tick(level, pos, blockState) }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext) = shape
}