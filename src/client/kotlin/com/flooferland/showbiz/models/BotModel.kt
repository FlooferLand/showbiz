package com.flooferland.showbiz.models

import net.minecraft.client.multiplayer.*
import net.minecraft.core.registries.*
import net.minecraft.resources.*
import net.minecraft.sounds.*
import net.minecraft.util.*
import net.minecraft.world.entity.*
import net.minecraft.world.level.block.entity.*
import com.flooferland.bizlib.bits.AnimCommand
import com.flooferland.bizlib.bits.BitMappingData
import com.flooferland.bizlib.bits.BitUtils
import com.flooferland.bizlib.bits.Movements
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.ShowbizClient
import com.flooferland.showbiz.addons.assets.AddonBot
import com.flooferland.showbiz.addons.data.BotModelData
import com.flooferland.showbiz.addons.data.ChainLayout
import com.flooferland.showbiz.models.CymbalModel.Companion.updateAnimation
import com.flooferland.showbiz.models.CymbalModel.Companion.updateState
import com.flooferland.showbiz.models.CymbalModel.CymbalState
import com.flooferland.showbiz.show.BitId
import com.flooferland.showbiz.show.SignalFrame
import com.flooferland.showbiz.types.*
import com.flooferland.showbiz.types.collidepart.CollidePartId
import com.flooferland.showbiz.types.collidepart.ICollidePartInteractable
import com.flooferland.showbiz.types.math.Vec3fc
import com.flooferland.showbiz.types.physics.ChainRig
import com.flooferland.showbiz.types.physics.ChainSolver
import com.flooferland.showbiz.types.physics.DynBone
import com.mojang.blaze3d.Blaze3D
import java.util.WeakHashMap
import software.bernie.geckolib.animatable.GeoAnimatable
import software.bernie.geckolib.animatable.stateless.StatelessAnimationController
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.animation.RawAnimation
import software.bernie.geckolib.animation.keyframe.event.SoundKeyframeEvent
import software.bernie.geckolib.cache.`object`.GeoBone
import software.bernie.geckolib.constant.DataTickets
import software.bernie.geckolib.util.ClientUtil

/** Responsible for fancy animation */
class BotModel<T> : BaseBotModel<T>() where T : IBot, T: GeoAnimatable {
    val localStorage = WeakHashMap<T, LocalBotStorage>()
    class LocalBotStorage {
        val valves = mutableMapOf<BitId, PneumaticValve>()
        var cylinderClock: Double = 0.0
        val cymbalStates = mutableMapOf<String, CymbalState>()
        var ownerBotId: ResourceId? = null
        var generation: Int = -1
        var lastFrameTime = -1.0
        var chains: List<ChainSolver> = emptyList()
        var chainsFor: ChainLayout? = null
        var chainClock: Double = -1.0
        var dynBones: List<DynBone> = emptyList()
        var dynBonesFor: Map<String, DynBone.Params>? = null
    }

    var triggeredBadAnimationError = false

