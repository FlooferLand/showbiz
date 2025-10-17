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

        val upperBody = animationProcessor.getBone("upper_body")
        val lowerBody = animationProcessor.getBone("lower_body")
        val upperArmRight = animationProcessor.getBone("upper_arm_r")
        val lowerArmRight = animationProcessor.getBone("lower_arm_r")
        val upperArmLeft = animationProcessor.getBone("upper_arm_l")
        val lowerArmLeft = animationProcessor.getBone("lower_arm_l")
        val head = animationProcessor.getBone("head")

        upperBody.rotX = 0f
        lowerBody.rotX = 0f
        upperArmRight.rotX = 0f
        lowerArmRight.rotX = 0f
        upperArmLeft.rotX = 0f
        lowerArmLeft.rotX = 0f
        head.rotX = 0f

        // Test animation
        if (animatable.playing) {
            upperBody.rotX = sin(state.animationTick * 0.5f).toFloat() * 0.1f
            lowerBody.rotX = sin(state.animationTick * 0.8f).toFloat() * 0.05f
            upperArmRight.rotX = sin(state.animationTick * 0.4f).toFloat() * 0.15f
            lowerArmRight.rotX = sin(state.animationTick * 0.4f).toFloat() * 0.15f
            upperArmLeft.rotX = sin(state.animationTick * 0.5f).toFloat() * 0.15f
            lowerArmLeft.rotX = sin(state.animationTick * 0.5f).toFloat() * 0.15f
            head.rotX = sin(state.animationTick * 0.5f).toFloat() * 0.2f
        }
    }
}