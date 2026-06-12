package com.flooferland.showbiz.items

import net.minecraft.*
import net.minecraft.network.chat.*
import net.minecraft.server.level.*
import net.minecraft.sounds.*
import net.minecraft.world.*
import net.minecraft.world.entity.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.item.*
import net.minecraft.world.item.context.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.registry.ModComponents
import com.flooferland.showbiz.registry.ModSounds
import com.flooferland.showbiz.types.connection.AutoConnection
import com.flooferland.showbiz.types.connection.ConnectionOwnerId
import com.flooferland.showbiz.types.connection.IConnectable
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

    fun link(player: Player, stack: ItemStack, level: ServerLevel, last: IConnectable?): InteractionResult {
        val owner = stack.get(ModComponents.HeldConnection.type)
        if (last != null && owner == null) {
            stack.set(ModComponents.HeldConnection.type, ConnectionOwnerId.of(last))
            finish(player, stack, level, ModSounds.End, "fire", "First target selected!")
            return InteractionResult.SUCCESS
        }

        val first = owner?.grabConnectable(level)
        if (first == null || last == null) {
            reset(player, stack, level, "First target cleared")
            return InteractionResult.CONSUME
        }
        if (first == last) {
            reset(player, stack, level, "You can't connect something to itself, what are you doing!!!")
            return InteractionResult.CONSUME
        }

        val result = AutoConnection.make(first, last) ?: AutoConnection.make(last, first)
        if (result != null) {
            stack.remove(ModComponents.HeldConnection.type)
            finish(player, stack, level, ModSounds.End, "retract", result)
        } else {
            reset(player, stack, level, "No matching ports found")
        }
        return InteractionResult.SUCCESS
    }

    fun finish(player: Player, stack: ItemStack, level: ServerLevel, sound: ModSounds, anim: String, msg: String) {
        triggerAnim<WandItem>(player, GeoItem.getOrAssignId(stack, level), "main", anim)
        player.playNotifySound(sound.event, SoundSource.PLAYERS, 1.0f, 1.0f)
        val message = Component.empty()
        for (string in msg.lines()) message.append(Component.literal(string))
        player.displayClientMessage(message, true)
    }

    fun reset(player: Player, stack: ItemStack, level: ServerLevel, error: String) {
        stack.remove(ModComponents.HeldConnection.type)
        finish(player, stack, level, sound = ModSounds.Deselect, anim = "retract", msg = error)
    }

    // Connecting blocks
    override fun useOn(ctx: UseOnContext): InteractionResult {
        if (ctx.level.isClientSide) return super.useOn(ctx)
        val player = ctx.player ?: return InteractionResult.PASS
        val level = ctx.level as? ServerLevel ?: return InteractionResult.PASS
        val target = level.getBlockEntity(ctx.clickedPos) as? IConnectable ?: return InteractionResult.PASS
        return link(player, ctx.itemInHand, level, target)
    }

    // Connecting entities
    fun useOnEntity(player: Player, level: ServerLevel, stack: ItemStack, entity: Entity, result: EntityHitResult?): InteractionResult {
        val connectable = entity as? IConnectable ?: run { return link(player, stack, level, null) }
        return link(player, stack, level, connectable)
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
