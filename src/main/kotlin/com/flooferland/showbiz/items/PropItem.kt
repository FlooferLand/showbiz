package com.flooferland.showbiz.items

import com.flooferland.showbiz.Permissions.Companion.mayPlaceAt
import com.flooferland.showbiz.components.PropComponent
import com.flooferland.showbiz.entities.PropEntity
import com.flooferland.showbiz.registry.ModComponents
import com.flooferland.showbiz.types.IStackPlacePreview
import com.flooferland.showbiz.utils.Extensions.applyComponent
import com.flooferland.showbiz.utils.rl
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import org.apache.commons.lang3.mutable.MutableObject
import software.bernie.geckolib.animatable.GeoItem
import software.bernie.geckolib.animatable.SingletonGeoAnimatable
import software.bernie.geckolib.animatable.client.GeoRenderProvider
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil
import java.util.function.Consumer

class PropItem(properties: Properties) : Item(properties), GeoItem, IStackPlacePreview {
    val renderProviderHolder = MutableObject<GeoRenderProvider>()
    val cache = GeckoLibUtil.createInstanceCache(this)!!
    override fun getAnimatableInstanceCache() = cache
    override fun createGeoRenderer(consumer: Consumer<GeoRenderProvider>) = consumer.accept(renderProviderHolder.value)
    init {
        SingletonGeoAnimatable.registerSyncedAnimatable(this)
    }
    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) = Unit

    override fun getName(stack: ItemStack): Component {
        val id = stack.get(ModComponents.Prop.type)?.id ?: return super.getName(stack)
        return Component.translatable("item.${id.namespace}.prop.${id.path}");
    }

    override fun getEntity(level: Level, stack: ItemStack) = PropEntity(level, stack)

    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level ?: return InteractionResult.SUCCESS
        val player = context.player ?: return InteractionResult.PASS
        if (level.isClientSide) return InteractionResult.SUCCESS

        val pos = context.clickedPos.above()
        val stack = context.itemInHand
        if (!player.mayPlaceAt(level, pos, stack)) return InteractionResult.FAIL

        val placeState = level.getBlockState(pos)
        val canPlaceOnBlock = placeState.isAir || !placeState.isCollisionShapeFullBlock(level, pos)
        if (canPlaceOnBlock) {
            place(level, player, stack, context.clickLocation)
        }
        return InteractionResult.SUCCESS
    }

    fun place(level: Level, player: Player, stack: ItemStack, pos: Vec3) {
        if (!player.mayBuild()) return
        if (stack.isEmpty) return

        val entity = PropEntity(level, stack)
        placeEntity(entity, pos, player)
        entity.playPlaceSound(player)
        level.addFreshEntity(entity)
        stack.shrink(1)
    }

    override fun getDefaultInstance() =
        ItemStack(this).also { it.applyComponent(ModComponents.Prop.type, PropComponent(rl("moonshine"))) }
}