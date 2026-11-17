package com.flooferland.showbiz.types.lights

import com.flooferland.showbiz.types.OwnerId
import com.flooferland.showbiz.types.light.*
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.lighting.LightEngine
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import java.util.concurrent.ConcurrentHashMap

// TODO: Figure out a way to prevent flashing lights using vanilla's lighting from looking so bad

object VanillaLightBackend : BaseLightBackend() {
    private val lights = ConcurrentHashMap.newKeySet<ILightEmitter>()!!
    private val lightBonusOwners = ConcurrentHashMap<ILightEmitter, HashSet<Long>>()
    private val lightBonusMap = ConcurrentHashMap<Long, Int>()
    private var frameBlockLights = ConcurrentHashMap<Long, Int>()

    private fun paintLightLevel(level: ClientLevel, pos: Long, light: Int) {
        val light = light.coerceIn(0, LightEngine.MAX_LEVEL)
        if (light != lightBonusMap[pos]) {
            lightBonusMap[pos] = light
            level.lightEngine.checkBlock(BlockPos.of(pos))
        }
    }

    /** Called from a mixin on a block update */
    fun getLightLevel(pos: Long): Int {
        return lightBonusMap[pos] ?: 0
    }

    override fun addLight(emitter: ILightEmitter, light: AbstractLight) {
        lights.add(emitter)
        lightBonusOwners[emitter] = hashSetOf()
    }

    override fun removeLight(emitter: ILightEmitter, light: AbstractLight) {
        if (lights.contains(emitter)) {
            lights.remove(emitter)
            lightBonusOwners.remove(emitter)?.let {
                for (pos in it) {
                    lightBonusMap.remove(pos)
                    level?.lightEngine?.checkBlock(BlockPos.of(pos))
                }
            }
        }
        cleanupLights(emitter)
    }

    override fun reset() {
        val level = level ?: return
        lightBonusOwners.values.flatten().forEach { pos ->
            lightBonusMap.remove(pos)
            level.lightEngine.checkBlock(BlockPos.of(pos))
        }
        lightBonusOwners.clear()
        lightBonusMap.clear()
        frameBlockLights.clear()
    }

    override fun preRender(emitter: ILightEmitter, light: AbstractLight) {
        frameBlockLights = ConcurrentHashMap<Long, Int>()
    }

    override fun renderPointlight(emitter: ILightEmitter, light: PointLight) {
        val level = level ?: return
        val blockPos = OwnerId.ofLight(emitter)?.grabBlockPos(level) ?: return
        if (!light.enabled || !light.vanillaBackend) return

        val blockLight = light.getLightLevel()
        frameBlockLights[blockPos.asLong()] = blockLight
    }

    override fun renderSpotlight(emitter: ILightEmitter, light: SpotLight) {
        vanillaRenderLightBeam(emitter, light, light.dir.normalize(), light.range)
    }

    override fun renderArealight(emitter: ILightEmitter, light: AreaLight) {
        vanillaRenderLightBeam(emitter, light, light.dir.normalize(), light.range)
    }

    override fun postRender(emitter: ILightEmitter, light: AbstractLight) {
        cleanupLights(emitter)
    }

    private fun vanillaRenderLightBeam(emitter: ILightEmitter, light: AbstractLight, lightDir: Vec3, lightRange: Float) {
        val level = level ?: return
        if (!light.enabled || !light.vanillaBackend) return
        val blockPos = OwnerId.ofLight(emitter)?.grabBlockPos(level) ?: return

        val blockLight = light.getLightLevel().coerceAtMost(13)
        if (blockLight == 0) return

        val start = blockPos.center.add(light.localPos.add(lightDir))
        val range = blockPos.center.add(light.localPos.add(lightDir.scale(lightRange.toDouble())))
        BlockGetter.traverseBlocks<BlockPos?, Unit?>(start, range, Unit, { _, blockPos ->
            val state = level.getBlockState(blockPos)
            val blocked = !state.getVisualShape(level, blockPos, CollisionContext.empty()).isEmpty
            if (!blocked) frameBlockLights[blockPos.asLong()] = blockLight
            if (blocked) blockPos else null
        }, { null })
    }

    fun cleanupLights(emitter: ILightEmitter) {
        val prevLights = lightBonusOwners.getOrPut(emitter) { hashSetOf() }
        val removed = prevLights - frameBlockLights.keys
        for (pos in removed) {
            lightBonusMap.remove(pos)
            level?.lightEngine?.checkBlock(BlockPos.of(pos))
        }
        for ((pos, light) in frameBlockLights) {
            paintLightLevel(level ?: break, pos, light)
        }
        lightBonusOwners[emitter] = frameBlockLights.keys.toHashSet()
    }
}