package com.flooferland.showbiz.models

import net.minecraft.util.*
import com.flooferland.showbiz.blocks.entities.SpotlightBlockEntity
import com.flooferland.showbiz.utils.rl
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.model.DefaultedBlockGeoModel

class SpotlightModel : DefaultedBlockGeoModel<SpotlightBlockEntity>(rl("spotlight")) {
    override fun setCustomAnimations(animatable: SpotlightBlockEntity, instanceId: Long, state: AnimationState<SpotlightBlockEntity>) {
        val neck = animationProcessor.getBone("neck") ?: return
        neck.updateRotation(0f, 0f, 0f)
        val head = animationProcessor.getBone("head") ?: return
        head.updateRotation(0f, 0f, 0f)

        neck.rotY = (animatable.turn.x * -1f) * Mth.DEG_TO_RAD
        head.rotX = animatable.turn.y * Mth.DEG_TO_RAD
    }
}