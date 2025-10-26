package com.flooferland.showbiz.addons.assets

data class AddonAssets(
    val id: String,
    val bots: Map<String, AddonBot>
)
