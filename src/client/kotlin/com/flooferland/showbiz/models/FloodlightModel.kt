package com.flooferland.showbiz.models

import net.minecraft.resources.*
import net.minecraft.util.*
import com.flooferland.showbiz.entities.FloodlightEntity
import com.flooferland.showbiz.utils.rl
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.model.DefaultedEntityGeoModel

class FloodlightModel : DefaultedEntityGeoModel<FloodlightEntity>(rl("floodlight")) {
    override fun getTextureResource(entity: FloodlightEntity): ResourceLocation =
        buildFormattedTexturePath(rl(if (entity.isLit) "floodlight_on" else "floodlight"))
    override fun setCustomAnimations(entity: FloodlightEntity, instanceId: Long, state: AnimationState<FloodlightEntity>) {
        val neck = fetchBone(BONE_NECK) ?: return
        val head = fetchBone(BONE_HEAD) ?: return

        val topSupport = fetchBone(BONE_SUPPORT_TOP) ?: return
        val bottomSupport = fetchBone(BONE_SUPPORT_BOTTOM) ?: return
        topSupport.isHidden = !entity.supportAbove
        bottomSupport.isHidden = !entity.supportBelow || entity.supportAbove

        neck.rotY = (entity.turn.x * -1f) * Mth.DEG_TO_RAD
        head.rotX = entity.turn.y * Mth.DEG_TO_RAD
    }

    fun fetchBone(boneName: String) =
        animationProcessor.getBone(boneName)?.also { it.resetStateChanges() }

    companion object {
        const val BONE_SUPPORT_TOP = "support_top"
        const val BONE_SUPPORT_BOTTOM = "support_bottom"
        const val BONE_NECK = "neck"
        const val BONE_HEAD = "head"
    }
}