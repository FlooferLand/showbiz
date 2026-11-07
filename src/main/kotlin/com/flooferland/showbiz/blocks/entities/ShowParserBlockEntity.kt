package com.flooferland.showbiz.blocks.entities

import com.flooferland.showbiz.menus.ShowParserEditMenu
import com.flooferland.showbiz.network.packets.editscreen.ShowParserEditPacket
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.types.*
import com.flooferland.showbiz.types.connection.ConnectionManager
import com.flooferland.showbiz.types.connection.IConnectable
import com.flooferland.showbiz.types.connection.PortDirection
import com.flooferland.showbiz.types.connection.data.PackedShowData
import com.flooferland.showbiz.utils.Extensions.getBooleanOrNull
import com.flooferland.showbiz.utils.Extensions.getEnumOrNull
import com.flooferland.showbiz.utils.Extensions.putEnum
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DiodeBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class ShowParserBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.ShowParser.entityType!!, pos, blockState), IConnectable, EditScreenOwner<ShowParserEditPacket> {
    override val connectionManager = ConnectionManager(this)
    val show = connectionManager.port("show", PackedShowData(), PortDirection.In) {
        val level = level as? ServerLevel ?: return@port
        level.updateNeighborsAt(blockPos, blockState.block)
    }

    override var menuData = EditScreenMenu.EditScreenBuf(OwnerId.of(blockPos))

    enum class Mode { Bits, Playing }

    val bitFilter = MappedBits()
    var mode = Mode.Bits
    var invert = false

    // From 0.5.x to 0.6.0
    var migrateFacing = false

    override fun getDisplayName() = Component.literal("Show Parser")!!
    override fun createMenu(i: Int, inventory: Inventory, player: Player): AbstractContainerMenu? {
        val player = player as? ServerPlayer ?: return null
        return ShowParserEditMenu(i, getScreenOpeningData(player))
    }
    override fun getScreenOpeningData(player: ServerPlayer) =
        ShowParserEditPacket(EditScreenMenu.EditScreenBuf(OwnerId.of(worldPosition)), bitFilter, mode, invert)

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        migrateFacing = !tag.contains("mode") && !tag.contains("invert") && !tag.contains("data_version")
        connectionManager.load(tag)
        bitFilter.load(tag, "bit_filter")
        tag.getEnumOrNull<Mode>("mode")?.let { mode = it }
        tag.getBooleanOrNull("invert")?.let { invert = it }
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        connectionManager.save(tag)
        bitFilter.save(tag, "bit_filter")
        tag.putEnum("mode", mode)
        tag.putBoolean("invert", invert)
    }

    override fun setLevel(level: Level) {
        super.setLevel(level)
        if (migrateFacing && level is ServerLevel) {
            VersionMigrator.queuedMigrations.add {
                if (isRemoved) return@add
                val state = blockState
                level.setBlock(
                    blockPos,
                    state.setValue(DiodeBlock.FACING, state.getValue(DiodeBlock.FACING).opposite),
                    Block.UPDATE_ALL
                )
                setChanged()
            }
            migrateFacing = false
        }
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag? {
        val tag = super.getUpdateTag(registries)
        saveAdditional(tag, registries)
        return tag
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket =
        ClientboundBlockEntityDataPacket.create(this)
}