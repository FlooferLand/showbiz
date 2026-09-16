package com.flooferland.showbiz.mixin;

import com.flooferland.showbiz.registry.ModClientLights;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.BlockLightEngine;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockLightEngine.class)
abstract class BlockLightEngineMixin {
	@Inject(
		method = "getEmission",
		at = @At("RETURN"),
		cancellable = true
	)
	private void showbiz_getEmission(long packedPos, BlockState state, CallbackInfoReturnable<Integer> cir) {
		if (!isClient((LayerLightEventListener) (Object) this)) return;

		int light = cir.getReturnValue() + ModClientLights.INSTANCE.getLightLevel(packedPos);
		cir.setReturnValue(Math.clamp(light, 0, LightEngine.MAX_LEVEL));
	}

	@Unique
	private boolean isClient(LayerLightEventListener engine) {
		var level = Minecraft.getInstance().level;
		if (level == null) return false;
		return level.getLightEngine().getLayerListener(LightLayer.BLOCK) == engine;
	}
}
