package com.flooferland.showbiz.types.connection

import net.minecraft.core.*
import net.minecraft.world.level.block.entity.*
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents

/** Common object for handling entity connections/links (See the client-side object) */
object GlobalConnections {
    private val loaded = mutableListOf<IConnectable>()
    val entries = mutableMapOf<BlockPos, MutableList<Point>>()

    enum class PointType { Input, Output }
    data class Connection(val pos: BlockPos, val point: Point)
    data class Point(val index: Int, val pointCount: Int, val id: String, val type: PointType, val connections: MutableList<Connection> = mutableListOf())

    init {
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register { blockEntity, level ->
            if (blockEntity !is IConnectable) return@register
            loaded.add(blockEntity)
        }
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register { blockEntity, level ->
            if (blockEntity !is IConnectable) return@register
            loaded.remove(blockEntity)
            for ((_, points) in entries) {
                points.forEach { point ->
                    point.connections.removeIf { it.pos == blockEntity.blockPos }
                }
            }
            entries.remove(blockEntity.blockPos)
        }
        ServerTickEvents.END_WORLD_TICK.register { level ->
            val queued = mutableListOf<IConnectable>()
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
                queued.add(connectable)
            }
            for (connectable in queued) {
                updateConnections(connectable)
            }
        }
    }

    fun updateConnections(connectable: IConnectable) {
        if (connectable !is BlockEntity) return
        val manager = connectable.connectionManager

        // Clearing listeners
        for ((channel, receiver) in manager.listeners) {
            val iterator = receiver.iterator()
            while (iterator.hasNext()) {
                val receiver = iterator.next()
                if (connectable.level?.getBlockEntity(receiver.pos) !is IConnectable) {
                    iterator.remove()
                }
            }
        }

        // Cleaning connections
        val points = entries[connectable.blockPos] ?: return
        points.forEach { it.connections.clear() }

        // Adding listeners
        for ((output, receivers) in manager.listeners) {
            val fromIndex = points.indexOfFirst { it.id == output.id && it.type == PointType.Output }
            if (fromIndex == -1) continue

            for (receiver in receivers) {
                val otherPoints = entries[receiver.pos] ?: continue
                val otherPoint = otherPoints.firstOrNull { it.id == receiver.channelId && it.type == PointType.Input } ?: continue
                points[fromIndex].connections.add(Connection(receiver.pos, otherPoint))
            }
        }
    }
}