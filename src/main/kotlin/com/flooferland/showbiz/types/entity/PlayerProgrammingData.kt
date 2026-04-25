package com.flooferland.showbiz.types.entity

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Player
import com.flooferland.showbiz.registry.ModPlayerSynchedData
import com.flooferland.showbiz.show.BitIdArray
import com.flooferland.showbiz.show.toBitId
import com.flooferland.showbiz.types.ICompoundable
import com.flooferland.showbiz.utils.Extensions.getBooleanOrNull
import com.flooferland.showbiz.utils.Extensions.getByteArrayOrNull
import com.flooferland.showbiz.utils.Extensions.getIntArrayOrNull
import com.flooferland.showbiz.utils.Extensions.getLongOrNull

/** Defines stuff for the programming block */
data class PlayerProgrammingData(
    var active: Boolean = false,
    var blockPos: BlockPos? = null,
    var heldKeys: BooleanArray = BooleanArray(9) { false },
    var keysToBits: BitIdArray = BitIdArray(9)
) : ICompoundable {
    override fun save(tag: CompoundTag) {
        tag.putBoolean("active", active)
        blockPos?.let { tag.putLong("block", it.asLong()) }
        tag.putByteArray("held_keys", heldKeys.map { if (it) 1 else 0 })
        tag.putIntArray("keys_to_bits", keysToBits.map { it.toInt() })
    }

    override fun load(tag: CompoundTag) {
        tag.getBooleanOrNull("active")?.let { active = it }
        tag.getLongOrNull("block")?.let { blockPos = BlockPos.of(it) }
        tag.getByteArrayOrNull("held_keys")?.let { it.forEachIndexed { i, b -> heldKeys[i] = (b.toInt() == 1) } }
        tag.getIntArrayOrNull("keys_to_bits")?.let { ints -> ints.forEachIndexed { i, int -> keysToBits[i] = int.toBitId() } }
    }

    fun saveToPlayer(player: Player) {
        player.entityData.set(ModPlayerSynchedData.Programming.type, CompoundTag().also { this.save(it) })
    }

    fun mapKeyToBit(keyId: Int) = keysToBits.getOrNull(keyId)?.toShort() ?: 0

    companion object {
        fun getFromPlayer(player: Player) =
            PlayerProgrammingData().also { it.load(player.entityData.get(ModPlayerSynchedData.Programming.type)) }
    }
}