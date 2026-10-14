package com.flooferland.showbiz.mixin;

import com.flooferland.showbiz.accessor.TextureSizeAccessor;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SimpleTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SimpleTexture.class)
public class SimpleTextureMixin implements TextureSizeAccessor {
	@Unique protected int showbiz_width = 0;
	@Unique protected int showbiz_height = 0;

	@Inject(method = "doLoad", at = @At("HEAD"))
	private void showbiz_doLoad(NativeImage image, boolean blur, boolean clamp, CallbackInfo ci) {
		showbiz_width = image.getWidth();
		showbiz_height = image.getHeight();
	}

	@Override
	public int showbiz_getWidth() { return showbiz_width; }

	@Override
	public int showbiz_getHeight() { return showbiz_height; }
}
