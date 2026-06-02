package com.flooferland.showbiz.models

import net.minecraft.client.*
import net.minecraft.client.multiplayer.*
import net.minecraft.core.registries.*
import net.minecraft.resources.*
import net.minecraft.sounds.*
import net.minecraft.util.*
import com.flooferland.bizlib.bits.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.ShowbizClient
import com.flooferland.showbiz.addons.assets.AddonBot
import com.flooferland.showbiz.addons.data.BotModelData
import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity
import com.flooferland.showbiz.models.CymbalModel.Companion.updateAnimation
import com.flooferland.showbiz.models.CymbalModel.Companion.updateState
import com.flooferland.showbiz.models.CymbalModel.CymbalState
import com.flooferland.showbiz.show.BitId
import com.flooferland.showbiz.types.ClientCollidePartInstance
import com.flooferland.showbiz.types.collidepart.CollidePartId
import com.flooferland.showbiz.types.math.Vec3fc
import com.flooferland.showbiz.utils.lerp
import java.lang.Math.clamp
import java.util.WeakHashMap
import software.bernie.geckolib.animatable.GeoAnimatable
import software.bernie.geckolib.animatable.stateless.StatelessAnimationController
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.animation.RawAnimation
import software.bernie.geckolib.animation.keyframe.event.SoundKeyframeEvent
import software.bernie.geckolib.cache.`object`.GeoBone
import software.bernie.geckolib.util.ClientUtil
import kotlin.math.PI
import kotlin.math.sin

/** Responsible for fancy animation */
class StagedBotBlockEntityModel : BaseBotModel() {
    val localStorage = WeakHashMap<StagedBotBlockEntity, LocalBotStorage>()
    class LocalBotStorage {
        val bitSmooths = mutableMapOf<BitId, Float>()
        val bitSpringOffset = mutableMapOf<BitId, Float>()
        val bitSpringVelocity = mutableMapOf<BitId, Float>()
        val cymbalStates = mutableMapOf<String, CymbalState>()
    }

    // Spring properties -- methods so I can hot reload code to modify them >:)
    fun getSpringStiff() = 0.6f
    fun getSpringDamp() = 0.2f
    fun getSpringImpulse() = 0.15f
    fun getSpringScale(data: BitMappingData) = 1.4f * data.wiggleMul.toFloat()

    var triggeredBadAnimationError = false

    override fun setCustomAnimations(animatable: StagedBotBlockEntity, instanceId: Long, state: AnimationState<StagedBotBlockEntity>) {
        val bot = ShowbizClient.bots[animatable.botId] ?: run { resetAll(); return }
        val model = currentModel ?: run { resetAll(); return }

        // Getting the bits via the mapping
        // TODO: Make bots work when they receive any mapping, even an empty one
        val mapping = animatable.show.data.mapping
        val bitmapBits = bot.bitmap.bits[mapping] ?: mutableMapOf()
        if (bitmapBits.isEmpty()) resetAll()

        // Resetting bones
        val instanceCache = animatable.getAnimatableInstanceCache()
        val animManager = instanceCache.getManagerForId<GeoAnimatable>(0)
        for ((_, data) in bitmapBits) {
            for (rotate in data.rotates) {
                val bone = animationProcessor.getBone(rotate.bone) ?: continue
                val initRot = model.initBoneRots[bone.name] ?: Vec3fc()
                bone.rotX = initRot.x; bone.rotY = initRot.y; bone.rotZ = initRot.z
            }
            for (move in data.moves) {
                val bone = animationProcessor.getBone(move.bone) ?: continue
                val initMove = model.initBoneMoves[bone.name] ?: Vec3fc()
                bone.posX = initMove.x; bone.posY = initMove.y; bone.posZ = initMove.z
            }
            if (!animatable.show.data.playing) {
                for (anim in data.anim) {
                    animManager.stopTriggeredAnimation(getAnimId(animatable, true, anim))
                    animManager.stopTriggeredAnimation(getAnimId(animatable, false, anim))
                }
                animManager.animationControllers.forEach { (_, controller) -> controller.stop() }
            }
        }

        // Resetting animations
        if (!animatable.show.data.playing) {
            for ((bit, data) in bitmapBits) {
                for (anim in data.anim) {
                    animManager.stopTriggeredAnimation(getAnimId(animatable, true, anim))
                    animManager.stopTriggeredAnimation(getAnimId(animatable, false, anim))
                }
            }
            animManager.animationControllers.forEach { (_, controller) -> controller.stop() }
        }

        // Getting bot storage & show playing guard
        val storage = localStorage.getOrPut(animatable) { LocalBotStorage() }

        // May be null since bot.getId doesn't always mean the movement (ex: 'rolfe' on the bitmap doesn't match the bot id 'rolfe_dewolfe')
        val movements = mapping?.let { BitUtils.readBitmap(it) }?.get(bot.getId())

        // Driving animation
        val delta = Minecraft.getInstance().timer.gameTimeDeltaTicks.coerceAtMost(1.25f)
        driveMotion(bitmapBits, animatable, animManager, storage, delta, bot, movements)
        driveCollideParts(animatable, model, storage, state.partialTick)
    }

