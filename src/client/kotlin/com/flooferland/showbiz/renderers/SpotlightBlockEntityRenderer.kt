package com.flooferland.showbiz.renderers

import net.minecraft.client.renderer.blockentity.*
import com.flooferland.showbiz.blocks.entities.SpotlightBlockEntity
import com.flooferland.showbiz.models.SpotlightModel
import com.flooferland.showbiz.renderers.base.GeoFixedBlockEntityRenderer

class SpotlightBlockEntityRenderer(val context: BlockEntityRendererProvider.Context) : GeoFixedBlockEntityRenderer<SpotlightBlockEntity>(SpotlightModel()) {

}