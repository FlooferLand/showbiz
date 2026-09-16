package com.flooferland.showbiz.models

import net.minecraft.resources.*
import net.minecraft.world.level.block.*
import com.flooferland.showbiz.items.base.GeoBlockItem
import com.flooferland.showbiz.registry.ModBlocks
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.model.DefaultedBlockGeoModel

class GenericBlockItemModel(assetSubpath: ResourceLocation) : DefaultedBlockGeoModel<GeoBlockItem>(assetSubpath) {
    var hiddenBones: Map<Block, Set<String>> = mapOf(
        ModBlocks.Spotlight.block to setOf(SpotlightModel.BONE_SUPPORT_TOP, SpotlightModel.BONE_SUPPORT_BOTTOM)
    )

    override fun setCustomAnimations(animatable: GeoBlockItem, instanceId: Long, animationState: AnimationState<GeoBlockItem>) {
        val hiddenBones = hiddenBones[animatable.block] ?: emptySet()

        // Resetting item bones to prevent them randomly changing in the hotbar
        animationProcessor.registeredBones.forEach { bone ->
            val snap = bone.initialSnapshot
            bone.rotX = snap.rotX
            bone.rotY = snap.rotY
            bone.rotZ = snap.rotZ
            bone.posX = snap.offsetX
            bone.posY = snap.offsetY
            bone.posZ = snap.offsetZ
            bone.scaleX = snap.scaleX
            bone.scaleY = snap.scaleY
            bone.scaleZ = snap.scaleZ
            bone.isHidden = hiddenBones.contains(bone.name)
            bone.resetStateChanges()
        }
    }
}