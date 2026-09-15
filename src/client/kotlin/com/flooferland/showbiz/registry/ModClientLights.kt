package com.flooferland.showbiz.registry

import net.minecraft.world.level.block.entity.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.blocks.entities.SpotlightBlockEntity
import com.flooferland.showbiz.types.math.Vec3fc
import com.flooferland.showbiz.utils.lerp
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents
import net.fabricmc.loader.api.FabricLoader
import org.qualet.irl.light.LightMath
import org.qualet.irl.light.LightRegistry

object ModClientLights {
    const val COOKIE_TEXTURE = "/assets/${Showbiz.MOD_ID}/textures/spotlight_cookie.png"
    val lights = hashSetOf<BlockEntity>()

    fun load() {
        ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register { entity, level ->
            if (entity is SpotlightBlockEntity) {
                lights.add(entity)
            }
        }
        ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register { entity, level ->
            if (lights.contains(entity)) {
                lights.remove(entity)
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

    fun emit(delta: Float) {
        for (entity in lights) {
            if (entity !is SpotlightBlockEntity || entity.isRemoved) continue

            val facing = entity.blockState.getValue(FacingEntityBlock.FACING) ?: continue
            val dir = Vec3fc(
                facing.stepX.toFloat(),
                facing.stepY.toFloat() + (entity.turn.y * 0.05f),
                facing.stepZ.toFloat() + (entity.turn.x * -0.01f)
            )
            val pos = entity.blockPos.center.add(dir.x * 0.4, 0.1 + dir.y * 0.25, dir.z * 0.1)

            val cookie = getCookie()
            val cone = LightMath.cone(entity.angle, entity.angle * 0.75f)
            val id = entity.blockPos.asLong()

            val r = (entity.color shr 16 and 0xFF) / 255f
            val g = (entity.color shr 8 and 0xFF) / 255f
            val b = (entity.color and 0xFF) / 255f
            entity.value = lerp(entity.value, if (entity.isOn) 1f else 0f, 0.3f * delta)
            entity.value = entity.value.coerceIn(0f, 1f)

            LightRegistry.registerSpot(
                pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat(),
                dir.x, dir.y, dir.z,
                r, g, b,
                entity.value * 0.8f,
                15f,
                cone.cosOuter, cone.cosInner,
                false, false,
                0.5f, 0.8f,
                0.5f, 0.0f,
                entity.shadows,
                cookie.toFloat(), 0f, 1f, 0f,
                id
            )
        }
    }
}