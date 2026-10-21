package com.flooferland.showbiz.addons

import com.flooferland.showbiz.types.ResourceId

data class AddonAssets(
    val id: String,
    val bots: Map<ResourceId, AddonBot>
)
