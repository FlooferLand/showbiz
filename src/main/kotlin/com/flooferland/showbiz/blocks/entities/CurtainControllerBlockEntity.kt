package com.flooferland.showbiz.blocks.entities

import com.flooferland.showbiz.menus.CurtainControllerEditMenu
import com.flooferland.showbiz.network.packets.editscreen.CurtainControllerEditPacket
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.types.*
import com.flooferland.showbiz.types.connection.ConnectionManager
import com.flooferland.showbiz.types.connection.IConnectable
import com.flooferland.showbiz.types.connection.PortDirection
import com.flooferland.showbiz.types.connection.data.PackedControlData
import com.flooferland.showbiz.types.connection.data.PackedShowData
import com.flooferland.showbiz.utils.Extensions.getBooleanOrNull
import com.flooferland.showbiz.utils.Extensions.getEnumOrNull
import com.flooferland.showbiz.utils.Extensions.getFloatOrNull
import com.flooferland.showbiz.utils.Extensions.getIntOrNull
import com.flooferland.showbiz.utils.Extensions.putEnum
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.redstone.Redstone

class CurtainControllerBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.CurtainController.entityType!!, pos, blockState), IConnectable, EditScreenOwner<CurtainControllerEditPacket> {
    override val connectionManager = ConnectionManager(this)

    val control = connectionManager.port("control", PackedControlData(), PortDirection.Out)
    val show = connectionManager.port("show", PackedShowData(), PortDirection.In) { data ->
        val mapping = data.mapping ?: return@port
        if (!bitFilterOpen.containsKey(mapping) && !bitFilterClose.containsKey(mapping)) return@port

        if (serverSignal > Redstone.SIGNAL_NONE) return@port
        if (serverLastPlaying != data.playing && defaultState != CurtainState.None) {
            serverLastPlaying = data.playing
            if (!data.playing) {
                sendControl(defaultState == CurtainState.Open)
            }
        } else {
            val hasCloseBit = bitFilterClose[mapping]?.any() { data.signal.frameHas(it) } ?: false
            val hasOpenBit = bitFilterOpen[mapping]?.any() { data.signal.frameHas(it) } ?: false
            if (hasCloseBit || hasOpenBit) {
                sendControl(hasOpenBit && !hasCloseBit)
            }
        }
    }

    override var menuData = EditScreenMenu.EditScreenBuf(OwnerId.of(pos))
    var bitFilterOpen: MappedBits = MappedBits()
    var bitFilterClose: MappedBits = MappedBits()
    var defaultState: CurtainState = CurtainState.None
    var speed: Float = CurtainBlockEntity.DEFAULT_BLOCK_SPEED

    var shouldOpen = false
    var serverSpeedDirty = false
    var serverSignal = Redstone.SIGNAL_NONE
    var serverLastPlaying = false

    override fun getDisplayName() = Component.literal("Curtain Controller")!!
    override fun createMenu(i: Int, inventory: Inventory, player: Player): AbstractContainerMenu? {
        val player = player as? ServerPlayer ?: return null
        return CurtainControllerEditMenu(i, getScreenOpeningData(player))
    }
    override fun getScreenOpeningData(player: ServerPlayer) =
        CurtainControllerEditPacket(EditScreenMenu.EditScreenBuf(OwnerId.of(worldPosition)), bitFilterOpen, bitFilterClose, defaultState, speed)

    fun tick() {
        if (serverSpeedDirty) {
            sendControl(null)
            serverSpeedDirty = false
        }
    }

    fun sendControl(open: Boolean?) {
        shouldOpen = open ?: shouldOpen
        control.data.writeCurtain(open, speed)
        control.send()
        control.data.clearCurtain()
        serverSpeedDirty = false
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        connectionManager.load(tag)
        bitFilterOpen.load(tag, "bit_filter_open")
        bitFilterClose.load(tag, "bit_filter_close")
        tag.getEnumOrNull<CurtainState>("default_state")?.let { defaultState = it }
        tag.getBooleanOrNull("should_open")?.let { shouldOpen = it }
        tag.getFloatOrNull("speed")?.let { speed = it }

        if (level?.isClientSide == true) {
            tag.getIntOrNull("redstone_signal")?.let { serverSignal = it }
        }
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        connectionManager.save(tag)
        bitFilterOpen.save(tag, "bit_filter_open")
        bitFilterClose.save(tag, "bit_filter_close")
        tag.putEnum("default_state", defaultState)
        tag.putBoolean("should_open", shouldOpen)
        tag.putFloat("speed", speed)
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = super.getUpdateTag(registries)
        saveAdditional(tag, registries)
        tag.putInt("redstone_signal", serverSignal)
        return tag
    }

    override fun getUpdatePacket() = ClientboundBlockEntityDataPacket.create(this)!!
}