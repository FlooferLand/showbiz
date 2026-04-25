package com.flooferland.showbiz.blocks.entities

import net.minecraft.core.*
import net.minecraft.core.particles.*
import net.minecraft.nbt.*
import net.minecraft.network.chat.*
import net.minecraft.network.protocol.game.*
import net.minecraft.server.level.*
import net.minecraft.sounds.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.inventory.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.blocks.ShowParserBlock.Companion.PLAYING_POWERED
import com.flooferland.showbiz.blocks.ShowParserBlock.Companion.SIGNAL_POWERED
import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.menus.ShowParserEditMenu
import com.flooferland.showbiz.network.packets.ShowParserEditPacket
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.types.EditScreenMenu
import com.flooferland.showbiz.types.EditScreenOwner
import com.flooferland.showbiz.types.connection.ConnectionManager
import com.flooferland.showbiz.types.connection.IConnectable
import com.flooferland.showbiz.types.connection.PortDirection
import com.flooferland.showbiz.types.connection.data.PackedShowData

class ShowParserBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.ShowParser.entityType!!, pos, blockState), IConnectable, EditScreenOwner<ShowParserEditPacket> {
    override val connectionManager = ConnectionManager(this)
    val show = connectionManager.port("show", PackedShowData(), PortDirection.In) {
        val level = level as? ServerLevel ?: return@port
        println(it.signal.raw.contentToString())
        level.updateNeighborsAt(blockPos, blockState.block)
    }

    override var menuData = EditScreenMenu.EditScreenBuf(blockPos)

    override fun getDisplayName() = Component.literal("Show Parser")!!
    override fun createMenu(i: Int, inventory: Inventory, player: Player): AbstractContainerMenu? {
        val player = player as? ServerPlayer ?: return null
        return ShowParserEditMenu(i, getScreenOpeningData(player))
    }
    override fun getScreenOpeningData(player: ServerPlayer) =
        ShowParserEditPacket(EditScreenMenu.EditScreenBuf(worldPosition, menuData.bitFilter, show.data.mapping))

    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        val level = level as? ServerLevel ?: return
        if (menuData.bitFilter.isEmpty()) {
            if (level.gameTime % 25 == 0L) {
                val facing = state.getValue(FacingEntityBlock.FACING)
                val forward = Vec3(facing.normal.x.toDouble(), facing.normal.y.toDouble(), facing.normal.z.toDouble())
                val center = pos.center.add(forward.scale(0.5)).subtract(0.0, 0.1, 0.0)
                level.sendParticles(ParticleTypes.SMOKE, center.x, center.y, center.z, 5, 0.15, 0.15, 0.15, 0.02)
                level.playSound(null, pos, SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.BLOCKS, 0.2f, 1.0f)
            }
            return
        }
        when (show.data.playing) {
             true -> {
                 val newState = state
                     .setValue(PLAYING_POWERED, true)
                     .setValue(SIGNAL_POWERED, show.data.signal.raw.any { bitId -> menuData.bitFilter.contains(bitId) })
                 level.setBlockAndUpdate(blockPos, newState)
             }
            false if (state.getValue(PLAYING_POWERED) || state.getValue(SIGNAL_POWERED)) -> {
                val newState = state
                    .setValue(PLAYING_POWERED, false)
                    .setValue(SIGNAL_POWERED, false)
                level.setBlockAndUpdate(blockPos, newState)
            }
            else -> {}
        }
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        connectionManager.load(tag)
        menuData.loadAdditional(tag)
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        connectionManager.save(tag)
        menuData.saveAdditional(tag)
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag? {
        val tag = super.getUpdateTag(registries)
        saveAdditional(tag, registries)
        return tag
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket =
        ClientboundBlockEntityDataPacket.create(this)
}