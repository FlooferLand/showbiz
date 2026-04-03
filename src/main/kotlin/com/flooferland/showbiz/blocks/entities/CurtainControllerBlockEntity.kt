package com.flooferland.showbiz.blocks.entities

import net.minecraft.core.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.*
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.inventory.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import com.flooferland.showbiz.menus.CurtainControllerEditMenu
import com.flooferland.showbiz.network.packets.CurtainControllerEditPacket
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.show.BitId
import com.flooferland.showbiz.show.toBitId
import com.flooferland.showbiz.types.EditScreenMenu
import com.flooferland.showbiz.types.EditScreenOwner
import com.flooferland.showbiz.types.connection.ConnectionManager
import com.flooferland.showbiz.types.connection.IConnectable
import com.flooferland.showbiz.types.connection.PortDirection
import com.flooferland.showbiz.types.connection.data.PackedControlData
import com.flooferland.showbiz.types.connection.data.PackedShowData
import com.flooferland.showbiz.utils.Extensions.getIntArrayOrNull

class CurtainControllerBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.CurtainControllerBlock.entityType!!, pos, blockState), IConnectable, EditScreenOwner<CurtainControllerEditPacket> {
    override val connectionManager = ConnectionManager(this)

    val control = connectionManager.port("control", PackedControlData(), PortDirection.Out)
    val show = connectionManager.port("show", PackedShowData(), PortDirection.In) { data ->
        val hasCloseBit = bitFilterClose.any() { data.signal.frameHas(it) }
        val hasOpenBit = bitFilterOpen.any() { data.signal.frameHas(it) }
        if (hasCloseBit || hasOpenBit) {
            control.data.writeCurtain(hasOpenBit && !hasCloseBit)
            control.send()
            control.data.clearCurtain()
        }
    }

    override var menuData = EditScreenMenu.EditScreenBuf(blockPos)
    var bitFilterOpen: MutableList<BitId> = mutableListOf()
    var bitFilterClose: MutableList<BitId> = mutableListOf()

    override fun getDisplayName() = Component.literal("Curtain Controller")!!
    override fun createMenu(i: Int, inventory: Inventory, player: Player): AbstractContainerMenu? {
        val player = player as? ServerPlayer ?: return null
        return CurtainControllerEditMenu(i, getScreenOpeningData(player))
    }
    override fun getScreenOpeningData(player: ServerPlayer) =
        CurtainControllerEditPacket(EditScreenMenu.EditScreenBuf(worldPosition, menuData.bitFilter, show.data.mapping), bitFilterOpen, bitFilterClose)

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        connectionManager.load(tag)
        menuData.loadAdditional(tag)
        bitFilterOpen = (tag.getIntArrayOrNull("bit_filter_open") ?: intArrayOf()).map { it.toBitId() }.toMutableList()
        bitFilterClose = (tag.getIntArrayOrNull("bit_filter_close") ?: intArrayOf()).map { it.toBitId() }.toMutableList()
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        connectionManager.save(tag)
        menuData.saveAdditional(tag)
        tag.putIntArray("bit_filter_open", bitFilterOpen.map { it.toInt() })
        tag.putIntArray("bit_filter_close", bitFilterClose.map { it.toInt() })
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = super.getUpdateTag(registries)
        saveAdditional(tag, registries)
        return tag
    }

    override fun getUpdatePacket() = ClientboundBlockEntityDataPacket.create(this)!!
}