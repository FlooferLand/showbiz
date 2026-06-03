package com.flooferland.showbiz.types

import software.bernie.geckolib.animatable.GeoAnimatable
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.animation.AnimationController
import software.bernie.geckolib.animation.PlayState
import software.bernie.geckolib.util.GeckoLibUtil

class BotPreviewAnimatable(override var botId: ResourceId?) : GeoAnimatable, IBot {
    val cache = GeckoLibUtil.createInstanceCache(this)!!

    override fun getAnimatableInstanceCache() = cache
    override fun getTick(obj: Any?) = 0.0

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {
        controllers.add(AnimationController(this, "main") { PlayState.CONTINUE })
    }
}