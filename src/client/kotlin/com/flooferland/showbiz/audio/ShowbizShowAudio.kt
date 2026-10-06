package com.flooferland.showbiz.audio

import net.minecraft.core.*
import com.flooferland.showbiz.ClientPackets
import com.flooferland.showbiz.network.packets.PlaybackAudioChunkPacket
import com.flooferland.showbiz.network.packets.PlaybackAudioStatePacket
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents

@Environment(EnvType.CLIENT)
object ShowbizShowAudio {
    val sources = mutableMapOf<BlockPos, Source>()
    private var gamePaused = false

    fun init() {
        ClientPackets.listen(PlaybackAudioChunkPacket.type) { payload, context ->
            context.client().execute {
                val source = sources.getOrPut(payload.blockPos) { Source(payload.format, payload.blockPos.center) }
                if (payload.playing) {
                    if (!source.isOpen()) source.open()
                    source.write(payload)
                }
            }
        }
        ClientPackets.listen(PlaybackAudioStatePacket.type) { packet, context ->
            context.client().execute {
                val source = sources[packet.blockPos] ?: return@execute
                if (packet.paused != source.paused) {
                    if (packet.paused) source.pause() else source.resume()
                }
                if (!packet.playing) {
                    source.close()
                    sources.remove(packet.blockPos)
                }
            }
        }

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val paused = client.isPaused
            if (paused == gamePaused) return@register
            gamePaused = paused
            for (source in sources.values) {
                if (paused) source.pause() else source.resume()
            }
        }

        // Cleanup in case the chunk unloads
        ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register { entity, level ->
            val state = sources[entity.blockPos] ?: return@register
            state.close()
            sources.remove(entity.blockPos)
        }
    }
}