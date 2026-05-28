package com.flooferland.showbiz.blocks.entities

import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.network.protocol.game.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.types.connection.ConnectionManager
import com.flooferland.showbiz.types.connection.IConnectable
import com.flooferland.showbiz.types.connection.PortDirection
import com.flooferland.showbiz.types.connection.data.PackedVideoData

class MonitorBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.Monitor.entityType!!, pos, blockState), IConnectable {
    override val connectionManager = ConnectionManager(this)
    val video = connectionManager.port("video", PackedVideoData(), PortDirection.In)

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
        const val VIDEO_DIST = 24f
        const val VIDEO_DIST_SQUARE = VIDEO_DIST * VIDEO_DIST
    }
}