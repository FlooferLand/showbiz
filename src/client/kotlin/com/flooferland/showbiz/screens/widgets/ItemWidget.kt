package com.flooferland.showbiz.screens.widgets

import net.minecraft.client.*
import net.minecraft.client.gui.*
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.narration.*
import net.minecraft.client.renderer.texture.*
import net.minecraft.client.resources.model.*
import net.minecraft.network.chat.*
import net.minecraft.world.item.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.ShowbizClient
import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis

class ItemWidget(private val stack: ItemStack, val size: Int = 16) : AbstractWidget(0, 0, 16, 16, Component.empty()) {
    var rotation = 0f
    init {
        width = size
        height = size
    }

    fun renderContent(graphics: GuiGraphics, poseStack: PoseStack, mouseX: Int, mouseY: Int, partialTick: Float) {
        val instance = Minecraft.getInstance() ?: return
        val level = instance.level ?: return
        val bakedModel: BakedModel = Minecraft.getInstance().getItemRenderer().getModel(stack, level, null, 0)
        val delta = ShowbizClient.getDeltaTime()

        poseStack.translate((x + (size / 2f)), (y + (size / 2f)), (150 + (if (bakedModel.isGui3d()) 5 else 0)).toFloat())
        if (isHovered) {
            val rotateDir = if (mouseX < x + (width / 2)) -1f else 1f
            rotation += (rotateDir * 4f) * delta
        } else {
            rotation = 0f
        }

        poseStack.scale(size.toFloat(), -size.toFloat(), size.toFloat())
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation))
        val flat = !bakedModel.usesBlockLight()
        if (flat) Lighting.setupForFlatItems()
        instance
            .getItemRenderer()
            .render(stack, ItemDisplayContext.GUI, false, poseStack, graphics.bufferSource(), 15728880, OverlayTexture.NO_OVERLAY, bakedModel)
        graphics.flush()
        if (flat) Lighting.setupFor3DItems()
    }

    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val poseStack = graphics.pose()
        graphics.enableScissor(x, y, x + width, y + height)
        poseStack.pushPose()
        try {
            renderContent(graphics, poseStack, mouseX, mouseY, partialTick)
        } catch (e: Exception) {
            Showbiz.log.error("Failed to render item '${stack.displayName}'", e)
        }
        poseStack.popPose()
        graphics.disableScissor()
    }

    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {}
}