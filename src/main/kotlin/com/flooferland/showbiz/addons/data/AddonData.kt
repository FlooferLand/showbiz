package com.flooferland.showbiz.addons.data

import com.flooferland.showbiz.types.ResourceId

data class AddonData(
    val manifest: AddonManifest,
    val bots: Map<ResourceId, AddonBotEntry>
)