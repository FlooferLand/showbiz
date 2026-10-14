package com.flooferland.showbiz.models

import net.minecraft.resources.*
import net.minecraft.util.*
import com.flooferland.showbiz.blocks.entities.SpotlightBlockEntity
import com.flooferland.showbiz.utils.rl
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.model.DefaultedBlockGeoModel

class SpotlightModel : DefaultedBlockGeoModel<SpotlightBlockEntity>(rl("spotlight")) {
    override fun getTextureResource(entity: SpotlightBlockEntity): ResourceLocation =
        buildFormattedTexturePath(rl(if (entity.isLit) "spotlight_on" else "spotlight"))
    override fun setCustomAnimations(entity: SpotlightBlockEntity, instanceId: Long, state: AnimationState<SpotlightBlockEntity>) {
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