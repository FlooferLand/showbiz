package com.flooferland.showbiz.models

import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity
import com.flooferland.showbiz.utils.rl
import net.minecraft.resources.*
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.model.DefaultedGeoModel
import kotlin.math.sin

class StagedBotBlockEntityModel : DefaultedGeoModel<StagedBotBlockEntity>(rl("conner")) {
    override fun subtype(): String = "block"
    override fun getAnimationResource(animatable: StagedBotBlockEntity): ResourceLocation? = null


    override fun setCustomAnimations(animatable: StagedBotBlockEntity, instanceId: Long, state: AnimationState<StagedBotBlockEntity>) {
        val base = animationProcessor.getBone("animatronic")
        base.posY = 16.0f

        // Test animation
        // TODO: Remove constant query to the world once the BlockEntity desync bug is fixed
        val be = animatable.level?.getBlockEntity(animatable.blockPos) as? StagedBotBlockEntity
        println("${be?.playing}")
        if (be != null && be.playing) {
            val upperBody = animationProcessor.getBone("upper_body")
            upperBody.rotX = sin(state.animationTick * 0.5f).toFloat() * 0.1f

            val lowerBody = animationProcessor.getBone("lower_body")
            lowerBody.rotX = sin(state.animationTick * 0.8f).toFloat() * 0.05f

            val upperArmRight = animationProcessor.getBone("upper_arm_r")
            upperArmRight.rotX = sin(state.animationTick * 0.4f).toFloat() * 0.15f

            val lowerArmRight = animationProcessor.getBone("lower_arm_r")
            lowerArmRight.rotX = sin(state.animationTick * 0.4f).toFloat() * 0.15f

            val upperArmLeft = animationProcessor.getBone("upper_arm_l")
            upperArmLeft.rotX = sin(state.animationTick * 0.5f).toFloat() * 0.15f

            val lowerArmLeft = animationProcessor.getBone("lower_arm_l")
            lowerArmLeft.rotX = sin(state.animationTick * 0.5f).toFloat() * 0.15f

            val head = animationProcessor.getBone("head")
            head.rotX = sin(state.animationTick * 0.5f).toFloat() * 0.2f
        }
    }
}