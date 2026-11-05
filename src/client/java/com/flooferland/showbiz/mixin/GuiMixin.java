package com.flooferland.showbiz.mixin;

import com.flooferland.showbiz.entities.ModelPartEntity;
import com.flooferland.showbiz.renderers.ProgrammerRenderer;
import com.flooferland.showbiz.types.entity.PlayerProgrammingData;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {
	@Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
	private void showbiz_renderCrosshair(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		var minecraft = Minecraft.getInstance();
		var target = minecraft.crosshairPickEntity;

		boolean dontDraw = target instanceof ModelPartEntity;
		if (dontDraw) {
			ci.cancel();

			final int size = 15;
			RenderSystem.enableBlend();
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.2f);
			guiGraphics.blitSprite(ResourceLocation.withDefaultNamespace("hud/crosshair"), (guiGraphics.guiWidth() - size) / 2, (guiGraphics.guiHeight() - size) / 2, size, size);
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
			RenderSystem.defaultBlendFunc();
		}
	}

	@Inject(method = "renderHotbarAndDecorations", at = @At("HEAD"), cancellable = true)
	private void showbiz_renderHotbarAndDecorations(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		var minecraft = Minecraft.getInstance();
		var player = minecraft.player;
		if (player == null) return;

		var data = PlayerProgrammingData.Companion.getFromPlayer(player);
		if (data.getActive()) {
			ci.cancel();
			ProgrammerRenderer.INSTANCE.renderBitView(guiGraphics, data);
		}
	}
}
