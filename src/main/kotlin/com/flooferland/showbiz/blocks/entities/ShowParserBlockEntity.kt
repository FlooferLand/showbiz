package com.flooferland.showbiz.blocks.entities

import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import com.flooferland.showbiz.menus.ShowParserMenu
import com.flooferland.showbiz.network.packets.ShowParserDataPacket
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.show.BitId
import com.flooferland.showbiz.show.toBitId
import com.flooferland.showbiz.show.toBitIdArray
import com.flooferland.showbiz.types.connection.ConnectionManager
import com.flooferland.showbiz.types.connection.IConnectable
import com.flooferland.showbiz.types.connection.PortDirection
import com.flooferland.showbiz.types.connection.data.PackedShowData
import com.flooferland.showbiz.utils.Extensions.getIntArrayOrNull
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory

class ShowParserBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.ShowParser.entity!!, pos, blockState), IConnectable, ExtendedScreenHandlerFactory<ShowParserDataPacket> {
    override val connectionManager = ConnectionManager(this)
    val show = connectionManager.port("show", PackedShowData(), PortDirection.In) {
        val level = level as? ServerLevel ?: return@port
        level.updateNeighborsAt(blockPos, blockState.block)
    }

    var bitFilter = mutableListOf<BitId>()
    override fun getDisplayName() = Component.literal("Show Parser")!!
    override fun createMenu(i: Int, inventory: Inventory, player: Player): AbstractContainerMenu? {
        val player = player as? ServerPlayer ?: return null
        return ShowParserMenu(i, getScreenOpeningData(player))
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider?) {
        bitFilter = (tag.getIntArrayOrNull("bit_filter") ?: intArrayOf()).map { it.toShort() }.toMutableList()
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider?) {
        tag.putIntArray("bit_filter", bitFilter.map { it.toInt() })
    }

    override fun getScreenOpeningData(player: ServerPlayer) =
        ShowParserDataPacket(worldPosition, bitFilter, show.data.mapping)
}