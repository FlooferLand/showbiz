package com.flooferland.showbiz.addons

import com.flooferland.showbiz.addons.data.AddonManifest

object AddonId {
    private val tooGeneric = setOf("fnaf", "rae", "cec", "cei", "cyber", "cybers")

    fun isTooGeneric(id: String): Boolean {
        return (id in tooGeneric) || id.length < 5
    }

    fun mightBeTooGeneric(id: String): Boolean {
        return id.length < 5
    }

    fun checkValid(packId: String, manifest: AddonManifest, onErr: (msg: String, throwable: Throwable?) -> Unit, onWarn: (msg: String) -> Unit): Boolean {
        var errors = 0
        fun warn(msg: String) = onWarn(msg)
        fun err(msg: String, throwable: Throwable? = null) {
            onErr(msg, throwable)
            errors += 1
        }

        val id = manifest.id.lowercase()
        if (id != manifest.id) {
            err("Manifest ID must be lowercase")
        }
        if (AddonId.isTooGeneric(id)) {
            err("Addon manifest ID '$id' is too generic of an ID and might be used by another addon. Please note the ID is NOT the same as your addon's name and it has to be unique.")
        }

        if (packId !in listOf("showbiz", "showbiz-fnaf") && manifest.id.startsWith("showbiz-")) {
            warn("Please don't use addon IDs starting or ending with `showbiz` as your addon might break in the future. This prefix is reserved for built-in or official addons.")
        }
        if (AddonId.mightBeTooGeneric(id)) {
            warn("Addon manifest ID '$id' might be too generic of an ID and might be used by another addon.")
        }
        return errors == 0
    }
}