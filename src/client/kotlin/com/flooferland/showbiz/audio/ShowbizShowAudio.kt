package com.flooferland.showbiz.audio

import net.minecraft.core.*
import com.flooferland.showbiz.network.packets.PlaybackChunkPacket
import com.flooferland.showbiz.network.packets.PlaybackStatePacket
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

@Environment(EnvType.CLIENT)
object ShowbizShowAudio {
    val sources = mutableMapOf<BlockPos, Source>()

    fun init() {
        ClientPlayNetworking.registerGlobalReceiver(PlaybackChunkPacket.type) { payload, context ->
            context.client().execute {
                val source = sources.getOrPut(payload.blockPos) { Source(payload.format, payload.blockPos.center) }
                if (payload.playing) {
                    if (!source.isOpen()) source.open()
                    source.write(payload)
                }
            }
        }
        ClientPlayNetworking.registerGlobalReceiver(PlaybackStatePacket.type) { payload, context ->
            context.client().execute {
                val state = sources[payload.blockPos] ?: return@execute
                if (!payload.playing) {
                    state.close()
                    sources.remove(payload.blockPos)
                    return@execute
                }
                if (payload.paused) {
                    state.paused
                } else {
                    if (!state.isOpen()) state.open()
                    state.resume()
                }
            }
        }

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val paused = client.isPaused
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