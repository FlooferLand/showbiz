package com.flooferland.showbiz.renderers

import net.minecraft.client.renderer.culling.*
import net.minecraft.client.renderer.entity.*
import com.flooferland.showbiz.entities.FloodlightEntity
import com.flooferland.showbiz.models.FloodlightModel
import com.flooferland.showbiz.renderers.base.GeoFixedEntityRenderer
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer

class FloodlightEntityRenderer(ctx: EntityRendererProvider.Context) : GeoFixedEntityRenderer<FloodlightEntity>(ctx, FloodlightModel()) {
    init { addRenderLayer(AutoGlowingGeoLayer(this)) }
    override fun shouldShowName(entity: FloodlightEntity) = false

    // Prevents the light from teleporting to 0 0 0 since the light position is gotten from the GeckoLib model
    override fun shouldRender(entity: FloodlightEntity, camera: Frustum, camX: Double, camY: Double, camZ: Double) =
        super.shouldRender(entity, camera, camX, camY, camZ)
                || entity.isLit || entity.value > 0f
}