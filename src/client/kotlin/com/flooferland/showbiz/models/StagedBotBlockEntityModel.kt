package com.flooferland.showbiz.models

import net.minecraft.client.*
import net.minecraft.sounds.SoundSource
import net.minecraft.util.*
import com.flooferland.bizlib.bits.AnimCommand
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.ShowbizClient
import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity
import com.flooferland.showbiz.registry.ModSounds
import com.flooferland.showbiz.show.BitId
import com.flooferland.showbiz.utils.lerp
import java.lang.Math.clamp
import software.bernie.geckolib.animatable.GeoAnimatable
import software.bernie.geckolib.animatable.stateless.StatelessAnimationController
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.animation.RawAnimation
import software.bernie.geckolib.cache.`object`.GeoBone

/** Responsible for fancy animation */
class StagedBotBlockEntityModel : BaseBotModel() {
    // TODO: Remove instance IDs when bots are removed (memory leak)
    val localStorage = mutableMapOf<Long, LocalBotStorage>()
    class LocalBotStorage {
        val bitSmooths = mutableMapOf<BitId, Float>()
        val bitSpringOffset = mutableMapOf<BitId, Float>()
        val bitSpringVelocity = mutableMapOf<BitId, Float>()
    }

    // Spring properties -- methods so I can hot reload code to modify them >:)
    fun getSpringStiff() = 0.1f
    fun getSpringDamp() = 0.88f
    fun getSpringImpulse() = 0.5f

    // var reelToReel: ReelToReelBlockEntity? = null
    // var greybox: GreyboxBlockEntity? = null

    var triggeredBadAnimationError = false

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
                animManager.animationControllers.forEach { (_, controller) -> controller.stop() }
            }
        }

        val storage = if (animatable.show.data.playing) {
            localStorage.getOrPut(instanceId) { LocalBotStorage() }
        } else {
            localStorage.remove(instanceId)
            return
        }


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
            val oldSmooth = storage.bitSmooths.putIfAbsent(bit, 0.0f) ?: 0.0f
            val bitSmooth = clamp(
                lerp(oldSmooth, if (bitOn) 1.0f else 0.0f, clamp(flowSpeed * delta, 0.01f, 10.0f)),
                0.0f, 1.0f
            )
            storage.bitSmooths[bit] = bitSmooth

            // Spring
            val (springVel, springOffset) = run {
                val diff = (bitSmooth - oldSmooth) / delta.coerceAtLeast(0.001f)
                var springOffset = storage.bitSpringOffset.getOrDefault(bit, 0f)
                var springVel= storage.bitSpringVelocity.getOrDefault(bit, 0f)
                springVel = (springVel + diff * getSpringImpulse() - springOffset * getSpringStiff()) * getSpringDamp()
                springOffset += springVel * delta
                Pair(springVel, springOffset)
            }
            storage.bitSpringOffset[bit] = springOffset
            storage.bitSpringVelocity[bit] = springVel

            // Manual rotation
            for (rotate in data.rotates) {
                val bone = animationProcessor.getBone(rotate.bone) ?: continue

                // Applying movement
                bone.rotX += (rotate.target.x * Mth.DEG_TO_RAD) * bitSmooth
                bone.rotY += (rotate.target.y * Mth.DEG_TO_RAD) * bitSmooth
                bone.rotZ += (rotate.target.z * Mth.DEG_TO_RAD) * bitSmooth

                // Applying wiggle
                val boneSize = run {
                    var size = 0f
                    fun recurse(bone: GeoBone) {
                        size += (bone.cubes.map { it.size.length() }.average() / bone.cubes.size).toFloat()
                        bone.childBones.forEach { recurse(it) }
                    }
                    recurse(bone)
                    size
                }
                var boneSizeMul = boneSize / 2f
                boneSizeMul = boneSizeMul.coerceIn(0f..1.5f)
                val affect = (springOffset * boneSizeMul).coerceIn(0.5f, 2f).let { if (it.isNaN()) 1f else it }
                bone.rotX += (rotate.target.x * Mth.DEG_TO_RAD) * affect
                bone.rotY += (rotate.target.y * Mth.DEG_TO_RAD) * affect
                bone.rotZ += (rotate.target.z * Mth.DEG_TO_RAD) * affect
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