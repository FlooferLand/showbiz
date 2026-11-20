package com.flooferland.showbiz.types

import com.flooferland.showbiz.entities.CollidePartEntity
import com.flooferland.showbiz.types.collidepart.CollidePartId
import com.flooferland.showbiz.types.collidepart.CollidePartManager
import com.flooferland.showbiz.types.collidepart.ICollidePartInteractable
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.entity.BlockEntity
import software.bernie.geckolib.renderer.GeoRenderer

class ClientCollidePartInstance(val owner: ICollidePartInteractable) : CollidePartManager.IInstance {
    val spawned = mutableMapOf<CollidePartId, CollidePartEntity>()

    val ownerId get() = OwnerId.ofPart(owner)
    val ownerEntity get() = owner as BlockEntity

    private val renderer get() = Minecraft.getInstance().blockEntityRenderDispatcher.getRenderer(ownerEntity) as? GeoRenderer<*>
    private val model get() = renderer?.geoModel

    override fun tick() {
        spawned.entries.removeIf { (partId, entity) ->
            entity.isRemoved || entity.partId == CollidePartId.None
        }
        if (spawned.isEmpty()) refresh()
    }

    override fun kill() {
        refresh()
    }

    override fun refresh() {
        val level = owner.partGetLevel() as? ClientLevel ?: return
        val ownerId = ownerId

        spawned.values.forEach { it.remove(Entity.RemovalReason.DISCARDED) }
        spawned.values.clear()
        if (ownerId?.isRemoved(level) == false) {
            val pos = ownerId.grabPos(level)
            for (id in owner.collidePartInstance.bonesToIds.values) {
                val entity = CollidePartEntity(level, ownerId, id, pos)
                level.addEntity(entity)
                spawned[id] = entity
            }
        }
    }
}