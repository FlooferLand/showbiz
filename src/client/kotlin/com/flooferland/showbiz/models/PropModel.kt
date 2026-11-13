package com.flooferland.showbiz.models

import com.flooferland.showbiz.entities.PropEntity
import com.flooferland.showbiz.items.PropItem
import com.flooferland.showbiz.registry.ModComponents
import com.flooferland.showbiz.renderers.PropItemRenderer
import com.flooferland.showbiz.utils.rl
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.animatable.GeoAnimatable
import software.bernie.geckolib.model.GeoModel

class PropModel<T : GeoAnimatable> : GeoModel<T>() {
    override fun getModelResource(animatable: T): ResourceLocation {
        val id = getPropId(animatable) ?: return rl("geo/empty.geo.json")
        return id.withPrefix("geo/prop_").withSuffix(".geo.json")
    }

    override fun getTextureResource(animatable: T): ResourceLocation {
        val id = getPropId(animatable) ?: return rl("textures/empty.png")
        return id.withPrefix("textures/item/prop_").withSuffix(".png")
    }

    override fun getAnimationResource(animatable: T) = null

    private fun getPropId(animatable: T) = when (animatable) {
        is PropEntity -> animatable.propId?.toLocation()
        is PropItem -> PropItemRenderer.currentStack?.get(ModComponents.Prop.type)?.id
        else -> null
    }
}