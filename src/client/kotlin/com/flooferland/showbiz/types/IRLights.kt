package com.flooferland.showbiz.types

import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.utils.ShowbizEnv

/** Safe class for communicating with IRLights to avoid NoClassDefFoundError */
object IRLights {
    fun shadersDisabled(): Boolean {
        if (!ShowbizEnv.hasIRLights()) return true
        return runCatching { org.qualet.irl.light.iris.IrisShadersState.shadersDisabled() }.getOrNull() ?: true
    }

    fun getCookie(): Int {
        if (!ShowbizEnv.hasIRLights()) return -1;
        try {
            val cls = Class.forName("org.qualet.irlredactor.light.cookie.CookieArray")
            val resolveMethod = cls.getMethod("resolve", String::class.java)
            return resolveMethod.invoke(null, "showbiz_spotlight.png") as? Int ?: -1
        } catch (error: Exception) {
            Showbiz.log.error("Failed to set the cookie mask", error)
            return -1;
        }
    }

    fun register() {

    }
}