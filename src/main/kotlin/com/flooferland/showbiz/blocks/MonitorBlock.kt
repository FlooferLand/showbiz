package com.flooferland.showbiz.blocks

import net.minecraft.core.*
import net.minecraft.network.chat.*
import net.minecraft.world.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.item.context.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.level.block.state.properties.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.types.FFmpeg
import com.flooferland.showbiz.utils.Extensions.click

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