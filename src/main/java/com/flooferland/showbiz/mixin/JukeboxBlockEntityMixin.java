package com.flooferland.showbiz.mixin;

import com.flooferland.showbiz.ILyricHolder;
import com.flooferland.showbiz.items.base.MusicDiscItem;
import com.flooferland.showbiz.network.packets.JukeboxLyricPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(JukeboxBlockEntity.class)
public class JukeboxBlockEntityMixin implements ILyricHolder {
	@Unique @NotNull private String showbiz_lyric = "";

	@Override public @NotNull String showbiz_getLyric() { return showbiz_lyric; }
	@Override public void showbiz_setLyric(@NotNull String lyric) { showbiz_lyric = lyric; }

	@Inject(method = "tick", at = @At("HEAD"))
	private static void showbiz_tick(Level level0, BlockPos pos, BlockState state, JukeboxBlockEntity jukebox, CallbackInfo ci) {
		if (!(level0 instanceof ServerLevel level)) return;
		if (!(jukebox instanceof ILyricHolder lyricHolder)) return;
		var stack = jukebox.getTheItem();
		var songPlayer = jukebox.getSongPlayer();
		boolean isPlaying = songPlayer.isPlaying();
		boolean isShowbiz = stack.getItem() instanceof MusicDiscItem;

		if (!isShowbiz || !isPlaying) {
			if (!lyricHolder.showbiz_getLyric().isEmpty()) {
				lyricHolder.showbiz_setLyric("");
				showbiz_updatePlayers(level, pos, "");
			}
			return;
		}

		var data = ((MusicDiscItem) stack.getItem()).getDisc();
		if (songPlayer.isPlaying() && !data.getLyrics().isEmpty()) {
			long progress = songPlayer.getTicksSinceSongStarted();
			var oldLyric = lyricHolder.showbiz_getLyric();
			var lyric = data.getLyric(progress);
			if (!Objects.equals(oldLyric, lyric)) {
				lyricHolder.showbiz_setLyric(lyric);
				showbiz_updatePlayers(level, pos, lyric);
			}
		}
	}

	@Inject(method = "popOutTheItem", at = @At("HEAD"))
	private void showbiz_popOutTheItem(CallbackInfo ci) {
		var jukebox = (JukeboxBlockEntity) (Object) this;
		if (!(jukebox instanceof ILyricHolder lyricHolder)) return;
		if (!(jukebox.getLevel() instanceof ServerLevel level)) return;

		lyricHolder.showbiz_setLyric("");
		showbiz_updatePlayers(level, jukebox.getBlockPos(), "");
	}

	/// Sends packets to all players, but I can't be bothered to figure out a way to send an end packet when the player is out of range
	@Unique
	private static void showbiz_updatePlayers(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull String lyric) {
		var packet = new JukeboxLyricPacket(pos, lyric);
		for (var player : level.players())
			ServerPlayNetworking.send(player, packet);
	}
}
