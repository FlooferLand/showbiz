package com.flooferland.showbiz.models

import net.minecraft.resources.ResourceLocation
import com.flooferland.showbiz.blocks.ShowSelectorBlock
import com.flooferland.showbiz.blocks.entities.ShowSelectorBlockEntity
import com.flooferland.showbiz.utils.rl
import software.bernie.geckolib.model.DefaultedBlockGeoModel
import kotlin.jvm.optionals.getOrNull

class ShowSelectorBlockEntityModel : DefaultedBlockGeoModel<ShowSelectorBlockEntity>(rl("show_selector")) {
    override fun getModelResource(animatable: ShowSelectorBlockEntity): ResourceLocation {
        val wallMounted = animatable.blockState?.getOptionalValue(ShowSelectorBlock.WALL_MOUNTED)?.getOrNull() ?: false
        return if (wallMounted)
            buildFormattedModelPath(rl("show_selector_wall"))
        else super.getModelResource(animatable)
    }
}