package com.flooferland.showbiz.blocks.entities

import com.flooferland.showbiz.registry.ModBlocks
import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.network.protocol.game.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*

class GreyboxBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.Greybox.entity!!, pos, blockState) {
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag? {
        return super.getUpdateTag(registries)
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket =
        ClientboundBlockEntityDataPacket.create(this) { _, registries ->
            val tag = CompoundTag()
            saveAdditional(tag, registries)
            tag
        }
}