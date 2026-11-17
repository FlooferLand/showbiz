package com.flooferland.showbiz.types.lights

import com.flooferland.showbiz.types.OwnerId
import com.flooferland.showbiz.types.light.*
import com.flooferland.showbiz.utils.ShowbizEnv
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.network.chat.Component
import net.minecraft.world.level.redstone.Redstone
import net.minecraft.world.phys.Vec3

sealed class BaseLightBackend {
    /** Called when the player switches to this backend */
    open fun load() {}

    /** Called when the player switches to another backend */
    open fun unload() {}

    /** Called before [load], and after [unload] */
    open fun reset() {}

    open fun addLight(emitter: ILightEmitter, light: AbstractLight) {}
    open fun removeLight(emitter: ILightEmitter, light: AbstractLight) {}

    open fun preRender(emitter: ILightEmitter, light: AbstractLight) {}
    open fun renderPointlight(emitter: ILightEmitter, light: PointLight) {}
    open fun renderSpotlight(emitter: ILightEmitter, light: SpotLight) {}
    open fun renderArealight(emitter: ILightEmitter, light: AreaLight) {}
    open fun postRender(emitter: ILightEmitter, light: AbstractLight) {}

    val level: ClientLevel? get() = Minecraft.getInstance().level

    protected fun log(text: String) {
        if (ShowbizEnv.isDev())
            Minecraft.getInstance()?.player?.displayClientMessage(Component.literal(text), true)
    }

    protected fun getPos(emitter: ILightEmitter, light: AbstractLight): Vec3? =
        OwnerId.ofLight(emitter)?.grabPos(level ?: return null)?.add(light.localPos)

    protected fun getPower(redstone: Int) =
        if (redstone > Redstone.SIGNAL_NONE) redstone / Redstone.SIGNAL_MAX.toFloat() else 1f
}