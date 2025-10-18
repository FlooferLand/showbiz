package com.flooferland.showbiz.blocks.entities

import com.flooferland.bizlib.RawShowData
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.utils.Extensions.getBooleanOrNull
import com.flooferland.showbiz.utils.Extensions.getDoubleOrNull
import com.flooferland.showbiz.utils.Extensions.markDirtyNotifyAll
import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.network.protocol.game.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*

class PlaybackControllerBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.PlaybackController.entity!!, pos, blockState) {
    public var show: RawShowData? = null
    public var playing = false
    public var seek = 0.0
    public var signal = 0

    private val tickInSecs: Double = 0.053  // TODO: Implement deltatime

    fun tick() {
        if (!playing || show == null) return
        seek += tickInSecs

        val index = seek.toInt().coerceIn(0, show?.signal?.size ?: 0)
        if (show?.signal?.isNotEmpty() ?: false) {
            show?.signal?.get(index)?.let { signal = it }
        }
        println("Signal: ${signal}")
        markDirtyNotifyAll()
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)

        // Save other
        tag.putBoolean("playing", playing)
        tag.putDouble("seek", seek)
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)

        // Load other
        playing = tag.getBooleanOrNull("playing") ?: false
        seek = tag.getDoubleOrNull("seek") ?: 0.0
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = CompoundTag()
        saveAdditional(tag, registries)
        return tag
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket =
        ClientboundBlockEntityDataPacket.create(this) { _, registries ->
            val tag = CompoundTag()
            tag.putInt("signal", signal)
            saveAdditional(tag, registries)
            tag
        }
}