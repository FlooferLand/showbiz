package com.flooferland.showbiz.items

import com.flooferland.showbiz.blocks.StagedBotBlock
import com.flooferland.showbiz.blocks.entities.PlaybackControllerBlockEntity
import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity
import com.flooferland.showbiz.registry.ModComponents
import com.flooferland.showbiz.registry.ModSounds
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
        if (state.block is StagedBotBlock) {
            player.displayClientMessage(Component.literal("Right click on a playback control block next"), true)
            comp.pos = Optional.of(ctx.clickedPos)
            ctx.itemInHand.applyComponent(ModComponents.WandBind.type, comp)
            player.displayClientMessage(Component.literal("Wawa '${comp.pos.getOrNull()}'!"), false)
            player.playNotifySound(ModSounds.Select.event, SoundSource.PLAYERS, 1.0f, 1.0f)
            return InteractionResult.CONSUME
        } else if (blockEntity is PlaybackControllerBlockEntity && compBlockEntity is StagedBotBlockEntity) {
            // Binding the previous block to the current one
            comp.pos.ifPresent { blockEntity.boundBot = it }

            // Binding the current block to the previous one
            // ..

            comp.pos = Optional.empty()
            ctx.itemInHand.applyComponent(ModComponents.WandBind.type, comp)
            player.playNotifySound(ModSounds.End.event, SoundSource.PLAYERS, 1.0f, 1.0f)
            return InteractionResult.CONSUME
        }

        comp.pos = Optional.empty()
        ctx.itemInHand.applyComponent(ModComponents.WandBind.type, comp)
        player.playNotifySound(ModSounds.Deselect.event, SoundSource.PLAYERS, 1.0f, 0.8f)
        return InteractionResult.CONSUME
    }
}
