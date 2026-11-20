package com.flooferland.showbiz.blocks

import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.datagen.blocks.CustomBlockModel
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.types.FFmpeg
import com.flooferland.showbiz.utils.Extensions.click
import com.flooferland.showbiz.utils.Extensions.generateHorizontal
import com.flooferland.showbiz.utils.Extensions.getCachedOrRotate
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape


class MonitorBlock(props: Properties) : FacingEntityBlock(props) {
    override val codec = simpleCodec(::MonitorBlock)!!
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    override fun newBlockEntity(pos: BlockPos, state: BlockState) =
        ModBlocks.Monitor.entityType!!.create(pos, state)!!

    init { registerDefaultState(stateDefinition.any().setValue(HANGED, false)) }

    fun shouldHang(level: Level, pos: BlockPos): Boolean {
        val above = level.getBlockState(pos.above())
        return !above.isAir
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(HANGED)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return super.getStateForPlacement(context)
            .setValue(HANGED, shouldHang(context.level, context.clickedPos))
    }

    override fun neighborChanged(state: BlockState, level: Level, pos: BlockPos, neighborBlock: Block, neighborPos: BlockPos, movedByPiston: Boolean) {
        val hanging = state.getValue(HANGED)
        val shouldHang = shouldHang(level, pos)
        if (hanging != shouldHang) {
            level.setBlockAndUpdate(pos, state.setValue(HANGED, shouldHang))
        }
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston)
    }

    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hitResult: BlockHitResult): InteractionResult? {
        if (!FFmpeg.serverAvailable) {
            val comp = Component.literal("You need to install FFmpeg on your server to use the TV")
                .click(ClickEvent.Action.OPEN_URL, "https://github.com/FlooferLand/showbiz/wiki/Blocks:Monitor");
            player.displayClientMessage(comp, false)
        }
        return super.useWithoutItem(state, level, pos, player, hitResult)
    }

    override fun modelBlockStates(builder: CustomBlockModel.BlockStateBuilder) {
        super.modelBlockStates(builder)
        builder.bool(HANGED) {
            trueState(suffix = "hanged") {}
            falseState() {}
        }
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        val facing = state.getValue(FACING)
        return shapes.getCachedOrRotate(facing)
    }

    companion object {
        val shapes: HashMap<Direction, VoxelShape> = run {
            var shape = Shapes.empty()
            shape = Shapes.join(shape, Shapes.box(0.203125, 0.160846875, 0.267628125, 0.803125, 0.668346875, 0.915128125), BooleanOp.OR);
            shape = Shapes.join(shape, Shapes.box(0.103125, 0.09999999999999998, 0.090625, 0.903125, 0.8, 0.390625), BooleanOp.OR);
            shape = Shapes.join(shape, Shapes.box(0.198125, 0.0, 0.390625, 0.808125, 0.2, 0.863125), BooleanOp.OR);
            shape = Shapes.join(shape, Shapes.box(0.103125, 0.0, 0.190625, 0.903125, 0.1, 0.390625), BooleanOp.OR);
            shape = Shapes.join(shape, Shapes.box(0.205625, 0.2, 0.765625, 0.800625, 0.5025, 0.990625), BooleanOp.OR);
            shape.generateHorizontal()
        }
        val HANGED = BooleanProperty.create("hanged")!!
    }
}