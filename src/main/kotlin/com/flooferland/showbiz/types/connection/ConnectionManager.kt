package com.flooferland.showbiz.types.connection

import net.minecraft.nbt.*
import net.minecraft.world.level.block.entity.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.utils.Extensions.getCompoundOrNull

class ConnectionManager(val entity: BlockEntity) {
    /** Ports that take in information */
    val inputs = hashMapOf<String, ConnectionPort<*>>()

    /** Ports that put out information; Includes listeners */
    val outputs = hashMapOf<String, ConnectionPort<*>>()

    fun <T: ConnectionData> addInput(port: ConnectionPort<T>) {
        if (port.direction == PortDirection.Out)
            Showbiz.log.warn("Port '${port.name}' was added via 'addInput'. However, it is an output.")
        inputs[port.id] = port
    }
    fun <T: ConnectionData> addOutput(port: ConnectionPort<T>) {
        if (port.direction == PortDirection.In)
            Showbiz.log.warn("Port '${port.name}' was added via 'addOutput'. However, it is an input.")
        outputs[port.id] = port
    }

    fun <T: ConnectionData> port(id: String, data: T, direction: PortDirection, react: ConnectionPort<T>.(T) -> Unit = {}) =
        ConnectionPort(this.entity as IConnectable, id, data, direction, react)

    /** Saves connections to a tag */
    fun save(tag: CompoundTag) {
        val tag = CompoundTag().also { tag.put("connections", it) }
        inputs.forEach { (id, port) ->
            val saved = runCatching { tag.put(id, CompoundTag().also { port.saveOrThrow(it) }) }
            saved.onFailure { throwable ->
                Showbiz.log.error("Failed to save input '${id}' on block entity '${entity::class.java.name}'", throwable)
            }
        }
        outputs.forEach { (id, port) ->
            val saved = runCatching { tag.put(id, CompoundTag().also { port.saveOrThrow(it) }) }
            saved.onFailure { throwable ->
                Showbiz.log.error("Failed to save output '${id}' on block entity '${entity::class.java.name}'", throwable)
            }
        }
    }

    /** Loads connections from a tag */
    fun load(tag: CompoundTag) {
        val tag = tag.getCompoundOrNull("connections") ?: return
        inputs.forEach { (id, port) ->
            val loaded = runCatching { tag.getCompoundOrNull(id)?.let { port.loadOrThrow(it) } }
            loaded.onFailure { throwable ->
                Showbiz.log.error("Failed to load input '${id}' on block entity '${entity::class.java.name}'", throwable)
            }
        }
        outputs.forEach { (id, port) ->
            val loaded = runCatching { tag.getCompoundOrNull(id)?.let { port.loadOrThrow(it) } }
            loaded.onFailure { throwable ->
                Showbiz.log.error("Failed to load output '${id}' on block entity '${entity::class.java.name}'", throwable)
            }
        }
    }
}