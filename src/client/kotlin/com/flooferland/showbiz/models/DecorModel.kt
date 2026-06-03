package com.flooferland.showbiz.models

import com.flooferland.showbiz.entities.DecorEntity
import com.flooferland.showbiz.utils.rl
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.model.GeoModel

@Suppress("OVERRIDE_DEPRECATION")
class DecorModel : GeoModel<DecorEntity>() {
    override fun getAnimationResource(animatable: DecorEntity?) = null
    override fun getModelResource(animatable: DecorEntity) = when (animatable.decorId) {
        DecorEntity.Id.PomPom -> rl("geo/entity/pom_pom.geo.json")
    }
    override fun getTextureResource(animatable: DecorEntity) = when (animatable.decorId) {
        DecorEntity.Id.PomPom -> rl("textures/pom_pom.png")
    }

    override fun setCustomAnimations(animatable: DecorEntity, instanceId: Long, animationState: AnimationState<DecorEntity>) {
        val root = animationProcessor.getBone("root") ?: return
        val bottom = animationProcessor.getBone("bottom") ?: return
    }
}