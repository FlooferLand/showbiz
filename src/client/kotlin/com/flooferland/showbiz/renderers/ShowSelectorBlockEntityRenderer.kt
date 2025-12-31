package com.flooferland.showbiz.renderers

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import com.flooferland.showbiz.blocks.entities.ShowSelectorBlockEntity
import com.flooferland.showbiz.models.ShowSelectorBlockEntityModel
import software.bernie.geckolib.renderer.GeoBlockRenderer

class ShowSelectorBlockEntityRenderer(val context: BlockEntityRendererProvider.Context) : GeoBlockRenderer<ShowSelectorBlockEntity>(ShowSelectorBlockEntityModel()) {
}