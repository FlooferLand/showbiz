package com.flooferland.showbiz.utils

import com.flooferland.showbiz.registry.ModSounds
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level

/** Fast way to play all sorts of server-side sounds to the player */
object Sounds {
    interface Source {
        fun play(sound: SoundEvent, source: SoundSource? = null, volume: Float = 1.0f, pitch: Float = 1.0f)

        fun play(sound: ModSounds, source: SoundSource? = null, volume: Float = 1.0f, pitch: Float = 1.0f) {
            play(sound.event, source, volume, pitch)
        }

        fun good() = play(SoundEvents.NOTE_BLOCK_CHIME.value(), volume = 0.8f, pitch = 1.5f)
        fun bad() = play(SoundEvents.NOTE_BLOCK_BASS.value(), volume = 0.8f, pitch = 0.5f)
        fun click() = play(ModSounds.ReelEnter, volume = 0.4f, pitch = 1.5f)
        fun enter() = play(ModSounds.ReelEnter, volume = 0.4f, pitch = 1.5f)
        fun exit() = play(ModSounds.ReelExit, volume = 0.4f, pitch = 1.5f)
    }

    //region Sources
    data class BlockSource(val level: Level, val pos: BlockPos) : Source {
        override fun play(sound: SoundEvent, source: SoundSource?, volume: Float, pitch: Float) {
            val source = source ?: SoundSource.BLOCKS
            (level as? ServerLevel)?.playSound(null, pos, sound, source, volume, pitch)
        }
    }

    data class PlayerSource(val player: Player, val local: Boolean = false) : Source {
        override fun play(sound: SoundEvent, source: SoundSource?, volume: Float, pitch: Float) {
            if (local) {
                player.playNotifySound(sound, SoundSource.MASTER, volume, pitch)
            } else if (player is ServerPlayer) {
                player.playNotifySound(sound, SoundSource.MASTER, volume, pitch)
            }
        }
    }
    //endregion

    fun block(level: Level, pos: BlockPos) = BlockSource(level, pos)
    fun serverPlayer(player: Player) = PlayerSource(player, local = false)
    fun localPlayer(player: Player) = PlayerSource(player, local = true)
}