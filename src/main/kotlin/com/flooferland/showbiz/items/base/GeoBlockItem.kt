package com.flooferland.showbiz.items.base

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.*
import com.flooferland.showbiz.blocks.base.FancyBlockItem
import java.util.function.Consumer
import org.apache.commons.lang3.mutable.MutableObject
import software.bernie.geckolib.animatable.GeoItem
import software.bernie.geckolib.animatable.SingletonGeoAnimatable
import software.bernie.geckolib.animatable.client.GeoRenderProvider
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

class GeoBlockItem(id: ResourceLocation, block: Block, properties: Properties) : FancyBlockItem(id, block, properties), GeoItem {
    val renderProviderHolder = MutableObject<GeoRenderProvider>()
    val cache = GeckoLibUtil.createInstanceCache(this)!!
    override fun getAnimatableInstanceCache() = cache
    override fun createGeoRenderer(consumer: Consumer<GeoRenderProvider>) = consumer.accept(renderProviderHolder.value)

    init {
        SingletonGeoAnimatable.registerSyncedAnimatable(this)
    }
    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) = Unit
}