package com.flooferland.showbiz.blocks.entities

import com.flooferland.showbiz.registry.ModBlocks
import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*

class PlaybackControllerBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.PlaybackController.entity!!, pos, blockState) {
    var boundBot: BlockPos? = null

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        val pos = boundBot
        val data = if (pos != null) intArrayOf(pos.x, pos.y, pos.z) else intArrayOf()
        tag.putIntArray("boundBot", data)
        super.saveAdditional(tag, registries)
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        val data = tag.getIntArray("boundBot")
        if (data.isNotEmpty() && data.size == 3) {
            boundBot = BlockPos(data[0], data[1], data[2])
        } else {
            boundBot = null
        }
        super.loadAdditional(tag, registries)
    }
}