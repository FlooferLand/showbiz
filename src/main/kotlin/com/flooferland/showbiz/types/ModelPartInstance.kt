package com.flooferland.showbiz.types

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import com.flooferland.showbiz.network.packets.ModelPartNamesPacket
import com.flooferland.showbiz.registry.ModBlocks
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

class ModelPartInstance(val owner: IModelPartInteractable, val modBlock: ModBlocks, val clientInstance: ModelPartManager.IInstance?) : ModelPartManager.IInstance {
    var cachedNameMap = mapOf<Int, String>()
    override fun kill() {
         clientInstance?.kill()
    }
    override fun tick(level: Level, pos: BlockPos, state: BlockState) {
        if (level.isClientSide) clientInstance?.tick(level, pos, state)
        if (level is ServerLevel) {
            if (owner !is BlockEntity) return
            if (owner.getNameMapping() != cachedNameMap) {
                val names = owner.getNameMapping()
                for (player in level.players().filter { it.distanceToSqr(owner.blockPos.center) < 8 * 8 }) {
                    ServerPlayNetworking.send(player, ModelPartNamesPacket(owner.blockPos, names))
                }
                cachedNameMap = owner.getNameMapping()
            }
        }
    }
}