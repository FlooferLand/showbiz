package com.flooferland.showbiz.types.lights

import com.flooferland.showbiz.types.OwnerId
import com.flooferland.showbiz.types.light.*
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.lighting.LightEngine
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import java.util.concurrent.ConcurrentHashMap

// TODO: Figure out a way to prevent flashing lights using vanilla's lighting from looking so bad

object VanillaLightBackend : BaseLightBackend() {
    private val lights = ConcurrentHashMap.newKeySet<OwnerId>()!!
    private val lightBonusOwners = ConcurrentHashMap<OwnerId, HashSet<Long>>()
    private val lightBonusMap = ConcurrentHashMap<Long, Int>()
    private var frameBlockLights = ConcurrentHashMap<Long, Int>()

    private fun loadEmitter(id: OwnerId, level: ClientLevel) {
        lights.add(id)
        lightBonusOwners[id] = hashSetOf()
    }
    private fun unloadEmitter(id: OwnerId, level: ClientLevel) {
        if (lights.contains(id)) {
            lights.remove(id)
            lightBonusOwners.remove(id)?.let {
                for (pos in it) {
                    lightBonusMap.remove(pos)
                    level.lightEngine.checkBlock(BlockPos.of(pos))
                }
            }
        }
    }

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

    init {
        ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register { entity, level ->
            if (entity !is ILightEmitter) return@register
            loadEmitter(OwnerId.of(entity.blockPos), level)
        }
        ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register { entity, level ->
            if (entity !is ILightEmitter) return@register
            unloadEmitter(OwnerId.ofEntity(entity), level)
        }
        ClientEntityEvents.ENTITY_LOAD.register { entity, level ->
            if (entity !is ILightEmitter) return@register
            loadEmitter(OwnerId.ofEntity(entity), level)
        }
        ClientEntityEvents.ENTITY_UNLOAD.register { entity, level ->
            if (entity !is ILightEmitter) return@register
            unloadEmitter(OwnerId.ofEntity(entity), level)
        }
    }

    override fun removeLight(ownerId: OwnerId, light: AbstractLight) {
        cleanupLights(ownerId)
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

    override fun preRender(ownerId: OwnerId, light: AbstractLight) {
        frameBlockLights = ConcurrentHashMap<Long, Int>()
    }

    override fun renderPointlight(ownerId: OwnerId, light: PointLight) {
        val level = level ?: return
        val blockPos = ownerId.grabBlockPos(level) ?: return
        if (!light.enabled) return

        val blockLight = light.getLightLevel()
        frameBlockLights[blockPos.asLong()] = blockLight
    }

    override fun renderSpotlight(ownerId: OwnerId, light: SpotLight) {
        vanillaRenderLightBeam(ownerId, light, light.dir.normalize(), light.range)
    }

    override fun renderArealight(ownerId: OwnerId, light: AreaLight) {
        vanillaRenderLightBeam(ownerId, light, light.dir.normalize(), light.range)
    }

    override fun postRender(ownerId: OwnerId, light: AbstractLight) {
        cleanupLights(ownerId)
    }

    private fun vanillaRenderLightBeam(ownerId: OwnerId, light: AbstractLight, lightDir: Vec3, lightRange: Float) {
        val level = level ?: return
        if (!light.enabled) return
        val blockPos = ownerId.grabBlockPos(level) ?: return

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

    fun cleanupLights(ownerId: OwnerId) {
        val prevLights = lightBonusOwners.getOrPut(ownerId) { hashSetOf() }
        val removed = prevLights - frameBlockLights.keys
        for (pos in removed) {
            lightBonusMap.remove(pos)
            level?.lightEngine?.checkBlock(BlockPos.of(pos))
        }
        for ((pos, light) in frameBlockLights) {
            paintLightLevel(level ?: break, pos, light)
        }
        lightBonusOwners[ownerId] = frameBlockLights.keys.toHashSet()
    }
}