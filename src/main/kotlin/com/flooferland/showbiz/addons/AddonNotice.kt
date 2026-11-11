package com.flooferland.showbiz.addons

import com.flooferland.bizlib.bits.BitsMap
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

data class AddonNotice(val severity: Severity, val side: Side, val text: Component, val title: String? = null, val throwable: Throwable? = null) {
    // Bitmap message
    constructor(severity: Severity, side: Side, id: ResourceLocation, message: BitsMap.LogMessage)
        : this(
            severity,
            side,
            text = Component.literal(message.toString()),
            title = id.toString()
        )

    enum class Side { Assets, Data }

    /** Hey, it's me, it's Severity. Ask me anything! */
    enum class Severity { Info, Warning, Error }
}