package com.flooferland.showbiz.addons.data

data class AddonData(
    val manifest: AddonManifest,
    val bots: HashMap<String, AddonBotEntry>
)