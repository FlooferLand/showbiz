package com.flooferland.showbiz.models

import com.flooferland.showbiz.blocks.entities.ReelHolderBlockEntity
import com.flooferland.showbiz.utils.rl
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.model.DefaultedBlockGeoModel

class ReelHolderModel : DefaultedBlockGeoModel<ReelHolderBlockEntity>(rl("reel_holder")) {
    override fun setCustomAnimations(animatable: ReelHolderBlockEntity, instanceId: Long, state: AnimationState<ReelHolderBlockEntity>) {
        animatable.inventory.forEachIndexed { slot, stack ->
            val bone = animationProcessor.getBone("reel$slot") ?: return@forEachIndexed
            bone.isHidden = stack.isEmpty
        }
    }
}