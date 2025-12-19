package com.flooferland.showbiz.models

import net.minecraft.client.*
import net.minecraft.util.*
import com.flooferland.bizlib.bits.AnimCommand
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.ShowbizClient
import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity
import com.flooferland.showbiz.show.BitId
import com.flooferland.showbiz.utils.lerp
import java.lang.Math.clamp
import software.bernie.geckolib.animatable.GeoAnimatable
import software.bernie.geckolib.animatable.stateless.StatelessAnimationController
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.animation.RawAnimation
import kotlin.math.sin

// TODO: Make the bot model not share the same bitSmooths and other properties.
//       Currently, 2 shows containing the same bots cannot animate at the same time due to this shared memory.

/** Responsible for fancy animation */
class StagedBotBlockEntityModel : BaseBotModel() {
    val bitSmooths = mutableMapOf<BitId, Float>()
    val lastTimes = mutableMapOf<Long, Long>()

    // var reelToReel: ReelToReelBlockEntity? = null
    // var greybox: GreyboxBlockEntity? = null

    var triggeredBadAnimationError = false

    private fun wiggle(time: Float, freq: Float = 1.0f, amp: Float = 1.0f): Float {
        return sin(time * freq * Mth.PI * 2f) * amp
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
        val model = currentModel ?: return

        // Getting the animation controller/state
        // TODO: Figure out why caching these won't work
        /*val greybox = this.greybox ?: animatable.greyboxPos?.let {
            animatable.level?.getBlockEntity(it) as? GreyboxBlockEntity
        }
        val reelToReel = this.reelToReel ?: greybox?.reelToReelPos?.let {
            animatable.level?.getBlockEntity(it) as? ReelToReelBlockEntity
        }
        if (this.greybox == null || this.reelToReel == null) {
            this.greybox = greybox
            this.reelToReel = reelToReel
        }*/
        /*val greybox = animatable.greyboxPos?.let {
            animatable.level?.getBlockEntity(it) as? GreyboxBlockEntity
        }
        val reelToReel = greybox?.reelToReelPos?.let {
            animatable.level?.getBlockEntity(it) as? ReelToReelBlockEntity
        }
        if (greybox == null || reelToReel == null) return*/

        // Getting the bits via the mapping
        val mapping = animatable.show.data.mapping ?: return
        if (mapping.isEmpty()) return
        val bitmapBits = bot.bitmap.bits[mapping] ?: run {
            Showbiz.log.warn("Mapping '$mapping' not found for bot '${animatable.botId}'. Skipping bot animation step")
            return
        }

        // Resetting bones
        val instanceCache = animatable.getAnimatableInstanceCache()
        val animManager = instanceCache.getManagerForId<GeoAnimatable>(0)
        for ((_, data) in bitmapBits) {
            for (rotate in data.rotates) {
                val bone = animationProcessor.getBone(rotate.bone) ?: continue
                val initRot = model.initBoneRots[bone.name]
                bone.rotX = initRot?.x ?: 0f
                bone.rotY = initRot?.y ?: 0f
                bone.rotZ = initRot?.z ?: 0f
            }

            for (move in data.moves) {
                val bone = animationProcessor.getBone(move.bone) ?: continue
                val initMove = model.initBoneMoves[bone.name]
                bone.posX = initMove?.x ?: 0f
                bone.posY = initMove?.y ?: 0f
                bone.posZ = initMove?.z ?: 0f
            }

            if (!animatable.show.data.playing) {
                data.anim?.let { anim ->
                    animManager.stopTriggeredAnimation(getAnimId(animatable, true, anim))
                    animManager.stopTriggeredAnimation(getAnimId(animatable, false, anim))
                }
            }
        }

        // TODO: FIXME: WHY THE F#@! IS 'PLAYING' FALSE??
        // if (!animatable.show.data.playing) return

        // Driving animation
        val delta = Minecraft.getInstance().timer.gameTimeDeltaTicks
        for ((bit, data) in bitmapBits) {
            // Getting things
            val frame = animatable.show.data.signal
            val flowSpeed = (data.flow.toFloat() * 0.2f)
            val bitOn = frame.frameHas(bit)

            // Animation
            // TODO: Fix animations sometimes snapping
            // TODO: Fix animations sometimes holding for no reason
            data.anim?.let { anim ->
                val controllerKey = "ctrl_$bit"

                // Adding controllers
                animManager?.let {
                    if (!animManager.animationControllers.contains(controllerKey)) {
                        val controller = StatelessAnimationController(animatable, controllerKey)
                        controller.transitionLength(5 + (data.flow * 5f).toInt())
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

            // Manual smoothing
            val oldSmooth = bitSmooths.putIfAbsent(bit, 0.0f) ?: 0.0f
            val bitSmooth = clamp(
                lerp(oldSmooth, if (bitOn) 1.0f else 0.0f, clamp(flowSpeed * delta, 0.01f, 10.0f)),
                0.0f, 1.0f
            )
            bitSmooths[bit] = bitSmooth

            // Manual rotation
            for (rotate in data.rotates) {
                val bone = animationProcessor.getBone(rotate.bone) ?: continue

                // Applying movement
                bone.rotX += (rotate.target.x * Mth.DEG_TO_RAD) * bitSmooth
                bone.rotY += (rotate.target.y * Mth.DEG_TO_RAD) * bitSmooth
                bone.rotZ += (rotate.target.z * Mth.DEG_TO_RAD) * bitSmooth

                // Applying wiggle
                // TODO: Scale the wiggle intensity and frequency based on bone length
                val wiggle = 0.05f
                val time = System.nanoTime() / 1_000_000_000.0f
                bone.rotX += wiggle(time + 0.0f, freq = 2.0f, amp = wiggle * bitSmooth * (1.0f - bitSmooth))
                bone.rotY += wiggle(time + 0.2f, freq = 2.0f, amp = wiggle * bitSmooth * (1.0f - bitSmooth))
                bone.rotZ += wiggle(time + 0.4f, freq = 2.0f, amp = wiggle * bitSmooth * (1.0f - bitSmooth))
                bone.rotX += wiggle(time + 0.0f, freq = 0.5f, amp = (wiggle * 2f) * bitSmooth * (1.0f - bitSmooth))
                bone.rotY += wiggle(time + 0.2f, freq = 0.5f, amp = (wiggle * 2f) * bitSmooth * (1.0f - bitSmooth))
                bone.rotZ += wiggle(time + 0.4f, freq = 0.5f, amp = (wiggle * 2f) * bitSmooth * (1.0f - bitSmooth))
            }

            // Manual position
            for (move in data.moves) {
                val bone = animationProcessor.getBone(move.bone) ?: continue

                // Applying movement
                bone.posX += (move.target.x.toDouble() * bitSmooth).toFloat()
                bone.posY += (move.target.y.toDouble() * bitSmooth).toFloat()
                bone.posZ += (move.target.z.toDouble() * bitSmooth).toFloat()
            }
        }
    }
}