package com.flooferland.showbiz.addons.assets

import com.flooferland.showbiz.types.ResourceId

data class AddonAssets(
    val id: String,
    val bots: Map<ResourceId, AddonBot>
)
