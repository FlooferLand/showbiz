package com.flooferland.showbiz.audio

import com.flooferland.showbiz.blocks.entities.PlaybackControllerBlockEntity
import com.flooferland.showbiz.network.base.PlaybackChunkPacket
import com.flooferland.showbiz.network.base.PlaybackStatePacket
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.core.*

@Environment(EnvType.CLIENT)
object ShowbizShowAudio {
    val sources = mutableMapOf<BlockPos, Source>()

    fun init() {
        ClientPlayNetworking.registerGlobalReceiver(PlaybackChunkPacket.type) { payload, context ->
            val level = context.player().level() ?: return@registerGlobalReceiver
            val entity = level.getBlockEntity(payload.blockPos) as? PlaybackControllerBlockEntity ?: return@registerGlobalReceiver

            val source = sources.getOrPut(payload.blockPos) { Source(entity.getFormat(), entity.blockPos.center) }
            if (entity.playing) {
                if (!source.isOpen()) source.open()
                source.write(payload.audioChunk, entity.getFormat().sampleRate.toInt())
            }
        }
        ClientPlayNetworking.registerGlobalReceiver(PlaybackStatePacket.type) { payload, context ->
            val level = context.player().level() ?: return@registerGlobalReceiver
            val entity = level.getBlockEntity(payload.blockPos) as? PlaybackControllerBlockEntity ?: return@registerGlobalReceiver
            val state = sources[payload.blockPos] ?: return@registerGlobalReceiver
            when (payload.playing) {
                true -> state.open()
                false -> state.close()
            }
        }

        // In case a chunk loads
        ClientChunkEvents.CHUNK_LOAD.register { level, chunk ->
            chunk.blockEntities.forEach { (blockPos, entity) ->
                if (entity !is PlaybackControllerBlockEntity) return@forEach

                if (entity.playing) {
                    sources[blockPos]?.close()
                    sources.remove(entity.blockPos)

                    val source = sources.getOrPut(blockPos) { Source(entity.getFormat(), entity.blockPos.center) }
                    source.open()
                }
            }
        }

        // Cleanup in case the chunk unloads
        ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register { entity, level ->
            if (entity !is PlaybackControllerBlockEntity) return@register
             run {
                 val state = sources[entity.blockPos] ?: return@register
                 state.close()
             }
            sources.remove(entity.blockPos)
        }
    }
}