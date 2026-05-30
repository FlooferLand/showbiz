package com.flooferland.showbiz.items

import net.minecraft.network.chat.*
import net.minecraft.world.*
import net.minecraft.world.item.*
import net.minecraft.world.item.context.*
import com.flooferland.showbiz.components.PlushComponent
import com.flooferland.showbiz.entities.PlushEntity
import com.flooferland.showbiz.registry.ModComponents
import com.flooferland.showbiz.utils.Extensions.applyComponent
import com.flooferland.showbiz.utils.rl
import java.util.function.Consumer
import org.apache.commons.lang3.mutable.MutableObject
import software.bernie.geckolib.animatable.GeoItem
import software.bernie.geckolib.animatable.SingletonGeoAnimatable
import software.bernie.geckolib.animatable.client.GeoRenderProvider
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

class PlushItem(properties: Properties) : Item(properties), GeoItem {
    val renderProviderHolder = MutableObject<GeoRenderProvider>()
    val cache = GeckoLibUtil.createInstanceCache(this)!!
    override fun getAnimatableInstanceCache() = cache
    override fun createGeoRenderer(consumer: Consumer<GeoRenderProvider>) = consumer.accept(renderProviderHolder.value)
    init {
        SingletonGeoAnimatable.registerSyncedAnimatable(this)
    }
    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) = Unit

    override fun getName(stack: ItemStack): Component {
        val id = stack.get(ModComponents.Plush.type)?.id ?: return super.getName(stack)
        return Component.translatable("item.${id.namespace}.plush.${id.path}");
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level ?: return InteractionResult.PASS
        val player = context.player ?: return InteractionResult.PASS
        if (level.isClientSide) return InteractionResult.SUCCESS

        val placeState = level.getBlockState(context.clickedPos.above())
        val canPlaceOnBlock = placeState.isAir || !placeState.isCollisionShapeFullBlock(level, context.clickedPos.above())
        if (canPlaceOnBlock && context.hand == InteractionHand.MAIN_HAND) {
            val entity = PlushEntity(level)
            entity.setPos(context.clickLocation)
            entity.yRot = 180f + context.rotation
            entity.itemStack = context.itemInHand
            level.addFreshEntity(entity)
            player.setItemInHand(context.hand, ItemStack.EMPTY)
        }
        return InteractionResult.PASS
    }

    override fun getDefaultInstance() =
        ItemStack(this).also { it.applyComponent(ModComponents.Plush.type, PlushComponent(rl("mitzi"))) }
}