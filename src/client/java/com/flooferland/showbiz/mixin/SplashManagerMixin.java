package com.flooferland.showbiz.mixin;

import com.flooferland.showbiz.utils.ShowbizUtils;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.resources.SplashManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SplashManager.class)
class SplashManagerMixin {
	@Inject(method = "getSplash", at = @At("HEAD"), cancellable = true)
	public void showbiz_getSplash(CallbackInfoReturnable<SplashRenderer> cir) {
		// Inside joke
		if (ShowbizUtils.INSTANCE.isSilly()) {
			cir.setReturnValue(new SplashRenderer("Fuck. You. /srs /gen /ij"));
			cir.cancel();
		}
	}
}
