package com.flooferland.showbiz.types.connection

import net.minecraft.core.*
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.network.packets.UpdateConnectionsPacket
import com.flooferland.showbiz.registry.ModItems
import com.google.common.math.IntMath.pow
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

// TODO: Add connection support for multiple levels/dimensions

/** Server-only object for handling entity connections/links (See the client-side object -> ClientConnections) */
object ServerConnections {
    val loaded = mutableListOf<IConnectable>()
    val entries = mutableMapOf<BlockPos, MutableList<Point>>()

    const val CLIENT_UPDATE_INTERVAL = 10L // ticks
    final val MAX_VIEW_DISTANCE_SQR = pow(24, 2)

    init {
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register { blockEntity, level ->
            if (level.dimension() != Level.OVERWORLD) return@register
            if (blockEntity !is IConnectable) return@register
            loaded.add(blockEntity)
        }
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register { blockEntity, level ->
            if (level.dimension() != Level.OVERWORLD) return@register
            if (blockEntity !is IConnectable) return@register
            loaded.remove(blockEntity)
            for ((_, points) in entries) {
                points.forEach { point ->
                    point.connections.removeIf { it.pos == blockEntity.blockPos }
                }
            }
            entries.remove(blockEntity.blockPos)

            val packet = UpdateConnectionsPacket(blockEntity.blockPos, emptyList())
            level.server.playerList.players.forEach { ServerPlayNetworking.send(it, packet) }
        }
        ServerTickEvents.END_WORLD_TICK.register { level ->
            if (level.dimension() != Level.OVERWORLD) return@register
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

                // Checking that I didn't forget to use the save/load functions of ConnectionManager
                if (Showbiz.log.isDebugEnabled) {
                    val tag = connectable.saveCustomOnly(level.registryAccess())
                    connectable.loadCustomOnly(tag, level.registryAccess())
                    val blockId = BlockEntityType.getKey(connectable.type)?.path
                    if (!manager.saveCalled)
                        Showbiz.log.error("${ServerConnections::class.simpleName}: Forgot to add the save function to the '$blockId' block")
                    if (!manager.loadCalled)
                        Showbiz.log.error("${ServerConnections::class.simpleName}: Forgot to add the load function to the '$blockId' block")
                }
            }
            for (connectable in queued) {
                updateConnections(connectable)
            }
        }
    }

    fun updateConnections(connectable: IConnectable) {
        if (connectable !is BlockEntity) return
        val manager = connectable.connectionManager

        // Clearing invalid listeners
        for ((_, port) in manager.outputs) {
            port.removeListeners { connectable.level?.getBlockEntity(it) !is IConnectable }
        }

        // Cleaning connections
        val points = entries[connectable.blockPos] ?: return
        points.forEach { it.connections.clear() }

        // Adding listeners from the other side
        for ((portId, port) in manager.outputs) {
            val fromIndex = points.indexOfFirst { it.id == portId && it.type == PointType.Output }
            if (fromIndex == -1) continue

            for (pos in port.readListeners()) {
                val otherPoints = entries[pos] ?: continue
                val otherPoint = otherPoints.firstOrNull { it.id == portId && it.type == PointType.Input } ?: continue
                points[fromIndex].connections.add(Connection(pos, otherPoint))
            }
        }

        // Sending a packet to the client while they hold the wand
        val level = connectable.level ?: return
        val server = level.server ?: return
        if (level.gameTime % CLIENT_UPDATE_INTERVAL == 0L) {
            for (player in server.playerList.players) {
                if (!canDisplayConnections(player)) continue
                if (player.distanceToSqr(connectable.blockPos.center) < MAX_VIEW_DISTANCE_SQR) {
                    ServerPlayNetworking.send(player, UpdateConnectionsPacket(connectable.blockPos, points))
                }
            }
        }
    }

    fun canDisplayConnections(player: Player) =
        player.isHolding(ModItems.Wand.item)

    enum class PointType { Input, Output }
    data class Connection(val pos: BlockPos, val point: Point) {
        companion object {
            fun encode(buf: FriendlyByteBuf, connection: Connection) {
                buf.writeBlockPos(connection.pos)
                Point.encode(buf, connection.point, shallow = true)
            }
            fun decode(buf: FriendlyByteBuf): Connection = Connection(
                pos = buf.readBlockPos(),
                point = Point.decode(buf, shallow = true)
            )
        }
    }
    data class Point(val index: Int, val pointCount: Int, val id: String, val type: PointType, val connections: MutableList<Connection> = mutableListOf()) {
        companion object {
            fun encode(buf: FriendlyByteBuf, point: Point, shallow: Boolean = false) {
                buf.writeInt(point.index)
                buf.writeInt(point.pointCount)
                buf.writeUtf(point.id)
                buf.writeEnum(point.type)
                if (!shallow) buf.writeCollection(point.connections, Connection::encode)
            }
            fun decode(buf: FriendlyByteBuf, shallow: Boolean = false): Point = Point(
                index = buf.readInt(),
                pointCount = buf.readInt(),
                id = buf.readUtf(),
                type = buf.readEnum(PointType::class.java),
                connections = if (!shallow) buf.readList(Connection::decode) else mutableListOf()
            )
        }
    }
}