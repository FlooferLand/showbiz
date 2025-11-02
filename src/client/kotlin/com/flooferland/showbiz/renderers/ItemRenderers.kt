package com.flooferland.showbiz.renderers

import com.flooferland.showbiz.items.WandItem
import com.flooferland.showbiz.registry.ModItems
import software.bernie.geckolib.model.DefaultedItemGeoModel
import software.bernie.geckolib.renderer.GeoItemRenderer

class WandItemRenderer : GeoItemRenderer<WandItem>(DefaultedItemGeoModel(ModItems.Wand.id));