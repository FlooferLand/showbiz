package com.flooferland.showbiz.mixin;

import com.flooferland.showbiz.Showbiz;
import com.flooferland.showbiz.types.IVersionMigrator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkSerializer.class)
public class ChunkSerializerMixin {
	@Inject(method = "read", at = @At("HEAD"))
	private static void showbiz$tagLegacy(ServerLevel level, PoiManager poi, RegionStorageInfo info, ChunkPos pos, CompoundTag tag, CallbackInfoReturnable<ProtoChunk> cir) {
		try { IVersionMigrator.tagLegacy(tag); } catch (Exception e) {
			Showbiz.log.error("Failed to migrate chunk '{}'", pos, e);
		}
	}
}
