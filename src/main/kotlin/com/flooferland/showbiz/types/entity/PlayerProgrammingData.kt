package com.flooferland.showbiz.types.entity

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Player
import com.flooferland.showbiz.blocks.entities.ProgrammerBlockEntity
import com.flooferland.showbiz.registry.ModPlayerSynchedData
import com.flooferland.showbiz.show.toBitId
import com.flooferland.showbiz.types.ICompoundable
import com.flooferland.showbiz.types.MappedBits
import com.flooferland.showbiz.utils.Extensions.getBooleanOrNull
import com.flooferland.showbiz.utils.Extensions.getByteArrayOrNull
import com.flooferland.showbiz.utils.Extensions.getCompoundOrNull
import com.flooferland.showbiz.utils.Extensions.getLongOrNull
import com.flooferland.showbiz.utils.Extensions.getShortOrNull
import kotlin.collections.forEach
import kotlin.collections.forEachIndexed

/** Defines stuff for the programming block */
data class PlayerProgrammingData(
    var active: Boolean = false,
    var blockPos: BlockPos? = null,
    var heldKeys: BooleanArray = BooleanArray(SIZE) { false },
    val keysToBits: Array<MappedBits> = Array(SIZE) { MappedBits() }
) : ICompoundable {
    override fun save(tag: CompoundTag) {
        tag.putBoolean("active", active)
        blockPos?.let { tag.putLong("block", it.asLong()) }
        tag.putByteArray("held_keys", heldKeys.map { if (it) 1 else 0 })
        tag.put("keys_to_bits", CompoundTag().also { tag ->
            keysToBits.forEachIndexed { i, bits ->
                if (bits.isEmpty()) return@forEachIndexed
                tag.put(i.toString(), CompoundTag().apply {
                    bits.forEach { (mapping, bit) -> putShort(mapping, bit.toShort()) }
                })
            }
        })
    }

    override fun load(tag: CompoundTag) {
        tag.getBooleanOrNull("active")?.let { active = it }
        tag.getLongOrNull("block")?.let { blockPos = BlockPos.of(it) }
        tag.getByteArrayOrNull("held_keys")?.let { it.forEachIndexed { i, b -> heldKeys[i] = (b.toInt() == 1) } }
        tag.getCompoundOrNull("keys_to_bits")?.let { tag ->
            keysToBits.indices.forEach { i ->
                keysToBits[i].clear()
                tag.getCompoundOrNull(i.toString())?.also { tag ->
                    tag.allKeys.forEach { mapping -> keysToBits[i][mapping] = tag.getShortOrNull(mapping)?.toBitId() ?: 0.toBitId() }
                }
            }
        }
    }

    fun saveToPlayer(player: Player) {
        player.entityData.set(ModPlayerSynchedData.Programming.type, CompoundTag().also { this.save(it) })
    }

    /** Resets things other than settings back to default */
    fun cleanBasic() {
        active = false
        blockPos = null
        heldKeys.forEachIndexed { i, _ -> heldKeys[i] = false }
    }

    fun mapKeyToBit(keyId: Int) = keysToBits.getOrNull(keyId)

    companion object {
        const val SIZE = 9;

        fun getFromPlayer(player: Player) =
            PlayerProgrammingData().also { it.load(player.entityData.get(ModPlayerSynchedData.Programming.type)) }

        /** Resets things other than settings back to default */
        fun resetPlayerState(player: Player) {
            val data = getFromPlayer(player)
            data.blockPos?.let { pos ->
                val level = player.level() ?: return@let
                val entity = level.getBlockEntity(pos) as? ProgrammerBlockEntity ?: return@let
                entity.operators.removeIf { it.id == player.id }
            }
            data.cleanBasic()
            data.saveToPlayer(player)
        }
    }
}