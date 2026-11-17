package com.flooferland.showbiz.types.lights

import com.flooferland.showbiz.types.OwnerId
import com.flooferland.showbiz.types.light.AbstractLight
import com.flooferland.showbiz.types.light.AreaLight
import com.flooferland.showbiz.types.light.PointLight
import com.flooferland.showbiz.types.light.SpotLight
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

    open fun addLight(ownerId: OwnerId, light: AbstractLight) {}
    open fun removeLight(ownerId: OwnerId, light: AbstractLight) {}

    open fun preRender(ownerId: OwnerId, light: AbstractLight) {}
    open fun renderPointlight(ownerId: OwnerId, light: PointLight) {}
    open fun renderSpotlight(ownerId: OwnerId, light: SpotLight) {}
    open fun renderArealight(ownerId: OwnerId, light: AreaLight) {}
    open fun postRender(ownerId: OwnerId, light: AbstractLight) {}

    val level: ClientLevel? get() = Minecraft.getInstance().level

    protected fun log(text: String) {
        if (ShowbizEnv.isDev())
            Minecraft.getInstance()?.player?.displayClientMessage(Component.literal(text), true)
    }

    protected fun getPos(ownerId: OwnerId, light: AbstractLight): Vec3? =
        ownerId.grabPos(level ?: return null)?.add(light.localPos)

    protected fun getPower(redstone: Int) =
        if (redstone > Redstone.SIGNAL_NONE) redstone / Redstone.SIGNAL_MAX.toFloat() else 1f
}