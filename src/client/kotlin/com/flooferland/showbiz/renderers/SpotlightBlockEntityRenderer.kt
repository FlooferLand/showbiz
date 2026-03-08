package com.flooferland.showbiz.renderers

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import com.flooferland.showbiz.blocks.entities.SpotlightBlockEntity
import com.flooferland.showbiz.models.SpotlightModel
import software.bernie.geckolib.renderer.GeoBlockRenderer

class SpotlightBlockEntityRenderer(val context: BlockEntityRendererProvider.Context) : GeoBlockRenderer<SpotlightBlockEntity>(SpotlightModel()) {
}