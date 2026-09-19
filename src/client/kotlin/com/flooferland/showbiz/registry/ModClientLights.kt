package com.flooferland.showbiz.registry

import net.minecraft.client.multiplayer.*
import net.minecraft.core.*
import net.minecraft.util.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.lighting.*
import net.minecraft.world.level.redstone.*
import net.minecraft.world.phys.shapes.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.blocks.entities.MonitorBlockEntity
import com.flooferland.showbiz.blocks.entities.SpotlightBlockEntity
import com.flooferland.showbiz.types.math.ColorMath.srgbToLinear
import com.flooferland.showbiz.types.math.Kelvin
import com.flooferland.showbiz.utils.lerp
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents
import net.fabricmc.loader.api.FabricLoader
import org.qualet.irl.light.LightMath
import org.qualet.irl.light.LightRegistry
import org.qualet.irl.light.iris.IrisShadersState
import kotlin.math.roundToInt

object ModClientLights {
    const val COOKIE_TEXTURE = "/assets/${Showbiz.MOD_ID}/textures/spotlight_cookie.png"
    val lights = ConcurrentHashMap.newKeySet<BlockEntity>()!!
    val lightBonusOwners = ConcurrentHashMap<BlockEntity, HashSet<Long>>()
    val lightBonusMap = ConcurrentHashMap<Long, Int>()
    var frameBlockLights = ConcurrentHashMap<Long, Int>()

    fun load() {
        ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register { entity, level ->
            if (entity is SpotlightBlockEntity || entity is MonitorBlockEntity) {
                lights.add(entity)
                lightBonusOwners[entity] = hashSetOf()
            }
        }
        ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register { entity, level ->
            if (lights.contains(entity)) {
                lights.remove(entity)
                lightBonusOwners.remove(entity)?.let {
                    for (pos in it) {
                        lightBonusMap.remove(pos)
                        level.lightEngine.checkBlock(BlockPos.of(pos))
                    }
                }
            }
        }

        try {
            val cookiesDir = FabricLoader.getInstance().configDir.resolve("irl-redactor").resolve("cookies")
            Files.createDirectories(cookiesDir)
            Files.copy(ModClientLights::class.java.getResourceAsStream(COOKIE_TEXTURE)!!, cookiesDir.resolve("showbiz_spotlight.png"), StandardCopyOption.REPLACE_EXISTING)
        } catch (error: Exception) {
            Showbiz.log.error("Failed to copy the cookie mask", error)
        }
    }

    fun getCookie(): Int {
        try {
            val cls = Class.forName("org.qualet.irlredactor.light.cookie.CookieArray")
            val resolveMethod = cls.getMethod("resolve", String::class.java)
            return resolveMethod.invoke(null, "showbiz_spotlight.png") as? Int ?: -1
        } catch (error: Exception) {
            Showbiz.log.error("Failed to set the cookie mask", error)
            return -1;
        }
    }

    fun useVanillaLights() = IrisShadersState.shadersDisabled()

    fun emit(delta: Float) {
        for (entity in lights) {
            if (entity.isRemoved) continue
            val level = entity.level as? ClientLevel ?: continue

            frameBlockLights = ConcurrentHashMap<Long, Int>()
            when (entity) {
                is SpotlightBlockEntity -> emitSpotlight(level, entity, delta)
                is MonitorBlockEntity -> emitMonitor(level, entity, delta)
            }

            // Vanilla light
            val prevLights = lightBonusOwners.getOrPut(entity) { hashSetOf() }
            val removed = prevLights - frameBlockLights.keys
            for (pos in removed) {
                lightBonusMap.remove(pos)
                level.lightEngine.checkBlock(BlockPos.of(pos))
            }
            for ((pos, light) in frameBlockLights)
                paintLightLevel(level, pos, light)
            lightBonusOwners[entity] = frameBlockLights.keys.toHashSet()
        }
    }

    // TODO: Add a vanilla light for the monitor
    fun emitMonitor(level: ClientLevel, entity: MonitorBlockEntity, delta: Float) {
        if (entity.video.data.bytes.isEmpty()) return

        val facing = entity.blockState.getValue(FacingEntityBlock.FACING) ?: return
        val forward = facing.step().mul(0.55f)
        val pos = entity.blockPos.center.add(forward.x.toDouble(), forward.y.toDouble(), forward.z.toDouble())
        val id = entity.blockPos.asLong()

        val color = entity.colorAverage.safe()
            .saturate(2.5f)

        // Vanilla block lights look too bad with the TV so it only uses shaders
        if (!useVanillaLights())
            LightRegistry.registerPoint(
                pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat(),
                color.r.toFloat(), color.g.toFloat(), color.b.toFloat(),
                0.02f,
                1.3f,
                false, false,
                0f, 0f,
                0.2f, 0.3f,
                true,
                id
            )
    }

    fun emitSpotlight(level: ClientLevel, entity: SpotlightBlockEntity, delta: Float) {
        val dir = entity.endPos.subtract(entity.startPos).normalize()
        val pos = entity.endPos

        val cookie = getCookie()
        val cone = LightMath.cone(entity.angle, entity.angle * 0.75f)
        val id = entity.blockPos.asLong()

        //val r = (entity.kelvin shr 16 and 0xFF) / 255f
        //val g = (entity.kelvin shr 8 and 0xFF) / 255f
        //val b = (entity.kelvin and 0xFF) / 255f
        val kelvinColor = Kelvin.toColor(entity.kelvin)
        val r = srgbToLinear(FastColor.ARGB32.red(kelvinColor)).coerceIn(0f, 1f)
        val g = srgbToLinear(FastColor.ARGB32.green(kelvinColor)).coerceIn(0f, 1f)
        val b = srgbToLinear(FastColor.ARGB32.blue(kelvinColor)).coerceIn(0f, 1f)
        val power = if (entity.redstoneSignal > Redstone.SIGNAL_NONE) entity.redstoneSignal / Redstone.SIGNAL_MAX.toFloat() else 1f

        val target = (if (entity.isLit) power else 0f) * entity.brightness
        val fadingOut = target < entity.value
        val speed = 0.14f - (if (fadingOut) 0.03f else 0.0f)
        entity.value = lerp(entity.value, target, speed * delta)
        entity.value = entity.value.coerceIn(0f, 1f)

        // Proper spotlights
        if (!useVanillaLights()) LightRegistry.registerSpot(
            pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat(),
            dir.x.toFloat(), dir.y.toFloat(), dir.z.toFloat(),
            r, g, b,
            entity.value,
            15f,
            cone.cosOuter, cone.cosInner,
            false, false,
            0.5f, 0.8f,
            0.5f, 0.0f,
            entity.shadows,
            cookie.toFloat(), 0f, 1f, 0f,
            id
        )

        // Block lights for compatibility
        val blockLight = (entity.value * LightEngine.MAX_LEVEL).roundToInt().coerceAtMost(13)
        if (entity.value <= 0f || !useVanillaLights()) return
        val start = pos.add(dir)
        val range = pos.add(dir.scale(10.0))
        BlockGetter.traverseBlocks<BlockPos?, Unit?>(start, range, Unit, { _, blockPos ->
            frameBlockLights[blockPos.asLong()] = blockLight
            val state = level.getBlockState(blockPos)
            if (!state.getVisualShape(level, blockPos, CollisionContext.empty()).isEmpty) blockPos else null
        }, { null })
    }

    fun paintLightLevel(level: ClientLevel, pos: Long, light: Int) {
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
}