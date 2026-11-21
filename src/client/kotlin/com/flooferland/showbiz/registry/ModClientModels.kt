package com.flooferland.showbiz.registry

import com.flooferland.showbiz.utils.ClientExtensions.isMissing
import com.flooferland.showbiz.utils.Extensions.forceLoad
import com.flooferland.showbiz.utils.rl
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.resources.ResourceLocation

sealed class ModClientModels {
    object Reel : ModClientModels("item/reel")
    object ReelClean : ModClientModels("item/reel_clean")

    val id: ResourceLocation
    constructor(id: String) {
        this.id = rl(id)
        entries += this
    }

    fun getModel(): BakedModel? = runCatching {
        val model = Minecraft.getInstance().modelManager.getModel(id)
        if (model.isMissing()) null else model
    }.getOrNull()

    companion object {
        val entries = mutableSetOf<ModClientModels>()

        fun register() {
            ModClientModels::class.forceLoad()
            ModelLoadingPlugin.register { ctx ->
                ctx.addModels(entries.map { it.id })
            }
        }
    }
}