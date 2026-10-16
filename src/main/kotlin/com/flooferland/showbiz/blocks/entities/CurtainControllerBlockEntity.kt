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
import com.flooferland.showbiz.menus.CurtainControllerEditMenu
import com.flooferland.showbiz.network.packets.editscreen.CurtainControllerEditPacket
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.types.EditScreenMenu
import com.flooferland.showbiz.types.EditScreenOwner
import com.flooferland.showbiz.types.MappedBits
import com.flooferland.showbiz.types.OwnerId
import com.flooferland.showbiz.types.connection.ConnectionManager
import com.flooferland.showbiz.types.connection.IConnectable
import com.flooferland.showbiz.types.connection.PortDirection
import com.flooferland.showbiz.types.connection.data.PackedControlData
import com.flooferland.showbiz.types.connection.data.PackedShowData

class CurtainControllerBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.CurtainController.entityType!!, pos, blockState), IConnectable, EditScreenOwner<CurtainControllerEditPacket> {
    override val connectionManager = ConnectionManager(this)

    val control = connectionManager.port("control", PackedControlData(), PortDirection.Out)
    val show = connectionManager.port("show", PackedShowData(), PortDirection.In) { data ->
        val mapping = data.mapping ?: return@port
        if (!bitFilterOpen.containsKey(mapping) && !bitFilterClose.containsKey(mapping)) return@port

        val hasCloseBit = bitFilterClose[mapping]?.any() { data.signal.frameHas(it) } ?: false
        val hasOpenBit = bitFilterOpen[mapping]?.any() { data.signal.frameHas(it) } ?: false
        if (hasCloseBit || hasOpenBit) {
            control.data.writeCurtain(hasOpenBit && !hasCloseBit)
            control.send()
            control.data.clearCurtain()
        }
    }

    override var menuData = EditScreenMenu.EditScreenBuf(OwnerId.of(pos))
    var bitFilterOpen: MappedBits = MappedBits()
    var bitFilterClose: MappedBits = MappedBits()

    override fun getDisplayName() = Component.literal("Curtain Controller")!!
    override fun createMenu(i: Int, inventory: Inventory, player: Player): AbstractContainerMenu? {
        val player = player as? ServerPlayer ?: return null
        return CurtainControllerEditMenu(i, getScreenOpeningData(player))
    }
    override fun getScreenOpeningData(player: ServerPlayer) =
        CurtainControllerEditPacket(EditScreenMenu.EditScreenBuf(OwnerId.of(worldPosition)), bitFilterOpen, bitFilterClose)

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        connectionManager.load(tag)
        bitFilterOpen.load(tag, "bit_filter_open")
        bitFilterClose.load(tag, "bit_filter_close")
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        connectionManager.save(tag)
        bitFilterOpen.save(tag, "bit_filter_open")
        bitFilterClose.save(tag, "bit_filter_close")
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = super.getUpdateTag(registries)
        saveAdditional(tag, registries)
        return tag
    }

    override fun getUpdatePacket() = ClientboundBlockEntityDataPacket.create(this)!!
}