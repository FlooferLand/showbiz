package com.flooferland.showbiz.types

import net.minecraft.client.*
import net.minecraft.client.multiplayer.*
import net.minecraft.sounds.*
import net.minecraft.util.Mth.*
import com.flooferland.bizlib.bits.MoveType
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.ShowbizClient
import com.flooferland.showbiz.registry.ModSounds
import com.flooferland.showbiz.show.BitId
import java.util.WeakHashMap

class BotSoundHandler : IBotAttachment {
    private val lastBits = WeakHashMap<IBot, MutableMap<BitId, Boolean>>()

    override fun tick(entity: IBot) {
        if (!Showbiz.config.audio.playPneumaticSounds) return

        val bot = ShowbizClient.bots[entity.botId] ?: return
        val level = entity.botLevel as? ClientLevel ?: return
        val pos = entity.botPos?.add(0.0, 2.0, 0.0) ?: return
        val show = entity.show?.data ?: return
        val bitmapBits = bot.bitmap.bits[show.mapping] ?: return
        /*if (!show.playing) {
            lastBits.remove(entity)
            return
        }*/
        val states = lastBits.getOrPut(entity) { mutableMapOf() }

        val minVolume = 0.04f
        val maxVolume = 0.1f
        for ((bit, data) in bitmapBits) {
            val bitOn = show.signal.frameHas(bit)
            val prevState = states[bit]
            if (data.type == MoveType.Effect) continue

            if (prevState != null && prevState != bitOn) {
                val playerDist = Minecraft.getInstance().player?.distanceToSqr(pos) ?: 0.0
                val playerDistMul = if (playerDist < 3 * 3) 1.5f else minVolume
                val sound = if (bitOn) ModSounds.PneumaticFire else ModSounds.PneumaticRelease
                val flow = data.flow.speed.toFloat().coerceIn(0.1f, 1.0f)
                val pitch = 0.3f + (flow * 0.9f)
                val volume = (0.5f * playerDistMul) + (flow * 0.5f)
                level.playLocalSound(
                    pos.x, pos.y, pos.z,
                    sound.event,
                    SoundSource.BLOCKS,
                    clamp(volume * maxVolume, minVolume, maxVolume * playerDistMul), pitch, false
                )
            }

            states[bit] = bitOn
        }
    }
}