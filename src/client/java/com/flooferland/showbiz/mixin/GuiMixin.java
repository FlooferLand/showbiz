package com.flooferland.showbiz.mixin;

import com.flooferland.showbiz.renderers.ProgrammerRenderer;
import com.flooferland.showbiz.types.entity.PlayerProgrammingData;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {
	@Inject(method = "renderHotbarAndDecorations", at = @At("HEAD"), cancellable = true)
	private void showbiz_renderHotbarAndDecorations(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		var minecraft = Minecraft.getInstance();
		var font = minecraft.font;
		var player = minecraft.player;
		if (player == null) return;

		var data = PlayerProgrammingData.Companion.getFromPlayer(player);
		if (data.getActive()) {
			ci.cancel();
			ProgrammerRenderer.INSTANCE.renderBitView(guiGraphics, data);
		}
	}
}