    private fun driveCollideParts(animatable: StagedBotBlockEntity, model: BotModelData, storage: LocalBotStorage, partialTick: Float) {
        val instance = animatable.collidePartInstance.clientInstance as? ClientCollidePartInstance ?: return
        animationProcessor.getBone("Cymbal")?.let { bone ->
            val state = storage.cymbalStates.getOrPut(bone.name) { CymbalState() }
            val entity = instance.spawned.values.firstOrNull { it.partId == CollidePartId.Cymbal } ?: return@let
            if (entity.lastHitTime != state.lastHitTime) updateState(animatable, entity, state)
            updateAnimation(bone, entity, state, partialTick)
        }
        animationProcessor.getBone("Snare")?.let { bone ->
            val entity = instance.spawned.values.firstOrNull { it.partId == CollidePartId.Snare } ?: return@let
            bone.posY = if (entity.used.isNotEmpty()) -0.1f else 0f
        }
    }

    // TODO: Make this function not a mess
    private fun driveMotion(bitmapBits: MutableMap<UShort, BitMappingData>, animatable: StagedBotBlockEntity, animManager: AnimatableManager<GeoAnimatable>?, storage: LocalBotStorage, delta: Float, bot: AddonBot, movements: Movements?) {
        for ((bit, data) in bitmapBits) {
            // Getting things
            val frame = animatable.show.data.signal
            val flowSpeed = (data.flow.speed.toFloat() * 0.3f)
            val flowEase = data.flow.easing
            val bitOn = frame.frameHas(bit)

            // Animation
            for (anim in data.anim) {
                val controllerKey = "ctrl_${bit}_${anim.id}"

                // Adding controllers
                animManager?.let {
                    if (!animManager.animationControllers.contains(controllerKey)) {
                        val controller = StatelessAnimationController(animatable, controllerKey)
                        controller.transitionLength(1)
                        controller.setSoundKeyframeHandler { state -> soundKeyframeHandler(animatable, state) }
                        animManager.addController(controller)
                    }
                }

                // Driving controllers
                val controller = animManager?.animationControllers[controllerKey] as? StatelessAnimationController
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
                lerp(oldSmooth, if (bitOn) 1.0f else 0.0f, clamp(flowSpeed * delta, 0.0f, 10.0f)),
                0.0f, 1.0f
            )
            storage.bitSmooths[bit] = bitSmooth.let { if (it.isNaN()) 0f else it }

            // Spring
            val diff = (bitSmooth - oldSmooth)
            var springOffset = storage.bitSpringOffset.getOrDefault(bit, 0f)
            var springVel = storage.bitSpringVelocity.getOrDefault(bit, 0f)
            val acceleration = (-getSpringStiff() * springOffset) - (getSpringDamp() * springVel)
            springVel += acceleration * delta
            springVel += diff * getSpringImpulse()
            springOffset += springVel * delta
            storage.bitSpringOffset[bit] = springOffset.let { if (it.isNaN()) 0f else it }
            storage.bitSpringVelocity[bit] = springVel.let { if (it.isNaN()) 0f else it }

            // Easing: https://easings.net/#easeOutSine
            val eased = when (flowEase) {
                Easing.Default, Easing.Linear -> bitSmooth
                Easing.EaseIn -> sin((bitSmooth * PI) / 2).toFloat()
            }

            // Manual rotation
            for (rotate in data.rotates) {
                val bone = animationProcessor.getBone(rotate.bone) ?: continue

                // Applying movement
                bone.rotX += (rotate.target.x * Mth.DEG_TO_RAD) * eased
                bone.rotY += (rotate.target.y * Mth.DEG_TO_RAD) * eased
                bone.rotZ += (rotate.target.z * Mth.DEG_TO_RAD) * eased

                // Applying wiggle
                val affect = (springOffset * getSpringScale(data))
                    .coerceIn(-2f, 2f)
                    .let { if (it.isNaN()) 0f else it }
                bone.rotX += (rotate.target.x * Mth.DEG_TO_RAD) * affect
                bone.rotY += (rotate.target.y * Mth.DEG_TO_RAD) * affect
                bone.rotZ += (rotate.target.z * Mth.DEG_TO_RAD) * affect
            }

            // Manual position
            for (move in data.moves) {
                val bone = animationProcessor.getBone(move.bone) ?: continue

                // Applying movement
                bone.posX += move.target.x * eased
                bone.posY += move.target.y * eased
                bone.posZ += move.target.z * eased

                // Manual overrides
                when (bot.getId()) {
                    // Looney wiggle
                    "looney_bird" if movements?.get("raise") == bit -> {
                        val affect = (springVel * getSpringScale(data)).coerceIn(-2f, 2f) * 2.0f
                        val time = System.currentTimeMillis() * 0.01f
                        val bone = animationProcessor.getBone("head") ?: bone
                        bone.rotX += (((move.target.y * 3f) * (sin(time + 2.123f) * 2f)) * Mth.DEG_TO_RAD) * affect
                        bone.rotY += (((move.target.y * 2f) * (sin(time + 9.124f) * 2f)) * Mth.DEG_TO_RAD) * affect
                        bone.rotZ += (((move.target.y * 1.2f) * (sin(time) * 2f)) * Mth.DEG_TO_RAD) * affect
                    }
                }

                // TODO: Add move wiggle
            }
        }
    }

