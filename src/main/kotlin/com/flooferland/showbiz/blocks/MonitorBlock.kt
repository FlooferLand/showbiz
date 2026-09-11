package com.flooferland.showbiz.blocks

import net.minecraft.core.*
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.*
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.level.block.state.properties.*
import net.minecraft.world.phys.BlockHitResult
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

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(HANGED)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        val above = context.level.getBlockState(context.clickedPos.above())
        return super.getStateForPlacement(context)
            .setValue(HANGED, !above.isAir)
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