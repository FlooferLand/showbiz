package com.flooferland.showbiz.renderers

import net.minecraft.client.renderer.blockentity.*
import com.flooferland.showbiz.blocks.entities.CymbalBlockEntity
import com.flooferland.showbiz.models.CymbalModel
import com.flooferland.showbiz.renderers.base.GeoFixedBlockEntityRenderer

class CymbalBlockEntityRenderer(ctx: BlockEntityRendererProvider.Context) : GeoFixedBlockEntityRenderer<CymbalBlockEntity>(CymbalModel())
