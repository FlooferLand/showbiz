package com.flooferland.showbiz.mixin;

import com.flooferland.showbiz.registry.ModClientLights;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.BlockLightEngine;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockLightEngine.class)
class BlockLightEngineMixin {
	@Inject(
		method = "getEmission",
		at = @At("RETURN"),
		cancellable = true
	)
	private void showbiz_getEmission(long packedPos, BlockState state, CallbackInfoReturnable<Integer> cir) {
		int light = cir.getReturnValue() + ModClientLights.INSTANCE.getLightLevel(packedPos);
		cir.setReturnValue(Math.clamp(light, 0, LightEngine.MAX_LEVEL));
	}
}
