package com.flooferland.showbiz

import com.flooferland.showbiz.blocks.entities.PlaybackControllerBlockEntity
import com.flooferland.showbiz.network.base.PlaybackChunkPacket
import com.flooferland.showbiz.network.base.PlaybackStatePacket
import com.flooferland.showbiz.utils.ShowbizUtils
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.core.*
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.SourceDataLine

@Environment(EnvType.CLIENT)
object ShowbizShowAudio {
    val states = mutableMapOf<BlockPos, State>()
    data class State(
        var aFormat: AudioFormat? = null,
        var audioLine: SourceDataLine? = null,
        var lastFramePlaying: Boolean = false
    ) {
        fun isOpen() = audioLine != null && audioLine?.isOpen == true
        fun open(entity: PlaybackControllerBlockEntity) {
            audioLine?.close()
            aFormat = entity.getFormat()
            audioLine = ShowbizUtils.startAudioDevice(aFormat!!, entity.aBufferSize).getOrNull()
            println("Started device: open=${audioLine?.isOpen}")
        }
        fun close() {
            audioLine?.runCatching { stop(); flush(); close() }
            audioLine = null
            println("Closed device")
        }
        fun write(chunk: ByteArray) {
            if (!isOpen()) return
            val available = audioLine?.available() ?: 0
            if (available >= chunk.size) {
                val res = runCatching { audioLine?.write(chunk, 0, chunk.size) }
                res.onFailure { it.printStackTrace() }
            }
        }
    }

    fun init() {
        ClientPlayNetworking.registerGlobalReceiver(PlaybackChunkPacket.type) { payload, context ->
            val level = context.player().level() ?: return@registerGlobalReceiver
            val entity = level.getBlockEntity(payload.blockPos) as? PlaybackControllerBlockEntity ?: return@registerGlobalReceiver

            val state = states.getOrPut(payload.blockPos) { State() }
            if (entity.playing) {
                if (!state.isOpen())
                    state.open(entity)
                state.write(payload.audioChunk)
            }
        }
        ClientPlayNetworking.registerGlobalReceiver(PlaybackStatePacket.type) { payload, context ->
            val level = context.player().level() ?: return@registerGlobalReceiver
            val entity = level.getBlockEntity(payload.blockPos) as? PlaybackControllerBlockEntity ?: return@registerGlobalReceiver
            val state = states[payload.blockPos] ?: return@registerGlobalReceiver
            when (payload.playing) {
                true -> if (!state.isOpen()) state.open(entity)
                false -> if (state.isOpen()) state.close()
            }
        }

        // In case a chunk loads
        ClientChunkEvents.CHUNK_LOAD.register { level, chunk ->
            chunk.blockEntities.forEach { (blockPos, entity) ->
                if (entity !is PlaybackControllerBlockEntity) return@forEach

                println("LOADED: playing=${entity.playing}")
                if (entity.playing) {
                    states[blockPos]?.close()
                    states.remove(entity.blockPos)

                    val state = states.getOrPut(blockPos) { State() }
                    state.open(entity)
                }
            }
        }

        // Cleanup in case the chunk unloads
        ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register { entity, level ->
            if (entity !is PlaybackControllerBlockEntity) return@register
            println("UNLOADED")
             run {
                 val state = states[entity.blockPos] ?: return@register
                 state.close()
             }
            states.remove(entity.blockPos)
        }
    }
}