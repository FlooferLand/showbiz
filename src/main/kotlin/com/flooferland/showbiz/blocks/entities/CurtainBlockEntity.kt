package com.flooferland.showbiz.blocks.entities

import net.minecraft.core.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import com.flooferland.showbiz.blocks.CurtainBlock
import com.flooferland.showbiz.blocks.CurtainShadowBlock
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.types.connection.ConnectionManager
import com.flooferland.showbiz.types.connection.IConnectable
import com.flooferland.showbiz.types.connection.PortDirection
import com.flooferland.showbiz.types.connection.data.PackedShowData
import com.flooferland.showbiz.utils.Extensions.applyChange
import com.flooferland.showbiz.utils.Extensions.getBooleanOrNull
import com.flooferland.showbiz.utils.Extensions.getIntOrNull

class CurtainBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.CurtainBlock.entityType!!, pos, blockState), IConnectable {
    override val connectionManager = ConnectionManager(this)

    val show = connectionManager.port("show", PackedShowData(), PortDirection.In) {
        // TODO: Add a nicer way of chosing the bit for the curtains
        val shouldOpen = when (it.mapping) {
            "rae" -> it.signal.frameHas(91)  // Stage center
            "faz" -> it.signal.frameHas(89)  // Stage main
            else -> false
        }
        val shouldClose = when (it.mapping) {
            "rae" -> it.signal.frameHas(92)  // Stage center
            "faz" -> it.signal.frameHas(90)  // Stage main
            else -> false
        }

        if (shouldOpen || shouldClose) {
            applyChange(true) { setCurtains(shouldOpen && !shouldClose) }
        }
    }

    var color: Int = 0xffffff
    var isOpen: Boolean = true
        public get
        private set

    fun closeCurtains() = setCurtains(false)
    fun openCurtains() = setCurtains(true)
    fun setCurtains(isOpen: Boolean, alreadyEdited: MutableSet<BlockPos> = mutableSetOf(), callCounter: Int = 0) {
        val level = level ?: return
        this.isOpen = isOpen
        val callCounter = callCounter + 1
        if (callCounter > 20) return

        // Sounds
        if (callCounter == 1) {
            if (level is ServerLevel)
                level.playSound(null, blockPos, SoundEvents.WOOL_HIT, SoundSource.BLOCKS, 1f, 1f)
        }

        // Filling blocks
        for (y in 1..findLength()) {
            val pos = blockPos.offset(0, -y, 0)
            val state = level.getBlockState(pos) ?: Blocks.AIR.defaultBlockState()
            if (isOpen) {  // Should open
                if (state.block is CurtainShadowBlock)
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState())
            } else {  // Should close
                if (state.isAir)
                    level.setBlockAndUpdate(pos, ModBlocks.CurtainBlockShadow.block.defaultBlockState())
            }
        }

        // Searching for other curtain rails
        for (x in -1..1) {
            for (z in -1..1) {
                val pos = blockPos.offset(x, 0, z)
                if (level.getBlockState(pos).block !is CurtainBlock) continue
                val blockEntity = level.getBlockEntity(pos) as? CurtainBlockEntity ?: continue
                if (alreadyEdited.contains(pos)) {
                    continue
                } else {
                    alreadyEdited.add(pos)
                }
                blockEntity.applyChange(true) {
                    setCurtains(isOpen, alreadyEdited, callCounter)
                }
            }
        }
    }

    fun findLength(): Int {
        var isLast = false
        var length = 0
        for (y in 0..MAX_LENGTH) {
            length = y
            for (y2 in 1..2) {
                val below = blockPos.below(y + y2)
                if ((level?.getBlockState(below)?.isSolidRender(level, below) ?: true)) {
                    isLast = true
                    break
                }
            }
            if (isLast) break
        }
        return length
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        tag.getBooleanOrNull("is_open")?.let { isOpen = it }
        tag.getIntOrNull("color")?.let { color = it }
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        tag.putBoolean("is_open", isOpen)
        tag.putInt("color", color)
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = super.getUpdateTag(registries)
        saveAdditional(tag, registries)
        return tag
    }

    override fun getUpdatePacket() = ClientboundBlockEntityDataPacket.create(this)!!

    companion object {
        const val MAX_LENGTH: Int = 6
    }
}