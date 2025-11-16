package com.flooferland.showbiz.types.connections

import net.minecraft.world.level.block.entity.*
import com.flooferland.showbiz.types.connection.GlobalConnections.Point
import com.flooferland.showbiz.types.connection.GlobalConnections.PointType
import com.flooferland.showbiz.types.connection.GlobalConnections.entries
import com.flooferland.showbiz.types.connection.GlobalConnections.updateConnections
import com.flooferland.showbiz.types.connection.IConnectable
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents

/**
 * Client object for [com.flooferland.showbiz.types.connection.GlobalConnections]
 */
object GlobalConnectionsClient {
    private val loaded = mutableListOf<IConnectable>()

    fun register() {
        ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register { blockEntity, level ->
            if (blockEntity !is IConnectable) return@register
            loaded.add(blockEntity)
        }
        ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register { blockEntity, level ->
            if (blockEntity !is IConnectable) return@register
            entries.remove(blockEntity.blockPos)
        }

        ClientTickEvents.END_WORLD_TICK.register { level ->
            for (connectable in loaded) {
                if (connectable !is BlockEntity) continue
                val manager = connectable.connectionManager

                val points = mutableListOf<Point>()
                var index = 0

                manager.inputs.keys.sorted().forEach { key ->
                    points.add(Point(index++, manager.inputs.size, key, PointType.Input))
                }
                manager.outputs.keys.sorted().forEach { key ->
                    points.add(Point(index++, manager.outputs.size, key, PointType.Output))
                }

                entries[connectable.blockPos] = points
                updateConnections(manager, connectable)
            }
        }
    }
}