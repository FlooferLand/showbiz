package com.flooferland.showbiz.mixin;

import com.flooferland.showbiz.types.IRedstoneExtras;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional because Neoforge messes up the function signatures */
@Mixin(RedStoneWireBlock.class)
public class RedStoneWireBlockMixin {
	@Inject(
		method = "shouldConnectTo(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private static void showbiz$shouldConnectTo(BlockState state, Direction direction, CallbackInfoReturnable<Boolean> cir) {
		if (state.getBlock() instanceof IRedstoneExtras extras) {
			cir.setReturnValue(extras.wireShouldConnectTo(state, direction));
		}
	}

	@Inject(
		method = "getConnectingSide(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Z)Lnet/minecraft/world/level/block/state/properties/RedstoneSide;",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private static void showbiz$getConnectingSide(BlockGetter level, BlockPos pos, Direction direction, boolean nonNormalCubeAbove, CallbackInfoReturnable<RedstoneSide> cir) {
		var neighbourState = level.getBlockState(pos.relative(direction));

		if (neighbourState.getBlock() instanceof IRedstoneExtras extras) {
			if (extras.wireShouldConnectTo(neighbourState, direction))
				cir.setReturnValue(RedstoneSide.SIDE);
			else
				cir.setReturnValue(RedstoneSide.NONE);
		}
	}
}
