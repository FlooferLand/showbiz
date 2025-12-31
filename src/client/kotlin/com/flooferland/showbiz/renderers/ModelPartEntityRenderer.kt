package com.flooferland.showbiz.renderers

import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import com.flooferland.showbiz.entities.ModelPartEntity

class ModelPartEntityRenderer(context: EntityRendererProvider.Context) : EntityRenderer<ModelPartEntity>(context) {
    override fun getTextureLocation(entity: ModelPartEntity?) = null
}