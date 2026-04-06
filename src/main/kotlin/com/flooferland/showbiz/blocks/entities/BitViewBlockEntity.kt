package com.flooferland.showbiz.blocks.entities

import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.*
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import com.flooferland.showbiz.menus.BitViewMenu
import com.flooferland.showbiz.menus.BotSelectMenu
import com.flooferland.showbiz.network.packets.BitViewPacket
import com.flooferland.showbiz.network.packets.CurtainControllerEditPacket
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.types.EditScreenOwner
import com.flooferland.showbiz.types.connection.ConnectionManager
import com.flooferland.showbiz.types.connection.IConnectable
import com.flooferland.showbiz.types.connection.PortDirection
import com.flooferland.showbiz.types.connection.data.PackedShowData
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory

class BitViewBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.BitView.entityType!!, pos, blockState), IConnectable, ExtendedScreenHandlerFactory<BitViewPacket> {
    override val connectionManager = ConnectionManager(this)
    val show = connectionManager.port("show", PackedShowData(), PortDirection.In)

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

    override fun getDisplayName() = Component.literal("Bit View")!!
    override fun getScreenOpeningData(player: ServerPlayer) = BitViewPacket(worldPosition)
    override fun createMenu(i: Int, inventory: Inventory, player: Player): AbstractContainerMenu? {
        val player = player as? ServerPlayer ?: return null
        return BitViewMenu(i, getScreenOpeningData(player))
    }
}