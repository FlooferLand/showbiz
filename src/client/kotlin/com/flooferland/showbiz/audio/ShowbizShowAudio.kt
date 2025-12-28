package com.flooferland.showbiz.audio

import com.flooferland.showbiz.blocks.entities.ReelToReelBlockEntity
import com.flooferland.showbiz.network.packets.PlaybackChunkPacket
import com.flooferland.showbiz.network.packets.PlaybackStatePacket
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.core.*
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents

@Environment(EnvType.CLIENT)
object ShowbizShowAudio {
    val sources = mutableMapOf<BlockPos, Source>()

    fun init() {
        ClientPlayNetworking.registerGlobalReceiver(PlaybackChunkPacket.type) { payload, context ->
            context.client().execute {
                val source = sources.getOrPut(payload.blockPos) { Source(payload.format, payload.blockPos.center) }
                if (payload.playing) {
                    if (!source.isOpen()) source.open()
                    source.write(payload.audioChunk, payload.format.sampleRate)
                }
            }
        }
        ClientPlayNetworking.registerGlobalReceiver(PlaybackStatePacket.type) { payload, context ->
            context.client().execute {
                val state = sources[payload.blockPos] ?: return@execute
                when (payload.playing) {
                    true -> state.open()
                    false -> state.close()
                }
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