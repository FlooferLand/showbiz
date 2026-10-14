package com.flooferland.showbiz.mixin;

import com.flooferland.showbiz.registry.ModClientLights;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameRenderer.class, priority = 900)
public class GameRendererMixin {
	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void showbiz_render(DeltaTracker deltaTracker, CallbackInfo ci) {
		var level = Minecraft.getInstance().level;
		if (level == null) return;
		ModClientLights.INSTANCE.emit(level, deltaTracker.getGameTimeDeltaPartialTick(true));
	}
}
