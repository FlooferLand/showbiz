package com.flooferland.showbiz.utils

import net.minecraft.core.*
import net.minecraft.server.level.*
import net.minecraft.sounds.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.level.*
import com.flooferland.showbiz.registry.ModSounds

/** Fast way to play all sorts of server-side sounds to the player */
object Sounds {
    fun play(player: Player, sound: ModSounds, volume: Float = 1.0f, pitch: Float = 1.0f) = play(player, sound.event, volume, pitch)
    fun play(player: Player, sound: SoundEvent, volume: Float = 1.0f, pitch: Float = 1.0f) {
        (player as? ServerPlayer)?.playNotifySound(sound, SoundSource.MASTER, volume, pitch)
    }

    fun play(level: Level, pos: BlockPos, sound: SoundEvent, source: SoundSource = SoundSource.BLOCKS, volume: Float = 1.0f, pitch: Float = 1.0f) {
        (level as? ServerLevel)?.playSound(null, pos, sound, source, volume, pitch)
    }
    fun bad(player: Player) {
        play(player, SoundEvents.NOTE_BLOCK_BASS.value(), volume = 1.0f, pitch = 0.5f)
    }

    fun click(player: Player) {
        play(player, ModSounds.ReelEnter, volume = 0.4f, pitch = 1.5f)
    }

    fun enter(player: Player) {
        play(player, ModSounds.ReelEnter, volume = 0.4f, pitch = 1.5f)
    }
    fun exit(player: Player) {
        play(player, ModSounds.ReelExit, volume = 0.4f, pitch = 1.5f)
    }
}