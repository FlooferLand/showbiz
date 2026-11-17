package com.flooferland.showbiz.types.lights

import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.types.IRLights
import com.flooferland.showbiz.types.OwnerId
import com.flooferland.showbiz.types.light.AreaLight
import com.flooferland.showbiz.types.light.ILightEmitter
import com.flooferland.showbiz.types.light.PointLight
import com.flooferland.showbiz.types.light.SpotLight
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.world.phys.Vec3
import org.qualet.irl.light.LightMath
import org.qualet.irl.light.LightRegistry
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.math.abs

internal object IRLightBackend : BaseLightBackend() {
    private const val COOKIE_TEXTURE = "/assets/${Showbiz.MOD_ID}/textures/spotlight_cookie.png"

    /** For some reason IRLight lights are slightly too far back */
    private val spotOffset get() = -0.25

    override fun load() {
        try {
            val cookiesDir = FabricLoader.getInstance().configDir.resolve("irl-redactor").resolve("cookies")
            val cookieFile = cookiesDir.resolve("showbiz_spotlight.png")
            Files.createDirectories(cookiesDir)
            Files.copy(IRLightBackend::class.java.getResourceAsStream(COOKIE_TEXTURE)!!, cookieFile, StandardCopyOption.REPLACE_EXISTING)
        } catch (error: Exception) {
            Showbiz.log.error("Failed to copy the cookie mask", error)
        }
    }

    override fun unload() {

    }

    override fun renderPointlight(emitter: ILightEmitter, light: PointLight) {
        if (!light.enabled || level == null) return
        val ownerId = OwnerId.ofLight(emitter) ?: return

        val pos = getPos(emitter, light) ?: return

        LightRegistry.registerPoint(
            pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat(),
            light.color.r.toFloat(), light.color.g.toFloat(), light.color.b.toFloat(),
            light.intensity,
            light.radius,
            false, false,
            0f, 0f,
            0.2f, 0.3f,
            light.shadows,
            ownerId.grabId()
        )
    }

    override fun renderSpotlight(emitter: ILightEmitter, light: SpotLight) {
        if (!light.enabled || level == null) return
        val ownerId = OwnerId.ofLight(emitter) ?: return

        val cookie = IRLights.getCookie()
        val angle = light.angle * 2f
        val cone = LightMath.cone(angle, angle * 0.9f)
        val dir = light.dir
        val pos = getPos(emitter, light)?.add(dir.scale(spotOffset)) ?: return
        val (r, g, b) = light.color.toLinear()
        LightRegistry.registerSpot(
            pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat(),
            dir.x.toFloat(), dir.y.toFloat(), dir.z.toFloat(),
            r, g, b,
            light.intensity,
            light.range * 0.8f,
            cone.cosOuter, cone.cosInner,
            false, false,
            0.5f, 0.8f,
            0.5f, 0.5f,
            light.shadows,
            cookie.toFloat(), 0f, 1f, 0f,
            ownerId.grabId()
        )
    }

    /**
     * IRLights doesn't support area lights for now so I'm spawning a spotlight at each corner..
     * This illusion works for the most part, but it is COOKED at larger area sizes
     */
    override fun renderArealight(emitter: ILightEmitter, light: AreaLight) {
        if (!light.enabled || level == null) return
        val ownerId = OwnerId.ofLight(emitter) ?: return

        val dir = light.dir.normalize()
        val pos = getPos(emitter, light)?.add(dir.scale(spotOffset)) ?: return

        // Right/up vectors
        val ref = if (abs(dir.y) > 0.99) Vec3(0.0, 0.0, 1.0) else Vec3(0.0, 1.0, 0.0)
        val right = dir.cross(ref).normalize()
        val up = right.cross(dir).normalize()

        // Adding more spotlights for larger sizes since it looks like a spot otherwise
        var angle = 140f
        val w = light.size.x * 0.3
        val h = light.size.y * 0.3
        val corners = mutableListOf<Vec3>()
        if (light.size.length() > 2f) {
            corners += pos.add(right.scale(w)).add(up.scale(h)) // Top right
            corners += pos.subtract(right.scale(w)).add(up.scale(h)) // Top left
            corners += pos.add(right.scale(w)).subtract(up.scale(h)) // Bottom right
            corners += pos.subtract(right.scale(w)).subtract(up.scale(h)) // Bottom left
        } else {
            angle += 10f
            corners += pos
        }

        // Calculating stuff
        val range = (light.range - (angle / 50f)).coerceAtLeast(1f)
        val intensity = light.intensity / corners.size.toFloat()
        val cone = LightMath.cone(angle, angle * 0.98f)
        val (r, g, b) = light.color.toLinear()

        // Adding spotlights
        val id = ownerId.grabId()
        for ((i, corner) in corners.withIndex()) {
            val cornerId = id + i.toLong()
            LightRegistry.registerSpot(
                corner.x.toFloat(), corner.y.toFloat(), corner.z.toFloat(),
                dir.x.toFloat(), dir.y.toFloat(), dir.z.toFloat(),
                r, g, b,
                intensity,
                range,
                cone.cosOuter, cone.cosInner,
                false, false,
                0.2f, 0.2f,
                0.5f, 0.0f,
                light.shadows,
                -1f, 0f, 1f, 0f,
                cornerId
            )
        }
    }
}