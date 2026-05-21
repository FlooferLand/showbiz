package com.flooferland.showbiz.renderers

import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import com.flooferland.showbiz.items.PlushBlockItem
import com.flooferland.showbiz.models.PlushModel
import com.mojang.blaze3d.vertex.PoseStack
import software.bernie.geckolib.renderer.GeoItemRenderer

class PlushItemRenderer : GeoItemRenderer<PlushBlockItem>(PlushModel()) {
    override fun renderByItem(stack: ItemStack, transformType: ItemDisplayContext, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int) {
        currentStack = stack
        super.renderByItem(stack, transformType, poseStack, bufferSource, packedLight, packedOverlay)
        currentStack = null
    }
    companion object {
        var currentStack: ItemStack? = null
    }
}
