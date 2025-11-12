package com.flooferland.showbiz.blocks.entities

import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.show.SignalFrame
import com.flooferland.showbiz.types.connection.ConnectionManager
import com.flooferland.showbiz.types.connection.DataChannelIn
import com.flooferland.showbiz.types.connection.DataChannelOut
import com.flooferland.showbiz.types.connection.IConnectable
import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.network.protocol.game.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*

class GreyboxBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.Greybox.entity!!, pos, blockState), IConnectable {
    object PlayingOut : DataChannelOut<Boolean>("playing")
    object PlayingIn : DataChannelIn<Boolean>("playing")
    object SignalOut : DataChannelOut<SignalFrame>("signal")
    object SignalIn : DataChannelIn<SignalFrame>("signal")
    override val connectionManager = ConnectionManager(this) {
        bind(PlayingOut)
        bind(PlayingIn) { data ->
            Showbiz.log.debug("Received playing '{}' on in:${PlayingIn.id}", data)
            it.send(PlayingOut, data)
        }

        bind(SignalOut)
        bind(SignalIn) { data ->
            it.send(SignalOut, data)
        }
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        connectionManager.save(tag)
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        connectionManager.load(tag)
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