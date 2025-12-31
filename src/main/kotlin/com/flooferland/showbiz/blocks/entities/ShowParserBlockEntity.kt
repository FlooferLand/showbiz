package com.flooferland.showbiz.blocks.entities

import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import com.flooferland.showbiz.blocks.ShowParserBlock.Companion.PLAYING_POWERED
import com.flooferland.showbiz.blocks.ShowParserBlock.Companion.SIGNAL_POWERED
import com.flooferland.showbiz.blocks.base.FacingEntityBlock
import com.flooferland.showbiz.menus.ShowParserMenu
import com.flooferland.showbiz.network.packets.ShowParserDataPacket
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.show.BitId
import com.flooferland.showbiz.types.connection.ConnectionManager
import com.flooferland.showbiz.types.connection.IConnectable
import com.flooferland.showbiz.types.connection.PortDirection
import com.flooferland.showbiz.types.connection.data.PackedShowData
import com.flooferland.showbiz.utils.Extensions.getIntArrayOrNull
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory

class ShowParserBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.ShowParser.entityType!!, pos, blockState), IConnectable, ExtendedScreenHandlerFactory<ShowParserDataPacket> {
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

    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        val level = level as? ServerLevel ?: return
        if (bitFilter.isEmpty()) {
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
                     .setValue(SIGNAL_POWERED, show.data.signal.raw.any { bitId -> bitFilter.contains(bitId) })
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

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider?) {
        connectionManager.load(tag)
        bitFilter = (tag.getIntArrayOrNull("bit_filter") ?: intArrayOf()).map { it.toShort() }.toMutableList()
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        connectionManager.save(tag)
        tag.putIntArray("bit_filter", bitFilter.map { it.toInt() })
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag? {
        val tag = super.getUpdateTag(registries)
        saveAdditional(tag, registries)
        return tag
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket =
        ClientboundBlockEntityDataPacket.create(this)

    override fun getScreenOpeningData(player: ServerPlayer) =
        ShowParserDataPacket(worldPosition, bitFilter, show.data.mapping)
}