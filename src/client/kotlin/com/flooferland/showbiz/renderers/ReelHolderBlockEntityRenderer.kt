package com.flooferland.showbiz.renderers

import com.flooferland.showbiz.blocks.entities.ReelHolderBlockEntity
import com.flooferland.showbiz.models.ReelHolderModel
import com.flooferland.showbiz.renderers.base.GeoFixedBlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider

class ReelHolderBlockEntityRenderer(ctx: BlockEntityRendererProvider.Context) : GeoFixedBlockEntityRenderer<ReelHolderBlockEntity>(ReelHolderModel())
