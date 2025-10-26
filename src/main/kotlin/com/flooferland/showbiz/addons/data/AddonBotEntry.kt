package com.flooferland.showbiz.addons.data

import kotlinx.serialization.Serializable

@Serializable
data class AddonBotEntry(
    val name: String,
    val authors: List<String>,
    val formats: List<String>
)
