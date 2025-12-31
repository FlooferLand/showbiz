package com.flooferland.showbiz.blocks.entities

import com.flooferland.showbiz.registry.ModBlocks
import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.network.protocol.game.*
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import com.flooferland.showbiz.types.connection.ConnectionManager
import com.flooferland.showbiz.types.connection.IConnectable
import com.flooferland.showbiz.types.connection.PortDirection
import com.flooferland.showbiz.types.connection.data.PackedAudioData

class SpeakerBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.Speaker.entityType!!, pos, blockState), IConnectable {
    override val connectionManager = ConnectionManager(this)
    val audio = connectionManager.port("audio", PackedAudioData(), PortDirection.In) {
        val level = level as? ServerLevel ?: return@port
        it.broadcastToAll(level, blockPos)
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        connectionManager.save(tag)
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        connectionManager.load(tag)
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag? {
        val tag = super.getUpdateTag(registries)
        saveAdditional(tag, registries)
        return tag
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket =
        ClientboundBlockEntityDataPacket.create(this)

    companion object {
        const val AUDIO_DIST = 24f
        const val AUDIO_DIST_SQUARE = AUDIO_DIST * AUDIO_DIST
    }
}