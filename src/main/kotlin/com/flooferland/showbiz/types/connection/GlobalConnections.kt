package com.flooferland.showbiz.types.connection

import net.minecraft.core.*
import net.minecraft.world.level.block.entity.*

/** Common object for handling entity connections/links (See the client-side object) */
object GlobalConnections {
    val entries = mutableMapOf<BlockPos, MutableList<Point>>()

    enum class PointType { Input, Output }
    data class Connection(val pos: BlockPos, val point: Point)
    data class Point(val index: Int, val pointCount: Int, val id: String, val type: PointType, val connections: MutableList<Connection> = mutableListOf())

    fun updateConnections(manager: ConnectionManager, connectable: IConnectable) {
        if (connectable !is BlockEntity) return
        val points = entries[connectable.blockPos] ?: return
        points.forEach { point ->
            point.connections.removeIf { it.pos == connectable.blockPos || entries[it.pos] == null }
        }
        points.forEach { it.connections.clear() }

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