    override fun setCustomAnimations(animatable: T, instanceId: Long, state: AnimationState<T>) {
        val bot = ShowbizClient.bots[animatable.botId] ?: run { resetAll(); return }
        val model = currentModel ?: run { resetAll(); return }

        // Getting the bits via the mapping
        // TODO: Make bots work when they receive any mapping, even an empty one
        val showMapping = animatable.show?.data?.mapping
        val showPlaying = animatable.show?.data?.playing == true
        val bitmapBits = bot.bitmap.bits[showMapping] ?: mutableMapOf()
        if (bitmapBits.isEmpty()) resetAll()

        // Resetting bones
        val instanceCache = animatable.getAnimatableInstanceCache()
        val animManager = instanceCache.getManagerForId<GeoAnimatable>(0)
        for ((_, data) in bitmapBits) {
            if (!showPlaying) {
                for (anim in data.anim) {
                    animManager.stopTriggeredAnimation(getAnimId(animatable, true, anim))
                    animManager.stopTriggeredAnimation(getAnimId(animatable, false, anim))
                }
                animManager.animationControllers.forEach { (_, controller) -> controller.stop() }
            }
        }

        // Resetting animations
        if (!showPlaying) {
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
        val movements = showMapping?.let { BitUtils.readBitmap(it) }?.get(bot.getId())

        // Driving animation
        val delta = run {
            // Doing the hardest thing in Minecraft modding.. Getting the delta time..
            // Making the Create mod is easy. Making Aeronautics is easy.. Compared to getting the delta time *thunder sound effect*
            val currentFrameTime = when (animatable) {
                is Entity -> animatable.tickCount + state.partialTick
                is BlockEntity -> (state.getData(DataTickets.TICK) ?: 0.0)
                else -> ShowbizClient.getDeltaTime()
            }.toDouble()
            val lastFrameTime = if (storage.lastFrameTime > 0f) storage.lastFrameTime else currentFrameTime
            storage.lastFrameTime = currentFrameTime
            val deltaTicks = (currentFrameTime - lastFrameTime).coerceIn(0.0..1.25)
            deltaTicks.toFloat()
        }
        driveMotion(bitmapBits, animatable, animManager, storage, delta, bot, model, movements, showMapping)
        driveCollideParts(animatable, model, storage, state.partialTick)
        driveChains(animatable, model, storage, bot)
    }

    private fun driveChains(animatable: T, model: BotModelData, storage: LocalBotStorage, bot: AddonBot) {
        val layout = model.chainLayout
        if (layout.size == 0) return
        val tables = bot.physics?.chains ?: return
        val poses = GeoWorkaroundRenderHook.lastPoses[animatable] ?: return
        if (storage.chainsFor !== layout) {
            storage.chains = ChainSolver.forTables(layout, tables)
            storage.chainsFor = layout
        }
        val now = Blaze3D.getTime()
        val last = storage.chainClock
        storage.chainClock = now
        if (last < 0.0) return
        val delta = if (ShowbizClient.getDeltaTime() <= 0.001f) 0.001f else ((now - last) * 20.0).toFloat().coerceIn(0.001f, 1.25f)

        for (solver in storage.chains) {
            val members = solver.members
            for (i in members.indices) solver.roots[i] = poses[layout.roots[members[i]]]
            solver.frame(delta, { i -> ChainRig.length(layout.links[members[i]], poses) }, { i, k -> poses[layout.links[members[i]][k]] })

            // Writing the bones
            for (i in members.indices) {
                val chain = solver.chains[i]
                if (!chain.posed) continue
                val links = layout.links[members[i]]
                for (k in 0 until chain.links) {
                    if (!chain.eulerOk[k]) continue
                    val bone = animationProcessor.getBone(links[k]) ?: continue
                    val rest = model.initBoneRots[links[k]] ?: continue
                    bone.rotX = rest.x + chain.euler[k * 3]
                    bone.rotY = rest.y + chain.euler[k * 3 + 1]
                    bone.rotZ = rest.z + chain.euler[k * 3 + 2]
                    bone.markRotationAsChanged()
                }
            }
        }
    }
    private fun driveCollideParts(animatable: T, model: BotModelData, storage: LocalBotStorage, partialTick: Float) {
        if (animatable !is ICollidePartInteractable) return

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
    private fun driveMotion(bitmapBits: MutableMap<UShort, BitMappingData>, animatable: T,
                            animManager: AnimatableManager<GeoAnimatable>?, storage: LocalBotStorage, delta: Float, bot: AddonBot,
                            model: BotModelData, movements: Movements?, showMapping: String?) {
        val cylinders = showMapping?.let { bot.physics?.bits?.get(it) }
        val supply = showMapping?.let { bot.physics?.cylinders?.get(it) } ?: cylinders?.let { PneumaticValve.Supply.DEFAULT }
        val hits = showMapping?.let { bot.physics?.hits?.get(it) }
        val dynBones = bot.physics?.dynbones
        if (dynBones != null && storage.dynBonesFor !== dynBones) {
            storage.dynBones = dynBones.map { (root, params) -> DynBone(root, params) }
            storage.dynBonesFor = dynBones
        }
        val frame = animatable.show?.data?.signal ?: SignalFrame()
        var steps = 0
        if (supply != null) {
            storage.cylinderClock += delta * 0.05   // ticks to seconds
            while (storage.cylinderClock >= 1.0 / supply.rate) {
                storage.cylinderClock -= 1.0 / supply.rate
                steps++
            }
        }

        val alpha = if (supply != null) (storage.cylinderClock * supply.rate).toFloat() else 1f

        // Every cylinder a step at a time, then the floppy parts off the pose that step puts the bones in
        if (supply != null && cylinders != null) repeat(steps) {
            for (bit in bitmapBits.keys) {
                val cylinder = cylinders[bit.toString()] ?: continue
                val valve = storage.valves.getOrPut(bit) { PneumaticValve() }
                val dualPressure = cylinder.dualPressureBit != 0 && frame.frameHas(cylinder.dualPressureBit)
                valve.step(frame.frameHas(bit), 60f / supply.rate, supply.psi, dualPressure, cylinder)
                if (hits != null && valve.hit > 0f) playHit(animatable, hits, bit, valve.hit, valve.pos >= 1f)
            }
            if (storage.dynBones.isNotEmpty()) {
                poseBits(bitmapBits, storage, cylinders, model) { it.pos }
                for (dynBone in storage.dynBones) dynBone.step(animationProcessor::getBone, 1f / supply.rate)
            }
        }

        for ((bit, data) in bitmapBits) {
            val bitOn = frame.frameHas(bit)

            // Animation
            for (anim in data.anim) {
                val controllerKey = "ctrl_${bit}_${anim.id}"

                // Adding controllers
                animManager?.let {
                    if (!animManager.animationControllers.contains(controllerKey)) {
                        val controller = StatelessAnimationController(animatable, controllerKey)
                        controller.transitionLength(4)
                        controller.setSoundKeyframeHandler { state -> soundKeyframeHandler(animatable, state) }
                        animManager.addController(controller)
                    }
                }

                // Driving controllers
                val controller = animManager?.animationControllers[controllerKey] as? StatelessAnimationController
                val animId = getAnimId(animatable, bitOn, anim)
                if (controller != null && controller.currentAnimation?.animation?.name != animId) {
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
        }
        poseBits(bitmapBits, storage, cylinders, model) { it.drawn(alpha) }
        for (dynBone in storage.dynBones) dynBone.draw(animationProcessor::getBone, alpha)
    }

    // Laying the bitmap's rotates and moves over the rest pose, each valve at [at] of its travel, floppy parts back at rest
    private fun poseBits(bitmapBits: MutableMap<UShort, BitMappingData>, storage: LocalBotStorage, cylinders: Map<String, PneumaticValve.Params>?, model: BotModelData, at: (PneumaticValve) -> Float) {
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
        }
        for (dynBone in storage.dynBones) for (name in dynBone.boneNames) {
            val bone = animationProcessor.getBone(name) ?: continue
            val initRot = model.initBoneRots[bone.name] ?: Vec3fc()
            bone.rotX = initRot.x; bone.rotY = initRot.y; bone.rotZ = initRot.z
        }
        for ((bit, data) in bitmapBits) {
            val bitSmooth = at(storage.valves.getOrPut(bit) { PneumaticValve() })

            // Manual rotation
            for (rotate in data.rotates) {
                val bone = animationProcessor.getBone(rotate.bone) ?: continue

                // Applying movement
                bone.rotX += (rotate.target.x * Mth.DEG_TO_RAD) * bitSmooth
                bone.rotY += (rotate.target.y * Mth.DEG_TO_RAD) * bitSmooth
                bone.rotZ += (rotate.target.z * Mth.DEG_TO_RAD) * bitSmooth
            }

            // Manual position
            for (move in data.moves) {
                val bone = animationProcessor.getBone(move.bone) ?: continue

                // Applying movement
                bone.posX += move.target.x * bitSmooth
                bone.posY += move.target.y * bitSmooth
                bone.posZ += move.target.z * bitSmooth
            }
        }
    }

    // Playing a cylinder's thunk at an end of its stroke, as its [PneumaticValve.Hit] says
    private fun playHit(animatable: T, hits: PneumaticValve.Hit, bit: UShort, speed: Float, out: Boolean) {
        if (!Showbiz.config.audio.playPneumaticSounds) return
        val own = hits.bits[bit.toString()]
        if (speed < (own?.minSpeed ?: hits.minSpeed)) return
        val volume = (own?.volume ?: hits.volume) * (speed / (own?.fullSpeed ?: hits.fullSpeed)).coerceAtMost(1f)
        if (volume <= 0f) return
        val sound = if (out) own?.soundOut ?: hits.soundOut else own?.soundIn ?: hits.soundIn
        val pos = animatable.botPos ?: return
        val level = animatable.botLevel as? ClientLevel ?: return
        val pitch = (own?.pitch ?: hits.pitch) + (level.random.nextFloat() * 2f - 1f) * (own?.pitchSpread ?: hits.pitchSpread)
        level.playLocalSound(pos.x, pos.y, pos.z, SoundEvent.createVariableRangeEvent(ResourceLocation.parse(sound)), SoundSource.BLOCKS, volume, pitch, false)
    }

    fun soundKeyframeHandler(animatable: T, state: SoundKeyframeEvent<GeoAnimatable>) {
        val level = animatable.botLevel as? ClientLevel ?: return
        if (!Showbiz.config.audio.playBotEffects) return
        if (animatable.botOwnerId?.isRemoved(level) ?: true) return
        val sound = ResourceLocation.parse(state.keyframeData.sound)
        if (!BuiltInRegistries.SOUND_EVENT.containsKey(sound)) {
            Showbiz.log.warn("Sound event '$sound' was not found for bot '${animatable.botId}'")
            return
        }
        val pos = animatable.botPos ?: return
        val soundEvent = SoundEvent.createVariableRangeEvent(sound)
        level.playSound(ClientUtil.getClientPlayer(), pos.x, pos.y, pos.z, soundEvent, SoundSource.BLOCKS, 0.5f, 1.0f)
    }

    fun getAnimId(animatable: T, bitOn: Boolean, anim: AnimCommand): String {
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
            if (bone.cubes.isEmpty()) return@recurse
            size += (bone.cubes.map { it.size.length() }.average() / bone.cubes.size).toFloat()
            bone.childBones.forEach { recurse(it) }
        }
        recurse(bone)
        return size
    }
}