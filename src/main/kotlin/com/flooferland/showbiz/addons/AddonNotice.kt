package com.flooferland.showbiz.addons

import net.minecraft.network.chat.*
import net.minecraft.resources.*
import com.flooferland.bizlib.bits.BitsMap

data class AddonNotice(val severity: Severity, val text: Component, val title: String? = null) {
    // Bitmap message
    constructor(severity: Severity, id: ResourceLocation, message: BitsMap.LogMessage)
        : this(
            severity,
            text = Component.literal(message.toString()),
            title = id.toString()
        )

    /** Hey, it's me, it's Severity. Ask me anything! */
    enum class Severity { Info, Warning, Error }
}