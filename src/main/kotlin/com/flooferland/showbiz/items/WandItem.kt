package com.flooferland.showbiz.items

import com.flooferland.showbiz.Permissions.Companion.mayInteractWith
import com.flooferland.showbiz.Permissions.Companion.mayPlaceAt
import com.flooferland.showbiz.items.base.FancyItem
import com.flooferland.showbiz.registry.ModComponents
import com.flooferland.showbiz.registry.ModSounds
import com.flooferland.showbiz.types.OwnerId
import com.flooferland.showbiz.types.connection.AutoConnection
import com.flooferland.showbiz.types.connection.IConnectable
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.EntityHitResult
import org.apache.commons.lang3.mutable.MutableObject
import software.bernie.geckolib.animatable.GeoItem
import software.bernie.geckolib.animatable.SingletonGeoAnimatable
import software.bernie.geckolib.animatable.client.GeoRenderProvider
import software.bernie.geckolib.animation.*
import software.bernie.geckolib.util.GeckoLibUtil
import java.util.function.Consumer

class WandItem(id: ResourceLocation, properties: Properties) : FancyItem(id, properties), GeoItem {
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
        // Starting a link
        val heldConnection = stack.get(ModComponents.HeldConnection.type)
        if (last != null && heldConnection == null) {
            if (player.isCrouching) {  // TODO: Add a way to clear the connections from the ports sending things to the listener too (senders, not listeners)
                var count = 0
                last.connectionManager.outputs.forEach { (_, port) -> port.removeListeners { count += 1; true } }
                reset(player, stack, level, "Cleared $count listeners!")
                return InteractionResult.SUCCESS
            }
            stack.set(ModComponents.HeldConnection.type, OwnerId.of(last))
            finish(player, stack, level, ModSounds.End, "fire", "First target selected!")
            return InteractionResult.SUCCESS
        }

        // Checks
        val first = heldConnection?.grabConnectable(level)
        if (first == null || last == null) {
            reset(player, stack, level, null)
            return InteractionResult.CONSUME
        }
        if (first == last) {
            reset(player, stack, level, "You can't connect something to itself, what are you doing!!!")
            return InteractionResult.CONSUME
        }

        // Finishing a link
        val result = AutoConnection.make(first, last) ?: AutoConnection.make(last, first)
        if (result != null) {
            stack.remove(ModComponents.HeldConnection.type)
            finish(player, stack, level, ModSounds.End, "retract", result)
        } else {
            reset(player, stack, level, "These two can't be connected")
        }
        return InteractionResult.SUCCESS
    }

    fun finish(player: Player, stack: ItemStack, level: ServerLevel, sound: ModSounds, anim: String, msg: String?) {
        triggerAnim<WandItem>(player, GeoItem.getOrAssignId(stack, level), "main", anim)
        player.playNotifySound(sound.event, SoundSource.PLAYERS, 1.0f, 1.0f)
        val message = Component.empty()
        for (string in msg?.lines() ?: listOf()) message.append(Component.literal(string))
        player.displayClientMessage(message, true)
    }

    fun reset(player: Player, stack: ItemStack, level: ServerLevel, message: String?) {
        stack.remove(ModComponents.HeldConnection.type)
        finish(player, stack, level, sound = ModSounds.Deselect, anim = "retract", msg = message)
    }

    override fun use(level: Level, player: Player, usedHand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(usedHand)
        if (player.isCrouching && level is ServerLevel) {
            reset(player, stack, level, "Cleared!")
            return InteractionResultHolder.success(stack)
        }
        return InteractionResultHolder.pass(stack)
    }

    // Connecting blocks
    override fun useOn(ctx: UseOnContext): InteractionResult {
        val level = ctx.level as? ServerLevel ?: return InteractionResult.SUCCESS
        val player = ctx.player ?: return InteractionResult.PASS
        val pos = ctx.clickedPos
        val stack = ctx.itemInHand
        if (!player.mayPlaceAt(level, pos, stack)) return InteractionResult.FAIL
        val target = level.getBlockEntity(pos) as? IConnectable
        return link(player, stack, level, target)
    }

    // Connecting entities
    fun useOnEntity(player: Player, level: ServerLevel, stack: ItemStack, entity: Entity, result: EntityHitResult?): InteractionResult {
        val connectable = entity as? IConnectable ?: run { return link(player, stack, level, null) }
        if (!player.mayInteractWith(entity)) return InteractionResult.FAIL
        return link(player, stack, level, connectable)
    }

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {
        controllers.add(
            AnimationController(this, "main") { PlayState.CONTINUE }
                .triggerableAnim("fire", fireAnim)
                .triggerableAnim("retract", retractAnim)
        )
    }
}
