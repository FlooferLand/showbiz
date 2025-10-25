package com.flooferland.showbiz.addons.assets

import com.flooferland.showbiz.utils.rlCustom
import net.minecraft.resources.*
import software.bernie.geckolib.loading.json.raw.Model

data class AddonAssets(val id: String, val bots: Map<String, AddonBot>, val models: MutableMap<ResourceLocation, Model>) {
    fun resFor(path: String): ResourceLocation {
        return rlCustom(id, path)
    }
}