package com.flooferland.showbiz.renderers

import com.flooferland.showbiz.items.PropItem
import com.flooferland.showbiz.models.PropModel
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import software.bernie.geckolib.renderer.GeoItemRenderer

class PropItemRenderer : GeoItemRenderer<PropItem>(PropModel()) {
    override fun renderByItem(stack: ItemStack, transformType: ItemDisplayContext, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int) {
        currentStack = stack
        runCatching {
            super.renderByItem(stack, transformType, poseStack, bufferSource, packedLight, packedOverlay)
        }
        currentStack = null
    }
    companion object {
        var currentStack: ItemStack? = null
    }
}
