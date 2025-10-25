package com.flooferland.showbiz.addons.assets

import BitMapping
import com.flooferland.showbiz.show.BitId
import com.flooferland.showbiz.utils.ResourcePath
import com.flooferland.showbiz.utils.rlCustom
import kotlinx.serialization.Serializable
import net.minecraft.resources.*

data class AddonBot(val assets: BotAssetsFile, val bitmap: BotBitmapFile, val resPath: ResourcePath, val model: ResourceLocation) {
    fun resPath(path: String) = rlCustom(this.resPath.name, path)

    fun getNamespace() = this.resPath.namespace
    fun getId() = this.resPath.name
    fun getTexture(name: String) = rlCustom(getNamespace(), resPath.resolve("textures").resolve(name).path)
    fun getModel(name: String) = rlCustom(getNamespace(), resPath.resolve("models").resolve(name).path)
    fun getDefaultModel() = getModel("${getId()}.geo.json")
    fun getDefaultTexture() = getTexture("${getId()}.png")
}

@Serializable
data class BotBitmapFile(
    val settings: Map<String, Boolean>,

    @Serializable(with = BitmapSerializer::class)
    val bits: Map<BitId, BitMapping>
)

@Serializable
data class BotAssetsFile(val variants: Map<String, BotVariant>) {
    fun getDefault(): BotVariant = variants["default"]!!
}

@Serializable
data class BotVariant(
    val texture: String? = null,
    val model: String? = null
)
