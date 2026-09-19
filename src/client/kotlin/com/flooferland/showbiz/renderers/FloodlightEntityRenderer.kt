package com.flooferland.showbiz.renderers

import net.minecraft.client.renderer.entity.*
import com.flooferland.showbiz.entities.FloodlightEntity
import com.flooferland.showbiz.models.FloodlightModel
import com.flooferland.showbiz.renderers.base.GeoFixedEntityRenderer
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer

class FloodlightEntityRenderer(ctx: EntityRendererProvider.Context) : GeoFixedEntityRenderer<FloodlightEntity>(ctx, FloodlightModel()) {
    init { addRenderLayer(AutoGlowingGeoLayer(this)) }
    override fun shouldShowName(entity: FloodlightEntity) = false
}