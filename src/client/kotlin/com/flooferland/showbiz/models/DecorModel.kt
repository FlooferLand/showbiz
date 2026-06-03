package com.flooferland.showbiz.models

import net.minecraft.world.phys.*
import com.flooferland.showbiz.ShowbizClient
import com.flooferland.showbiz.entities.DecorEntity
import com.flooferland.showbiz.utils.lerp
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


        val delta = ShowbizClient.getDeltaTime().coerceAtMost(0.25f)
        val pos = animatable.position()
        if (animatable.oldPos == Vec3.ZERO) animatable.oldPos = pos
        val diff = pos.subtract(animatable.oldPos).scale(30.0)
        val vSquish = (0.8f + diff.y.toFloat()).coerceIn(0.6f..1f)
        val hSquish = (1.3f - diff.y.toFloat()).coerceIn(0.6f..1f)
        root.rotX = lerp(root.rotX, diff.x.toFloat(), 0.5f * delta)
        root.rotZ = lerp(root.rotX, diff.z.toFloat(), 0.5f * delta)
        root.scaleY = lerp(root.scaleY, vSquish, 0.2f * delta)
        root.scaleX = lerp(root.scaleX, hSquish, 0.2f * delta)
        root.scaleZ = lerp(root.scaleZ, hSquish, 0.2f * delta)
        bottom.rotX = lerp(bottom.rotX, diff.x.toFloat() * 0.6f, 0.5f * delta)
        bottom.rotY = lerp(bottom.rotY, diff.y.toFloat() * 0.2f, 0.5f * delta)
        bottom.rotZ = lerp(bottom.rotX, diff.z.toFloat() * 0.6f, 0.5f * delta)
        animatable.oldPos = pos
    }
}