package com.flooferland.showbiz.addons.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddonBotEntry(
    val name: String,
    val authors: List<String>,
    val accepts: AcceptedFormats,

    @SerialName("allow_missing")
    val allowMissing: Boolean = false
)

// TODO: Enforce accepted formats so people can't load fshw files for RAE bots
@Serializable
data class AcceptedFormats(
    val rockafire: Boolean = false,
    val fnaf1: Boolean = false,
    val other: Boolean = false
)
