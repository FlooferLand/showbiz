package com.flooferland.showbiz.blocks.entities

import net.minecraft.core.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.types.connection.ConnectionManager
import com.flooferland.showbiz.types.connection.IConnectable
import com.flooferland.showbiz.types.connection.PortDirection
import com.flooferland.showbiz.types.connection.data.PackedShowData
import com.flooferland.showbiz.utils.Extensions.applyChange
import com.flooferland.showbiz.utils.Extensions.getBooleanOrNull

class CurtainBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.CurtainBlock.entityType!!, pos, blockState), IConnectable {
    override val connectionManager = ConnectionManager(this)
    val show = connectionManager.port("show", PackedShowData(), PortDirection.Both) {
        // TODO: Add a nicer way of chosing the bit for the curtains
        val shouldOpen = when (it.mapping) {
            "rae" -> it.signal.frameHas(91)  // Stage center
            "faz" -> it.signal.frameHas(89)  // Stage main
            else -> false
        }
        val shouldClose = when (it.mapping) {
            "rae" -> it.signal.frameHas(92)  // Stage center
            "faz" -> it.signal.frameHas(90)  // Stage main
            else -> false
        }

        if ((shouldOpen && !isOpen) || (shouldClose && isOpen)) {
            applyChange(true) { isOpen = shouldOpen && !shouldClose }
        }
    }

    var isOpen: Boolean = true

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        tag.getBooleanOrNull("is_open")?.let { isOpen = it }
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        tag.putBoolean("is_open", isOpen)
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = super.getUpdateTag(registries)
        saveAdditional(tag, registries)
        return tag
    }

    override fun getUpdatePacket() = ClientboundBlockEntityDataPacket.create(this)!!
}