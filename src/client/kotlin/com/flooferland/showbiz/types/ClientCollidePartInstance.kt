package com.flooferland.showbiz.types

import net.minecraft.client.*
import net.minecraft.client.multiplayer.*
import net.minecraft.core.*
import net.minecraft.world.entity.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import com.flooferland.showbiz.entities.CollidePartEntity
import com.flooferland.showbiz.types.collidepart.CollidePartId
import com.flooferland.showbiz.types.collidepart.CollidePartManager
import com.flooferland.showbiz.types.collidepart.ICollidePartInteractable
import software.bernie.geckolib.renderer.GeoBlockRenderer

class ClientCollidePartInstance(val owner: ICollidePartInteractable) : CollidePartManager.IInstance {
    val spawned = mutableMapOf<CollidePartId, CollidePartEntity>()

    val ownerEntity get() = owner as BlockEntity
    private val renderer get() = Minecraft.getInstance().blockEntityRenderDispatcher.getRenderer(ownerEntity) as? GeoBlockRenderer
    private val model get() = renderer?.geoModel

    override fun tick(level: Level, pos: BlockPos, state: BlockState) {
        val level = level as? ClientLevel ?: return

        spawned.entries.removeIf { (partId, entity) ->
            entity.isRemoved || entity.partId == CollidePartId.None
        }

        if (spawned.isEmpty()) refresh(level, pos)
    }

    override fun refresh(level: Level, pos: BlockPos) {
        val level = level as? ClientLevel ?: return
        spawned.values.removeIf { it.remove(Entity.RemovalReason.DISCARDED); true }
        for (id in owner.collidePartInstance.bonesToIds.values) {
            val entity = CollidePartEntity(level, pos.center, id, owner)
            level.addEntity(entity)
            spawned[id] = entity
        }
    }
}