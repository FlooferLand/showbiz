package com.flooferland.showbiz.renderers

import net.minecraft.client.renderer.*
import net.minecraft.world.item.*
import com.flooferland.showbiz.items.PlushItem
import com.flooferland.showbiz.models.PlushModel
import com.mojang.blaze3d.vertex.PoseStack
import software.bernie.geckolib.renderer.GeoItemRenderer

class PlushItemRenderer : GeoItemRenderer<PlushItem>(PlushModel()) {
    override fun renderByItem(stack: ItemStack, transformType: ItemDisplayContext, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int) {
        currentStack = stack
        super.renderByItem(stack, transformType, poseStack, bufferSource, packedLight, packedOverlay)
        currentStack = null
    }
    companion object {
        var currentStack: ItemStack? = null
    }
}
