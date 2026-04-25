package com.flooferland.showbiz.mixin;

import com.flooferland.showbiz.registry.ModPlayerSynchedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PlayerMixin {
	@Inject(method = "defineSynchedData", at = @At("HEAD"))
	void showbiz_defineSynchedData(SynchedEntityData.Builder builder, CallbackInfo ci) {
		ModPlayerSynchedData.Companion.defineSynchedData(builder);
	}

	@Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
	void showbiz_addAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
		var player = (Player) (Object) this;
		ModPlayerSynchedData.Companion.saveAdditional(tag, player);
	}

	@Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
	void showbiz_readAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
		var player = (Player) (Object) this;
		ModPlayerSynchedData.Companion.loadAdditional(tag, player);
	}
}
