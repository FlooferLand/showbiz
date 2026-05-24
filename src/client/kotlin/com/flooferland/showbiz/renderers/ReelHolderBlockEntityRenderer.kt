package com.flooferland.showbiz.renderers

import net.minecraft.client.renderer.blockentity.*
import com.flooferland.showbiz.blocks.entities.ReelHolderBlockEntity
import com.flooferland.showbiz.models.ReelHolderModel
import software.bernie.geckolib.renderer.GeoBlockRenderer

class ReelHolderBlockEntityRenderer(ctx: BlockEntityRendererProvider.Context) : GeoBlockRenderer<ReelHolderBlockEntity>(ReelHolderModel())
