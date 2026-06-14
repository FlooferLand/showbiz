package com.flooferland.showbiz.types

import net.minecraft.client.*
import net.minecraft.client.multiplayer.*
import net.minecraft.world.entity.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.entity.*
import com.flooferland.showbiz.entities.CollidePartEntity
import com.flooferland.showbiz.types.collidepart.CollidePartId
import com.flooferland.showbiz.types.collidepart.CollidePartManager
import com.flooferland.showbiz.types.collidepart.ICollidePartInteractable
import software.bernie.geckolib.renderer.GeoRenderer

class ClientCollidePartInstance(val owner: ICollidePartInteractable) : CollidePartManager.IInstance {
    val spawned = mutableMapOf<CollidePartId, CollidePartEntity>()

    val ownerEntity get() = owner as BlockEntity
    private val renderer get() = Minecraft.getInstance().blockEntityRenderDispatcher.getRenderer(ownerEntity) as? GeoRenderer<*>
    private val model get() = renderer?.geoModel

    override fun tick(level: Level, ownerId: OwnerId) {
        val level = level as? ClientLevel ?: return

        spawned.entries.removeIf { (partId, entity) ->
            entity.isRemoved || entity.partId == CollidePartId.None
        }

        if (spawned.isEmpty()) refresh(level, ownerId)
    }

    override fun refresh(level: Level, ownerId: OwnerId) {
        val level = level as? ClientLevel ?: return
        val pos = ownerId.grabPos(level)

        spawned.values.forEach { it.remove(Entity.RemovalReason.DISCARDED) }
        spawned.values.clear()
        for (id in owner.collidePartInstance.bonesToIds.values) {
            val entity = CollidePartEntity(level, ownerId, id, pos)
            level.addEntity(entity)
            spawned[id] = entity
        }
    }
}