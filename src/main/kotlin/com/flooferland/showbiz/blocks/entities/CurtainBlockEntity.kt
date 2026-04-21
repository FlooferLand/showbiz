package com.flooferland.showbiz.blocks.entities

import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.network.protocol.game.*
import net.minecraft.server.level.*
import net.minecraft.sounds.*
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.phys.AABB
import com.flooferland.showbiz.blocks.CurtainBlock
import com.flooferland.showbiz.blocks.CurtainShadowBlock
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.types.connection.ConnectionManager
import com.flooferland.showbiz.types.connection.IConnectable
import com.flooferland.showbiz.types.connection.PortDirection
import com.flooferland.showbiz.types.connection.data.PackedControlData
import com.flooferland.showbiz.utils.Extensions.applyChange
import com.flooferland.showbiz.utils.Extensions.getBooleanOrNull
import com.flooferland.showbiz.utils.Extensions.getFloatOrNull
import com.flooferland.showbiz.utils.Extensions.getIntOrNull
import com.flooferland.showbiz.utils.lerp

class CurtainBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.CurtainBlock.entityType!!, pos, blockState), IConnectable {
    override val connectionManager = ConnectionManager(this)

    val control = connectionManager.port("control", PackedControlData(), PortDirection.In) {
        val open = it.readCurtain() ?: return@port
        if (open != isOpen) applyChange(true) { setCurtains(open) }
    }

    var color: Int = 0xffffff
    var openAmount: Float = 0f
    var isOpen: Boolean = true
        public get
        private set

    var connectedCurtains = mutableSetOf<BlockPos>()
    var centerCurtain: BlockPos? = null

    fun closeCurtains() = setCurtains(false)
    fun openCurtains() = setCurtains(true)
    fun setCurtains(isOpen: Boolean) {
        val level = level as? ServerLevel ?: return
        applyChange(true) {
            this.isOpen = isOpen
        }

        // Searching for other curtain rails
        val rails = findConnectedCurtains()
        rails.forEach { pos ->
            val blockEntity = level.getBlockEntity(pos) as? CurtainBlockEntity ?: return@forEach
            blockEntity.applyChange(true) {
                blockEntity.isOpen = isOpen
                for (y in 1..blockEntity.findLength()) {
                    val pos = blockEntity.blockPos.offset(0, -y, 0)
                    val state = level.getBlockState(pos) ?: Blocks.AIR.defaultBlockState()
                    if (isOpen) {  // Should open
                        if (state.block is CurtainShadowBlock)
                            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState())
                    } else {  // Should close
                        if (state.isAir)
                            level.setBlockAndUpdate(pos, ModBlocks.CurtainBlockShadow.block.defaultBlockState())
                    }
                }
            }
        }

        // Finding the center curtain
        val center = findCenter(rails) ?: blockPos

        // Sounds
        if (level is ServerLevel)
            level.playSound(null, center, SoundEvents.WOOL_HIT, SoundSource.BLOCKS, 1f, 1f)
    }

    fun findConnectedCurtains(): Set<BlockPos> {
        val level = level ?: return emptySet()
        val connected = mutableSetOf<BlockPos>()
        val queue = ArrayDeque<BlockPos>()
        queue.add(blockPos)

        var callCounter = 0
        while (queue.isNotEmpty() && callCounter < 30) {
            callCounter += 1
            val current = queue.removeFirstOrNull() ?: continue
            if (!connected.add(current)) continue
            for (x in -1..1) {
                for (z in -1..1) {
                    val pos = current.offset(x, 0, z)
                    if (level.getBlockState(pos).block is CurtainBlock && !connected.contains(pos)) {
                        queue.add(pos)
                    }
                }
            }
        }

        connectedCurtains.clear()
        connectedCurtains.addAll(connected)
        return connected
    }

    fun findCenter(rails: Set<BlockPos>) = rails.minByOrNull { pos ->
        rails.sumOf { otherPos -> pos.distSqr(otherPos) }
    }.also { centerCurtain = it }

    fun findLength(): Int {
        var isLast = false
        var length = 0
        for (y in 0..MAX_LENGTH) {
            length = y
            for (y2 in 1..2) {
                val below = blockPos.below(y + y2)
                if ((level?.getBlockState(below)?.isSolidRender(level!!, below) ?: true)) {
                    isLast = true
                    break
                }
            }
            if (isLast) break
        }
        return length
    }

    fun tick() {
        if (level?.isClientSide == true) {
            // Caches stuff for rendering
            findCenter(findConnectedCurtains())
            return
        }

        if (isOpen && openAmount < 1.0f) {
            applyChange(true) { openAmount += 0.01f }
        } else if (!isOpen && openAmount > 0.0f) {
            applyChange(true) { openAmount -= 0.01f }
        }
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        connectionManager.load(tag)
        tag.getFloatOrNull("open_amount")?.let { openAmount = it }
        tag.getBooleanOrNull("is_open")?.let { isOpen = it }
        tag.getIntOrNull("color")?.let { color = it }
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        connectionManager.save(tag)
        tag.putFloat("open_amount", openAmount)
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