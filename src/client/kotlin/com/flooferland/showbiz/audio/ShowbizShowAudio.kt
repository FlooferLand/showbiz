package com.flooferland.showbiz.audio

import net.minecraft.client.multiplayer.*
import net.minecraft.core.*
import com.flooferland.showbiz.ClientPackets
import com.flooferland.showbiz.blocks.entities.ReelToReelBlockEntity
import com.flooferland.showbiz.network.packets.PlaybackChunkPacket
import com.flooferland.showbiz.network.packets.PlaybackStatePacket
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents

@Environment(EnvType.CLIENT)
object ShowbizShowAudio {
    val sources = mutableMapOf<BlockPos, Source>()

    fun init() {
        ClientPackets.listen(PlaybackChunkPacket.type) { payload, context ->
            context.client().execute {
                val source = sources.getOrPut(payload.blockPos) { Source(payload.format, payload.blockPos.center) }
                if (payload.playing) {
                    if (!source.isOpen()) source.open()
                    source.write(payload)
                }
            }
        }
        ClientPackets.listen(PlaybackStatePacket.type) { packet, context ->
            context.client().execute {
                val level = context.player().level() as? ClientLevel ?: return@execute
                val blockEntity = level.getBlockEntity(packet.blockPos) as? ReelToReelBlockEntity ?: return@execute
                blockEntity.clientApplyPlaybackState(packet)
                val state = sources[packet.blockPos] ?: return@execute
                if (!packet.playing) {
                    state.close()
                    sources.remove(packet.blockPos)
                    return@execute
                }
                if (packet.paused) {
                    state.pause()
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