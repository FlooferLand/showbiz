package com.flooferland.showbiz.blocks.entities

import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.types.connection.ConnectionManager
import com.flooferland.showbiz.types.connection.IConnectable
import com.flooferland.showbiz.types.connection.PortDirection
import com.flooferland.showbiz.types.connection.data.PackedVideoData
import com.flooferland.showbiz.types.math.Color4
import com.flooferland.showbiz.utils.Extensions.bytesToFriendlySize
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import kotlin.time.Duration.Companion.nanoseconds

class MonitorBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.Monitor.entityType!!, pos, blockState), IConnectable {
    override val connectionManager = ConnectionManager(this)
    val video = connectionManager.port("video", PackedVideoData(), PortDirection.In) {
        val elapsedMillis = (System.nanoTime() - it.timestamp).nanoseconds.inWholeMilliseconds
        if (elapsedMillis > 1000) {
            Showbiz.log.warn("$blockPos video frame transit time: $elapsedMillis ms (size = ${it.bytes.size.bytesToFriendlySize()})")
        }
    }
    var colorAverage = Color4()

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