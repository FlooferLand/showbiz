package com.flooferland.showbiz.renderers

import com.flooferland.showbiz.entities.PropEntity
import com.flooferland.showbiz.models.PropModel
import net.minecraft.client.renderer.entity.EntityRendererProvider
import software.bernie.geckolib.renderer.GeoEntityRenderer

class PropEntityRenderer(ctx: EntityRendererProvider.Context) : GeoEntityRenderer<PropEntity>(ctx, PropModel<PropEntity>()) {
    override fun shouldShowName(entity: PropEntity) = false
}