package com.flooferland.showbiz.models

import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity
import com.flooferland.showbiz.utils.rl
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.model.DefaultedGeoModel

class StagedBotBlockEntityModel : DefaultedGeoModel<StagedBotBlockEntity>(rl("conner")) {
    override fun subtype(): String = "block"
    override fun getAnimationResource(animatable: StagedBotBlockEntity): ResourceLocation? = null

    override fun setCustomAnimations(animatable: StagedBotBlockEntity, instanceId: Long, animationState: AnimationState<StagedBotBlockEntity>) {
        val base = animationProcessor.getBone("animatronic")
        base.posY = 16.0f
    }
}