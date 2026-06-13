package com.flooferland.showbiz.types.connection

import net.minecraft.network.*
import net.minecraft.server.level.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.network.packets.UpdateConnectionsPacket
import com.flooferland.showbiz.registry.ModItems
import com.flooferland.showbiz.utils.copy
import com.google.common.math.IntMath.pow
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

// TODO: Fix Reel-To-Reel's connections somehow remaining even after it's destroyed
// TODO: Add connection support for multiple levels/dimensions

/** Server-only object for handling entity connections/links (See the client-side object -> ClientConnections) */
object ServerConnections {
    val loaded = mutableListOf<IConnectable>()
    val points = mutableMapOf<ConnectionOwnerId, MutableList<Point>>()

    const val CLIENT_UPDATE_INTERVAL = 10L // ticks
    final val MAX_VIEW_DISTANCE_SQR = pow(32, 2)

    fun loadEntity(connectable: Any, level: Level) {
        if (level.dimension() != Level.OVERWORLD) return
        if (connectable !is IConnectable) return
        loaded.add(connectable)
    }
    fun unloadEntity(connectable: Any, level: Level) {
        if (level.dimension() != Level.OVERWORLD) return
        if (connectable !is IConnectable) return
        loaded.remove(connectable)
        for ((_, points) in points) {
            points.forEach { point -> point.connections.removeIf { it.id.matches(connectable) } }
        }

        val id = ConnectionOwnerId.of(connectable) ?: return
        points.remove(id)
        val packet = UpdateConnectionsPacket(id, emptyList())
        level.server?.playerList?.players?.forEach { ServerPlayNetworking.send(it, packet) }
    }
    fun broadcastUpdate(id: ConnectionOwnerId, level: ServerLevel) {
        val points = points[id] ?: return
        val server = level.server
        val connectable = id.grabConnectable(level)
        val centerPos = connectable?.grabCenterPos() ?: Vec3.ZERO

        for (player in server.playerList.players) {
            if (!canDisplayConnections(player)) continue
            if (connectable == null || player.distanceToSqr(centerPos) < MAX_VIEW_DISTANCE_SQR) {
                ServerPlayNetworking.send(player, UpdateConnectionsPacket(id, points))
            }
        }
    }

    init {
        ServerEntityEvents.ENTITY_LOAD.register { entity, level -> loadEntity(entity, level) }
        ServerEntityEvents.ENTITY_UNLOAD.register { blockEntity, level -> unloadEntity(blockEntity, level) }
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register { entity, level -> loadEntity(entity, level) }
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register { blockEntity, level -> unloadEntity(blockEntity, level) }

        ServerTickEvents.END_WORLD_TICK.register { level ->
            if (level.dimension() != Level.OVERWORLD) return@register
            val queued = mutableListOf<IConnectable>()
            for (connectable in loaded.copy()) {
                val manager = connectable.connectionManager
                val id = ConnectionOwnerId.of(connectable) ?: continue

                val points = mutableListOf<Point>()
                var index = 0

                manager.inputs.keys.sorted().forEach { key ->
                    points.add(Point(index++, manager.inputs.size, key, PointType.Input))
                }
                manager.outputs.keys.sorted().forEach { key ->
                    points.add(Point(index++, manager.outputs.size, key, PointType.Output))
                }

                ServerConnections.points[id] = points
                queued.add(connectable)

                // Checking that I didn't forget to use the save/load functions of ConnectionManager
                if (Showbiz.log.isDebugEnabled && connectable is BlockEntity) {
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

            if (Showbiz.log.isDebugEnabled) {
                for (connectable in loaded.toList()) {
                    if (connectable !is BlockEntity) continue
                    val state = level.getBlockState(connectable.blockPos)
                    if (state.isAir || level.getBlockEntity(connectable.blockPos) != connectable) {
                        Showbiz.log.error("Connection block has no block entity! Make sure you call the parent onRemove function")
                        loaded.remove(connectable)
                    }
                }
            }
        }
    }

    fun updateConnections(connectable: IConnectable) {
        val manager = connectable.connectionManager

        val id = ConnectionOwnerId.of(connectable)
        val level = connectable.grabLevel()

        // Clearing invalid listeners
        if (level != null && level.gameTime > 100L && !level.isClientSide) {
            for ((_, port) in manager.outputs) {
                port.removeListeners { ownerId ->
                    val missing = ownerId.grabConnectable(level) == null
                    val unloaded = !ownerId.isLoaded(level)
                    connectable.grabRemoved() || (missing && unloaded)
                }
            }
        }

        // Cleaning connections
        val points = points[id] ?: return
        points.forEach { it.connections.clear() }

        // Adding listeners from the other side
        for ((portId, port) in manager.outputs) {
            val fromIndex = points.indexOfFirst { it.id == portId && it.type == PointType.Output }
            if (fromIndex == -1) continue

            // TODO: "id" being used here isn't correct? No fucking clue why using listenerId breaks things
            for (listenerId in port.readListeners()) {
                val otherPoints = ServerConnections.points[id] ?: continue
                val otherPoint = otherPoints.firstOrNull { it.id == portId && it.type == PointType.Input } ?: continue
                points[fromIndex].connections.add(Connection(listenerId, otherPoint))
            }
        }

        // Sending a packet to the client while they hold the wand
        if (level is ServerLevel && level.gameTime % CLIENT_UPDATE_INTERVAL == 0L && id != null) {
            broadcastUpdate(id, level)
        }
    }

    fun canDisplayConnections(player: Player) =
        player.isHolding(ModItems.Wand.item)

    enum class PointType { Input, Output }
    data class Connection(val id: ConnectionOwnerId, val point: Point) {
        companion object {
            fun encode(buf: FriendlyByteBuf, connection: Connection) {
                connection.id.encode(buf)
                Point.encode(buf, connection.point, shallow = true)
            }
            fun decode(buf: FriendlyByteBuf): Connection = Connection(
                id = ConnectionOwnerId.decode(buf),
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