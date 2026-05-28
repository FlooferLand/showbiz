package com.flooferland.showbiz.models

import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.blocks.entities.CymbalBlockEntity
import com.flooferland.showbiz.types.ClientCollidePartInstance
import com.flooferland.showbiz.types.collidepart.CollidePartId
import com.flooferland.showbiz.types.math.Vec3f
import com.flooferland.showbiz.utils.Extensions.secsToTicks
import com.flooferland.showbiz.utils.rl
import java.util.WeakHashMap
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.model.DefaultedBlockGeoModel
import kotlin.math.sin
import kotlin.math.sqrt

class CymbalModel : DefaultedBlockGeoModel<CymbalBlockEntity>(rl("cymbal")) {
    data class CymbalState(var lastHitTime: Int = 0, val force: Vec3f = Vec3f())
    val states = WeakHashMap<CymbalBlockEntity, CymbalState>()

    override fun setCustomAnimations(animatable: CymbalBlockEntity, instanceId: Long, animState: AnimationState<CymbalBlockEntity>) {
        val instance = animatable.collidePartInstance.clientInstance as? ClientCollidePartInstance ?: return
        val entity = instance.spawned.values.firstOrNull { it.partId == CollidePartId.Cymbal } ?: return

        val cymbal = animationProcessor.getBone("cymbal") ?: return
        cymbal.updateRotation(0f, 0f, 0f)

        val state = states.getOrPut(animatable) { CymbalState() }
        if (entity.lastHitTime != state.lastHitTime) {
            state.lastHitTime = entity.lastHitTime

            val length = sqrt(entity.hitDirection.x * entity.hitDirection.x + entity.hitDirection.z * entity.hitDirection.z)
            val nx = if (length > 0f) entity.hitDirection.x / length else 1.0
            val nz = if (length > 0f) entity.hitDirection.z / length else 0.0

            val facing = animatable.blockState.getValue(FacingEntityBlock.FACING)
            val (sx, sz) = Pair(facing.stepX.toDouble(), facing.stepZ.toDouble())
            state.force.x = (nx * sz - nz * sx).toFloat()
            state.force.z = (nx * sx + nz * sz).toFloat()
        }

        val ticks = entity.tickCount + animState.partialTick
        val timeSinceHit = ticks - state.lastHitTime
        val duration = (2.3).secsToTicks().toDouble()
        if (timeSinceHit in 0.0..duration) {
            val decay = (duration - timeSinceHit) / duration
            val wobble = sin(timeSinceHit) * 0.4f * decay
            cymbal.rotX += wobble.toFloat() * state.force.z
            cymbal.rotZ += wobble.toFloat() * -state.force.x
        }

        states[animatable] = state
    }
}