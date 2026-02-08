package com.flooferland.showbiz.types

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth.clamp
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import com.flooferland.showbiz.ShowbizClient
import com.flooferland.showbiz.blocks.entities.StagedBotBlockEntity
import com.flooferland.showbiz.registry.ModSounds
import com.flooferland.showbiz.show.BitId
import java.util.WeakHashMap

class BotSoundHandler : IBotSoundHandler {
    private val lastBits = WeakHashMap<StagedBotBlockEntity, MutableMap<BitId, Boolean>>()

    override fun tick(entity: StagedBotBlockEntity, level: Level, pos: BlockPos, state: BlockState) {
        val level = level as? ClientLevel ?: return
        if (!ShowbizClient.config.audio.playPneumaticSounds) return

        val bot = ShowbizClient.bots[entity.botId] ?: return
        val show = entity.show.data
        val bitmapBits = bot.bitmap.bits[show.mapping] ?: return
        /*if (!show.playing) {
            lastBits.remove(entity)
            return
        }*/
        val states = lastBits.getOrPut(entity) { mutableMapOf() }

        for ((bit, data) in bitmapBits) {
            val bitOn = show.signal.frameHas(bit)
            val prevState = states[bit]

            if (prevState != null && prevState != bitOn) {
                val sound = if (bitOn) ModSounds.PneumaticFire else ModSounds.PneumaticRelease
                val flow = data.flow.toFloat().coerceIn(0.1f, 1.0f)
                val pitch = 0.4f + (flow * 0.8f)
                val volume = 0.5f + (flow * 0.5f)
                level.playLocalSound(
                    entity.blockPos.above().above(),
                    sound.event,
                    SoundSource.BLOCKS,
                    clamp(volume * 0.05f, 0.005f, 0.05f), pitch, false
                )
            }

            states[bit] = bitOn
        }
    }
}