    fun soundKeyframeHandler(animatable: StagedBotBlockEntity, state: SoundKeyframeEvent<GeoAnimatable>) {
        if (!Showbiz.config.audio.playBotEffects) return
        if (animatable.isRemoved) return
        val sound = ResourceLocation.parse(state.keyframeData.sound)
        if (!BuiltInRegistries.SOUND_EVENT.containsKey(sound)) {
            Showbiz.log.warn("Sound event '$sound' was not found for bot '${animatable.botId}'")
            return
        }
        val soundEvent = SoundEvent.createVariableRangeEvent(sound)
        val level = animatable.level as? ClientLevel ?: return
        level.playSound(ClientUtil.getClientPlayer(), animatable.blockPos, soundEvent, SoundSource.BLOCKS, 0.5f, 1.0f)
    }

    fun getAnimId(animatable: StagedBotBlockEntity, bitOn: Boolean, anim: AnimCommand): String {
        val id = if (bitOn)
            anim.on ?: "${anim.id}.on"
        else
            anim.off ?: "${anim.id}.off"
        return "animation.${animatable.botId?.path}.${id}"
    }

    fun resetAll() {
        val model = currentModel ?: return
        for ((name, initMove) in model.initBoneMoves) {
            val bone = animationProcessor.getBone(name) ?: continue
            bone.posX = initMove.x; bone.posY = initMove.y; bone.posZ = initMove.z
        }
        for ((name, initRot) in model.initBoneRots) {
            val bone = animationProcessor.getBone(name) ?: continue
            bone.rotX = initRot.x; bone.rotY = initRot.y; bone.rotZ = initRot.z
        }
    }

    /** Gets how large a bone is based off cubes */
    fun getBoneSize(bone: GeoBone): Float {
        var size = 0f
        fun recurse(bone: GeoBone) {
            size += (bone.cubes.map { it.size.length() }.average() / bone.cubes.size).toFloat()
            bone.childBones.forEach { recurse(it) }
        }
        recurse(bone)
        return size
    }
}