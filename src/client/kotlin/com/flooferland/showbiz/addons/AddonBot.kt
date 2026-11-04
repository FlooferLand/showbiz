package com.flooferland.showbiz.addons

import com.flooferland.bizlib.bits.BotBitmapFile
import com.flooferland.showbiz.types.ResourcePath
import com.flooferland.showbiz.utils.rlCustom
import kotlinx.serialization.Serializable
import net.minecraft.resources.ResourceLocation

data class AddonBot(val assets: BotAssetsFile, val bitmap: BotBitmapFile, val resPath: ResourcePath,
                    val variants: Map<String, BotVariant>, val physics: PhysicsData? = null) {
    fun resPath(path: String) = rlCustom(getNamespace(), path)

    fun getNamespace() = this.resPath.namespace
    fun getId() = this.resPath.name
    fun getTexture(name: String) = resPath.resolve("textures").resolve(name).toLocation()
    fun getModel(name: String) = resPath.resolve("models").resolve(name).toLocation()

    private val defaultModelLocation by lazy { getModel(getId() + BotAssetsFile.MODEL_EXT) }
    private val defaultTextureLocation by lazy { getTexture(getId() + BotAssetsFile.TEXTURE_EXT) }
    fun getDefaultModel() = defaultModelLocation
    fun getDefaultTexture() = defaultTextureLocation
}

data class BotVariant(
    val model: ResourceLocation? = null,
    val texture: ResourceLocation? = null,
    val animations: ResourceLocation? = null,
)

@Serializable
data class BotAssetsFile(val variants: MutableMap<String, Variant>) {
    fun getDefault(): Variant = variants["default"]!!

    @Serializable
    data class Variant(
        var model: String? = null,
        var texture: String? = null,
        var animations: String? = null,
    ) {
        /** This is the best name I can come up with help */
        fun real(root: ResourcePath) = BotVariant(
            model = model?.let { modelLocation(root, it) },
            texture = texture?.let { textureLocation(root, it) },
            animations = animations?.let { animationLocation(root, it) },
        )
    }

    companion object {
        const val ANIM_EXT = ".animation.json"
        const val MODEL_EXT = ".geo.json"
        const val TEXTURE_EXT = ".png"

        fun modelLocation(root: ResourcePath, model: String) =
            root.resolve("models").resolve(model + MODEL_EXT).toLocation()

        fun animationLocation(root: ResourcePath, animations: String) =
            root.resolve("models").resolve(animations + ANIM_EXT).toLocation()

        fun textureLocation(root: ResourcePath, texture: String) =
            root.resolve("textures").resolve(texture + TEXTURE_EXT).toLocation()
    }
}
