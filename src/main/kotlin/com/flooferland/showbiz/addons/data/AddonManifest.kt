package com.flooferland.showbiz.addons.data

import kotlinx.serialization.Serializable

@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class Comment(val text: String)

@Serializable
data class AddonManifest(
    val addon: AddonManifestData
)

@Serializable
@Suppress("ArrayInDataClass")
data class AddonManifestData(
    @Comment("The ID of the addon. Make sure this is unique, avoid generic names like \"rae\" or \"fnaf\", or names that start with \"showbiz\"")
    val id: String,

    @Comment("The display name of the addon")
    val name: String,

    @Comment("The developers of the addon")
    val authors: Array<String>,

    @Comment("The description of the addon, ex: \"Adds the Rock-afire Explosion\"")
    val description: String,

    @Comment("The version of your addon. Increase it any time you make a change (see https://semver.org)")
    val version: String,

    @Comment("""
        The format version. The number will increment every time there's a breaking change to the addon manifest format and/or addon file structure.
        Essentially, this dictates whenever your addon will keep working in newer releases or not.
    """)
    val format: Short
)
