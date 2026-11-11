package com.flooferland.showbiz.addons

import com.flooferland.showbiz.addons.data.AddonManifest
import com.flooferland.showbiz.types.ResourceId

/** Data about an addon from the `assets` folder */
data class AddonAssets(
    val bots: Map<ResourceId, AddonBot>,
    val manifest: AddonManifest?
)
