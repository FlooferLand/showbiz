package com.flooferland.showbiz.renderers

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import com.flooferland.showbiz.blocks.entities.CymbalBlockEntity
import com.flooferland.showbiz.models.CymbalModel
import com.flooferland.showbiz.renderers.base.GeoFixedBlockEntityRenderer

class CymbalBlockBlockEntityRenderer(ctx: BlockEntityRendererProvider.Context) : GeoFixedBlockEntityRenderer<CymbalBlockEntity>(CymbalModel())
