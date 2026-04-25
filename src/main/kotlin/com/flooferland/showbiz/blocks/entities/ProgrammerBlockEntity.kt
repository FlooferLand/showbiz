package com.flooferland.showbiz.blocks.entities

import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.show.SignalFrame
import com.flooferland.showbiz.show.toBitIdArray
import com.flooferland.showbiz.types.connection.ConnectionManager
import com.flooferland.showbiz.types.connection.IConnectable
import com.flooferland.showbiz.types.connection.PortDirection
import com.flooferland.showbiz.types.connection.data.PackedShowData
import com.flooferland.showbiz.types.entity.PlayerProgrammingData
import com.flooferland.showbiz.utils.Extensions.getIntArrayOrNull
import kotlin.math.roundToInt
import kotlin.math.sqrt

class ProgrammerBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.Programmer.entityType!!, pos, blockState), IConnectable {
    override val connectionManager = ConnectionManager(this)
    val show = connectionManager.port("show", PackedShowData(), PortDirection.Both)

    val operators = mutableListOf<Player>()

    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        // Cleaning up operators
        operators.removeIf {
            val distSqr = it.distanceToSqr(pos.center)
            val shouldRemove = it.isRemoved || distSqr > playerMaxDistSqr
            if (shouldRemove) {
                val data = PlayerProgrammingData.getFromPlayer(it)
                data.active = false
                data.blockPos = null
                data.saveToPlayer(it)
                it.displayClientMessage(Component.literal("Exited programming mode"), true)
            } else if (distSqr > (playerMaxDistSqr / 2)) {
                it.displayClientMessage(Component.literal("Too far from the programmer! (${sqrt(distSqr).roundToInt() - sqrt(playerMaxDistSqr / 2).toInt() + 1} blocks too far)"), true)
            }
            shouldRemove
        }

        // Show recording
        if (operators.isEmpty()) return
        if (show.data.mapping.isNullOrEmpty()) show.data.mapping = "rae"
        show.data.playing = true
        show.data.signal.reset()
        for (player in operators) {
            val data = PlayerProgrammingData.getFromPlayer(player)
            val heldBits = data.heldKeys
                .mapIndexed { i, held -> if (held) data.mapKeyToBit(i).toUShort() else null }
                .filterNotNull()
            show.data.signal.raw += heldBits
        }
        show.send()
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        connectionManager.save(tag)
        tag.putIntArray("operators", operators.map { it.id })
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        connectionManager.load(tag)
        (tag.getIntArrayOrNull("operators") ?: intArrayOf()).let { ids ->
            operators.clear()
            ids.forEach { id -> (level?.getEntity(id) as? Player)?.let { operators.add(it) } }
        }
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag? {
        val tag = super.getUpdateTag(registries)
        saveAdditional(tag, registries)
        return tag
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket =
        ClientboundBlockEntityDataPacket.create(this)

    companion object {
        val playerMaxDistSqr = 16.0f.let { it * it }
    }
}