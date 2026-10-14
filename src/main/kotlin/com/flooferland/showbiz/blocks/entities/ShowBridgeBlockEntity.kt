package com.flooferland.showbiz.blocks.entities

import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.network.chat.*
import net.minecraft.network.protocol.game.*
import net.minecraft.server.level.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.inventory.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import com.flooferland.showbiz.menus.ShowBridgeEditMenu
import com.flooferland.showbiz.network.packets.editscreen.ShowBridgeEditPacket
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.types.EditScreenMenu
import com.flooferland.showbiz.types.EditScreenOwner
import com.flooferland.showbiz.types.OwnerId
import com.flooferland.showbiz.types.connection.ConnectionManager
import com.flooferland.showbiz.types.connection.IConnectable
import com.flooferland.showbiz.types.connection.PortDirection
import com.flooferland.showbiz.types.connection.data.PackedShowData

class ShowBridgeBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.ShowBridge.entityType!!, pos, blockState), IConnectable, EditScreenOwner<ShowBridgeEditPacket> {
    override val connectionManager = ConnectionManager(this)
    val show = connectionManager.port("show", PackedShowData(), PortDirection.In)

    override var menuData = EditScreenMenu.EditScreenBuf(OwnerId.of(blockPos))
    override fun getDisplayName() = Component.literal("Show Bridge")!!
    override fun createMenu(i: Int, inventory: Inventory, player: Player): AbstractContainerMenu? {
        val player = player as? ServerPlayer ?: return null
        return ShowBridgeEditMenu(i, getScreenOpeningData(player))
    }
    override fun getScreenOpeningData(player: ServerPlayer) =
        ShowBridgeEditPacket(EditScreenMenu.EditScreenBuf(OwnerId.of(worldPosition)))

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
}