package com.flooferland.showbiz.models

import AnimCommand
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.ShowbizClient
import com.flooferland.showbiz.blocks.entities.PlaybackControllerBlockEntity
import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity
import com.flooferland.showbiz.show.BitId
import com.flooferland.showbiz.utils.lerp
import software.bernie.geckolib.animatable.GeoAnimatable
import software.bernie.geckolib.animatable.stateless.StatelessAnimationController
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.animation.RawAnimation
import java.lang.Math.clamp

/** Responsible for fancy animation */
class StagedBotBlockEntityModel : BaseBotModel() {
    val bitSmooths = mutableMapOf<BitId, Double>()

    var lastTime = System.nanoTime()
    var triggeredBadAnimationError = false

    private fun nextDelta(): Double {
        val now = System.nanoTime()
        val delta = ((now - lastTime) / 1_000_000_000.0)  // To secs (there's no function for this I checked)
            .coerceIn(0.005, 0.3)
        lastTime = now
        return delta
    }

    fun getAnimId(animatable: StagedBotBlockEntity, bitOn: Boolean, anim: AnimCommand): String {
        val id = if (bitOn)
            anim.on ?: "${anim.id}.on"
        else
            anim.off ?: "${anim.id}.off"
        return "animation.${animatable.botId}.${id}"
    }

    override fun setCustomAnimations(animatable: StagedBotBlockEntity, instanceId: Long, state: AnimationState<StagedBotBlockEntity>) {
        val bot = ShowbizClient.bots[animatable.botId] ?: return

        // Getting the animation controller
        // TODO: Figure out why caching the block entity doesn't work
        val controller = animatable.controllerPos?.let {
            animatable.level?.getBlockEntity(it) as? PlaybackControllerBlockEntity
        }
        if (controller == null) {
            return
        }

        // Resetting bones
        val instanceCache = animatable.getAnimatableInstanceCache()
        val animManager = instanceCache.getManagerForId<GeoAnimatable>(0)
        for (mapping in bot.bitmap.bits.values) {
            mapping.rotate?.let { rotate ->
                val bone = animationProcessor.getBone(rotate.bone) ?: continue
                bone.rotX = 0f
                bone.rotY = 0f
                bone.rotZ = 0f
            }

            if (!controller.playing) {
                mapping.anim?.let { anim ->
                    animManager.stopTriggeredAnimation(getAnimId(animatable, true, anim))
                    animManager.stopTriggeredAnimation(getAnimId(animatable, false, anim))
                }
            }
        }
        if (!controller.playing) return

        // Driving animation
        for ((bit, mapping) in bot.bitmap.bits) {
            // Getting things
            val frame = controller.signal
            val flowSpeed = (mapping.flow.toFloat() * 10.0f)
            val bitOn = frame.frameHas(bit)
            val delta = nextDelta()

            // Animation
            mapping.anim?.let { anim ->
                val controllerKey = "ctrl_${bit}"

                // Adding controllers
                animManager?.let {
                    if (!animManager.animationControllers.contains(controllerKey)) {
                        val controller = StatelessAnimationController(animatable, controllerKey)
                        controller.transitionLength(5 + (mapping.flow * 5f).toInt())
                        animManager.addController(controller)
                    }
                }

                // Driving controllers
                val controller = animManager.animationControllers[controllerKey] as? StatelessAnimationController
                val animId = getAnimId(animatable, bitOn, anim)
                if (controller != null && controller.currentAnimation?.animation?.name != animId) {
                    // Checking if the animation exists
                    val playback = runCatching {
                        val animation = getAnimation(animatable, animId)
                        if (animation == null) {
                            if (!triggeredBadAnimationError)
                                Showbiz.log.error("Failed to play animation '$animId' (file=${ShowbizClient.bots[animatable.botId]?.animations})")
                            triggeredBadAnimationError = true
                            return@runCatching
                        } else {
                            triggeredBadAnimationError = false
                        }

                        controller.setCurrentAnimation(RawAnimation.begin().thenPlayAndHold(animId))
                    }
                    playback.onFailure { throwable ->
                        Showbiz.log.error("Exception occured while playing animation '$animId' on bot '${animatable.botId}'", throwable)
                    }
                }
            }

            // Manual rotation
            mapping.rotate?.let { rotate ->
                val bone = animationProcessor.getBone(rotate.bone) ?: continue

                // Smoothing
                val oldSmooth = bitSmooths.putIfAbsent(bit, 0.0) ?: 0.0
                val bitSmooth = clamp(
                    lerp(oldSmooth, if (bitOn) 1.0 else 0.0, clamp(flowSpeed * delta, 0.01, 10.0)),
                    0.0, 1.0
                )
                bitSmooths[bit] = bitSmooth

                // Applying
                bone.rotX += (Math.toRadians(rotate.target.x.toDouble()) * bitSmooth).toFloat()
                bone.rotY += (Math.toRadians(rotate.target.y.toDouble()) * bitSmooth).toFloat()
                bone.rotZ += (Math.toRadians(rotate.target.z.toDouble()) * bitSmooth).toFloat()
            }
        }
    }
}