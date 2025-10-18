package com.flooferland.showbiz.items

import com.flooferland.showbiz.blocks.PlaybackControllerBlock
import com.flooferland.showbiz.blocks.entities.PlaybackControllerBlockEntity
import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity
import com.flooferland.showbiz.registry.ModComponents
import com.flooferland.showbiz.registry.ModSounds
import com.flooferland.showbiz.utils.Extensions.applyChange
import com.flooferland.showbiz.utils.Extensions.applyComponent
import net.minecraft.network.chat.*
import net.minecraft.sounds.*
import net.minecraft.world.*
import net.minecraft.world.item.*
import net.minecraft.world.item.context.*
import net.minecraft.world.level.block.entity.*
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

class WandItem(properties: Properties) : Item(properties) {
    override fun useOn(ctx: UseOnContext): InteractionResult {
        if (ctx.level.isClientSide) return super.useOn(ctx)
        val player = ctx.player ?: return super.useOn(ctx)
        val level = ctx.level

        val state = level.getBlockState(ctx.clickedPos)
        val comp = ctx.itemInHand.components.get(ModComponents.WandBind.type)!!
        val compBlockEntity: BlockEntity? = comp.pos.getOrNull()?.let { level.getBlockEntity(it) }
        val blockEntity = level.getBlockEntity(ctx.clickedPos)
        if (state.block is PlaybackControllerBlock) {
            player.displayClientMessage(Component.literal("Right click on a bot block next"), true)
            comp.pos = Optional.of(ctx.clickedPos)
            ctx.itemInHand.applyComponent(ModComponents.WandBind.type, comp)
            player.playNotifySound(ModSounds.Select.event, SoundSource.PLAYERS, 1.0f, 1.0f)
            return InteractionResult.SUCCESS
        } else if (blockEntity is StagedBotBlockEntity && compBlockEntity is PlaybackControllerBlockEntity) {
            // Setting the StagedBotBlockEntity's reference to the controller
            comp.pos.ifPresent { blockEntity.applyChange(true) { controllerPos = it } }
            comp.pos = Optional.empty()
            ctx.itemInHand.applyComponent(ModComponents.WandBind.type, comp)
            player.playNotifySound(ModSounds.End.event, SoundSource.PLAYERS, 1.0f, 1.0f)
            return InteractionResult.SUCCESS
        }

        comp.pos = Optional.empty()
        ctx.itemInHand.applyComponent(ModComponents.WandBind.type, comp)
        player.playNotifySound(ModSounds.Deselect.event, SoundSource.PLAYERS, 1.0f, 0.8f)
        return InteractionResult.SUCCESS
    }
}
