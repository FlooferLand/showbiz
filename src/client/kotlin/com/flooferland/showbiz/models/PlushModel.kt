package com.flooferland.showbiz.models

import net.minecraft.resources.*
import com.flooferland.showbiz.blocks.entities.PlushBlockEntity
import com.flooferland.showbiz.items.PlushBlockItem
import com.flooferland.showbiz.registry.ModComponents
import com.flooferland.showbiz.renderers.PlushItemRenderer
import com.flooferland.showbiz.utils.rl
import software.bernie.geckolib.animatable.GeoAnimatable
import software.bernie.geckolib.model.GeoModel

class PlushModel<T : GeoAnimatable> : GeoModel<T>() {
    override fun getModelResource(animatable: T): ResourceLocation {
        val id = getPlushId(animatable) ?: return rl("geo/empty.geo.json")
        return id.withPrefix("geo/plush_").withSuffix(".geo.json")
    }

    override fun getTextureResource(animatable: T): ResourceLocation {
        val id = getPlushId(animatable) ?: return rl("textures/empty.png")
        return id.withPrefix("textures/plush_").withSuffix(".png")
    }

    override fun getAnimationResource(animatable: T) = null

    private fun getPlushId(animatable: T) = when (animatable) {
        is PlushBlockEntity -> animatable.itemStack.get(ModComponents.Plush.type)?.id
        is PlushBlockItem -> PlushItemRenderer.currentStack?.get(ModComponents.Plush.type)?.id
        else -> null
    }
}