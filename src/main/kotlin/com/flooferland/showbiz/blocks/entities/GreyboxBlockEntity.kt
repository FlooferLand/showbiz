package com.flooferland.showbiz.blocks.entities

import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.utils.Extensions.getIntArrayOrNull
import com.flooferland.showbiz.utils.Extensions.getOrNull
import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.network.protocol.game.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*

class GreyboxBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.Greybox.entity!!, pos, blockState) {
    var reelToReelPos: BlockPos? = null
    val connected = mutableListOf<BlockPos>()

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        reelToReelPos?.let { tag.putIntArray("Reel-To-Reel", intArrayOf(it.x, it.y, it.z)) }
        tag.put("Connected", CompoundTag().also {
            for ((i, pos) in connected.withIndex()) {
                it.putIntArray("$i", intArrayOf(pos.x, pos.y, pos.z))
            }
        })
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        reelToReelPos = tag.getIntArrayOrNull("Reel-To-Reel")?.let {
            if (it.size < 3) return@let null
            BlockPos(it[0], it[1], it[2])
        }

        connected.clear()
        tag.getOrNull("Connected")?.let {
            val tag = (it as CompoundTag)
            for (i in 0..tag.size()) {
                val arr = tag.getIntArrayOrNull("$i") ?: continue
                if (arr.size < 3) continue
                connected.add(BlockPos(arr[0], arr[1], arr[2]))
            }
        }
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = CompoundTag()
        saveAdditional(tag, registries)
        return tag
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket =
        ClientboundBlockEntityDataPacket.create(this) { _, registries ->
            val tag = CompoundTag()
            saveAdditional(tag, registries)
            tag
        }
}