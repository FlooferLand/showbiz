package com.flooferland.showbiz.addons.assets

import com.flooferland.bizlib.bits.BotBitmapFile
import com.flooferland.showbiz.types.ResourcePath
import com.flooferland.showbiz.utils.rlCustom
import kotlinx.serialization.Serializable
import net.minecraft.resources.*

data class AddonBot(val assets: BotAssetsFile, val bitmap: BotBitmapFile, val resPath: ResourcePath, val model: ResourceLocation, val animations: ResourceLocation?) {
    fun resPath(path: String) = rlCustom(getNamespace(), path)

    fun getNamespace() = this.resPath.namespace
    fun getId() = this.resPath.name
    fun getTexture(name: String) = resPath.resolve("textures").resolve(name).toLocation()
    fun getModel(name: String) = resPath.resolve("models").resolve(name).toLocation()
    fun getDefaultModel() = getModel("${getId()}.geo.json")
    fun getDefaultTexture() = getTexture("${getId()}.png")
}

@Serializable
data class BotAssetsFile(val variants: Map<String, BotVariant>) {
    fun getDefault(): BotVariant = variants["default"]!!
}

@Serializable
data class BotVariant(
    val texture: String? = null,
    val model: String? = null
)
