package com.flooferland.showbiz.addons

import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.addons.data.AddonManifest

object AddonId {
    private val tooGeneric = setOf("fnaf", "rae", "cec", "cei", "cyber", "cybers")

    fun isInternal(id: String): Boolean {
        return id.startsWith(Showbiz.MOD_ID)
    }

    fun isTooGeneric(id: String): Boolean {
        return (id in tooGeneric) || id.length < 5
    }

    fun mightBeTooGeneric(id: String): Boolean {
        return id.length < 5
    }

    fun checkValid(packId: String, manifest: AddonManifest, onErr: (msg: String, throwable: Throwable?) -> Unit, onWarn: (msg: String) -> Unit): Boolean {
        fun err(msg: String, throwable: Throwable? = null) = onErr(msg, throwable)
        fun warn(msg: String) = onWarn(msg)

        val id = manifest.id.lowercase()
        if (id != manifest.id) {
            err("Manifest ID must be lowercase")
            return false
        }
        if (id != packId) {
            err("Addon manifest ID '$id' does not match it's pack ID ('$packId')")
            return false
        }
        if (AddonId.isInternal(id)) {
            err("Addon manifest ID '$id' might be internally used by Showbiz Mod later, so it cannot be used.")
            return false
        }
        if (AddonId.isTooGeneric(id)) {
            err("Addon manifest ID '$id' is too generic of an ID and might be used by another addon. Please note the ID is NOT the same as your addon's name and it has to be unique.")
            return false
        }
        if (AddonId.mightBeTooGeneric(id)) {
            warn("Addon manifest ID '$id' might be too generic of an ID and might be used by another addon.")
            return false
        }
        return true
    }
}