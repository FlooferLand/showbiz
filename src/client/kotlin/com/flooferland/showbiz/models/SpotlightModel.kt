package com.flooferland.showbiz.models

import net.minecraft.resources.ResourceLocation
import net.minecraft.util.*
import com.flooferland.showbiz.blocks.entities.SpotlightBlockEntity
import com.flooferland.showbiz.utils.rl
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.model.DefaultedBlockGeoModel

class SpotlightModel : DefaultedBlockGeoModel<SpotlightBlockEntity>(rl("spotlight")) {
    override fun getTextureResource(entity: SpotlightBlockEntity): ResourceLocation =
        buildFormattedTexturePath(rl(if (entity.isLit) "spotlight_on" else "spotlight"))
    override fun setCustomAnimations(entity: SpotlightBlockEntity, instanceId: Long, state: AnimationState<SpotlightBlockEntity>) {
        val neck = animationProcessor.getBone("neck") ?: return
        neck.updateRotation(0f, 0f, 0f)
        val head = animationProcessor.getBone("head") ?: return
        head.updateRotation(0f, 0f, 0f)

        val topSupport = animationProcessor.getBone("support_top") ?: return
        val bottomSupport = animationProcessor.getBone("support_bottom") ?: return
        topSupport.isHidden = !entity.supportAbove
        bottomSupport.isHidden = !entity.supportBelow || entity.supportAbove

        neck.rotY = (entity.turn.x * -1f) * Mth.DEG_TO_RAD
        head.rotX = entity.turn.y * Mth.DEG_TO_RAD
    }
}