package com.flooferland.showbiz.items

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.*
import net.minecraft.server.level.*
import net.minecraft.sounds.*
import net.minecraft.world.*
import net.minecraft.world.item.*
import net.minecraft.world.item.context.*
import com.flooferland.showbiz.blocks.entities.CurtainBlockEntity
import com.flooferland.showbiz.blocks.entities.GreyboxBlockEntity
import com.flooferland.showbiz.blocks.entities.ReelToReelBlockEntity
import com.flooferland.showbiz.blocks.entities.ShowParserBlockEntity
import com.flooferland.showbiz.blocks.entities.SpeakerBlockEntity
import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity
import com.flooferland.showbiz.registry.ModComponents
import com.flooferland.showbiz.registry.ModSounds
import com.flooferland.showbiz.utils.Extensions.applyChange
import com.flooferland.showbiz.utils.Extensions.applyComponent
import java.util.Optional
import java.util.function.Consumer
import org.apache.commons.lang3.mutable.MutableObject
import software.bernie.geckolib.animatable.GeoItem
import software.bernie.geckolib.animatable.SingletonGeoAnimatable
import software.bernie.geckolib.animatable.client.GeoRenderProvider
import software.bernie.geckolib.animation.*
import software.bernie.geckolib.util.GeckoLibUtil

class WandItem(properties: Properties) : Item(properties), GeoItem {
    val renderProviderHolder = MutableObject<GeoRenderProvider>()
    val cache = GeckoLibUtil.createInstanceCache(this)!!
    override fun createGeoRenderer(consumer: Consumer<GeoRenderProvider>) = consumer.accept(renderProviderHolder.value)
    override fun getAnimatableInstanceCache() = cache

    init {
        SingletonGeoAnimatable.registerSyncedAnimatable(this)
    }

    val fireAnim = RawAnimation.begin().thenPlay("animation.wand.fire")?.then("animation.wand.shake", Animation.LoopType.LOOP)!!
    val retractAnim = RawAnimation.begin().thenPlayAndHold("animation.wand.retract")!!

    override fun useOn(ctx: UseOnContext): InteractionResult {
        if (ctx.level.isClientSide) return super.useOn(ctx)
        val player = ctx.player ?: return InteractionResult.CONSUME
        val level = ctx.level as? ServerLevel ?: return InteractionResult.CONSUME

        val first = ctx.itemInHand.components.get(ModComponents.WandBind.type)!!
        val lastEntity = level.getBlockEntity(ctx.clickedPos) ?: return InteractionResult.CONSUME

        fun finish(sound: ModSounds, anim: String, message: String? = null) {
            ctx.itemInHand.applyComponent(ModComponents.WandBind.type, first)
            triggerAnim<WandItem>(player, GeoItem.getOrAssignId(ctx.itemInHand, level), "main", anim)
            player.playNotifySound(sound.event, SoundSource.PLAYERS, 1.0f, 1.0f)
            message?.let {
                player.displayClientMessage(Component.literal(it), true)
            }
        }

        fun reset(error: String? = null) {
            first.pos = Optional.empty()
            finish(sound = ModSounds.Deselect, anim = "retract", message = "Deselected" + (error?.let { "; $it" } ?: ""))
        }

        if (first.pos.isEmpty) {
            when (lastEntity) {
                is ReelToReelBlockEntity,
                is GreyboxBlockEntity,
                is StagedBotBlockEntity,
                is ShowParserBlockEntity,
                is SpeakerBlockEntity,
                is CurtainBlockEntity
                -> {
                    first.pos = Optional.of(lastEntity.blockPos)
                    finish(sound = ModSounds.End, anim = "fire", message = "Select the next block")
                    return InteractionResult.SUCCESS
                }
                else -> { reset(); return InteractionResult.CONSUME }
            }
        } else {
            // TODO: Add automatic linking instead of dealing with this wand useOn monstrosity
            val firstEntity = level.getBlockEntity(first.pos.get())
            when (lastEntity) {
                is GreyboxBlockEntity if firstEntity is StagedBotBlockEntity -> {
                    val (greybox, stagedBot) = Pair(lastEntity, firstEntity)
                    greybox.applyChange(true) {
                        show.bindListener(stagedBot)
                    }
                    first.pos = Optional.empty()
                    finish(sound = ModSounds.End, anim = "retract", message = "Bot added")
                    return InteractionResult.SUCCESS
                }
                is GreyboxBlockEntity if firstEntity is ReelToReelBlockEntity -> {
                    val (greybox, reelToReel) = Pair(lastEntity, firstEntity)
                    reelToReel.applyChange(true) {
                        showOut.bindListener(greybox)
                        audioOut.bindListener(greybox)
                    }
                    first.pos = Optional.empty()
                    finish(sound = ModSounds.End, anim = "retract", message = "Reel-to-reel added")
                    return InteractionResult.SUCCESS
                }
                is GreyboxBlockEntity if firstEntity is ShowParserBlockEntity -> {
                    val (greybox, showParser) = Pair(lastEntity, firstEntity)
                    greybox.applyChange(true) {
                        show.bindListener(showParser)
                    }
                    first.pos = Optional.empty()
                    finish(sound = ModSounds.End, anim = "retract", message = "Show parser added")
                    return InteractionResult.SUCCESS
                }
                is GreyboxBlockEntity if firstEntity is SpeakerBlockEntity -> {
                    val (greybox, speaker) = Pair(lastEntity, firstEntity)
                    greybox.applyChange(true) {
                        audio.bindListener(speaker)
                    }
                    first.pos = Optional.empty()
                    finish(sound = ModSounds.End, anim = "retract", message = "Speaker added")
                    return InteractionResult.SUCCESS
                }
                is GreyboxBlockEntity if firstEntity is CurtainBlockEntity -> {
                    val (greybox, curtain) = Pair(lastEntity, firstEntity)
                    greybox.applyChange(true) {
                        show.bindListener(curtain)
                    }
                    curtain.applyChange(true) {
                        curtain.isOpen = false
                    }
                    first.pos = Optional.empty()
                    finish(sound = ModSounds.End, anim = "retract", message = "Curtain added")
                    return InteractionResult.SUCCESS
                }
                else -> { reset(error = "Unknown order"); return InteractionResult.CONSUME }
            }
        }
    }

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {
        controllers.add(
            AnimationController(this, "main") { PlayState.CONTINUE }
                .triggerableAnim("fire", fireAnim)
                .triggerableAnim("retract", retractAnim)
        )
    }

    override fun appendHoverText(stack: ItemStack, context: TooltipContext, tooltip: MutableList<Component>, tooltipFlag: TooltipFlag) {
        tooltip.add(
            Component.literal("Use this wand").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(" (totally not a pneumatic cylinder)").withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY))
        )
        tooltip.add(
            Component.literal("to create wires between blocks").withStyle(ChatFormatting.GRAY)
        )
    }
}
