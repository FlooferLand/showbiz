package com.flooferland.showbiz.registry

import net.minecraft.nbt.*
import net.minecraft.network.syncher.*
import net.minecraft.world.entity.player.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.types.ICompoundable
import com.flooferland.showbiz.types.entity.PlayerProgrammingData
import com.flooferland.showbiz.utils.Extensions.getCompoundOrNull

sealed class ModPlayerSynchedData(id: String, val default: ICompoundable) {
    object Programming : ModPlayerSynchedData("programming", PlayerProgrammingData())

    val id = "${Showbiz.MOD_ID}_$id"
    val type = SynchedEntityData.defineId(Player::class.java, EntityDataSerializers.COMPOUND_TAG)!!

    companion object {
        fun defineSynchedData(builder: SynchedEntityData.Builder) {
            for (entry in entries)
                builder.define(entry.type, CompoundTag().also { entry.default.save(it) })
        }
        fun saveAdditional(tag: CompoundTag, player: Player) {
            for (entry in entries)
                tag.put(entry.id, player.entityData.get(entry.type))
        }
        fun loadAdditional(tag: CompoundTag, player: Player) {
            for (entry in entries)
                tag.getCompoundOrNull(entry.id)?.let { player.entityData.set(entry.type, it) }
        }

        var entries: List<ModPlayerSynchedData>
        init {
            val list = mutableListOf<ModPlayerSynchedData>()
            ModPlayerSynchedData::class.sealedSubclasses.forEach {
                it.objectInstance?.let { instance -> list.add(instance) }
            }
            entries = list
        }
    }
}