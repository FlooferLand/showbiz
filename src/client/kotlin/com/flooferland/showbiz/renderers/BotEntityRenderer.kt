package com.flooferland.showbiz.renderers

import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.entity.*
import com.flooferland.showbiz.entities.BotEntity
import com.flooferland.showbiz.models.BotModel
import com.flooferland.showbiz.renderers.base.GeoFixedEntityRenderer

class BotEntityRenderer(ctx: EntityRendererProvider.Context) : GeoFixedEntityRenderer<BotEntity>(ctx, BotModel()) {
    override fun shouldShowName(entity: BotEntity) = false
    override fun shouldRender(livingEntity: BotEntity, camera: Frustum, camX: Double, camY: Double, camZ: Double): Boolean {
        // TODO: Figure out a better fix to solve Sodium cutting off entities early. Bad for performance.
        //       Not sure how to dynamically scale the entity dimensions based on the bot size cause of addons, or if that's even a good idea
        return true;
    }
}