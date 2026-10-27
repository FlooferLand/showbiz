package com.flooferland.showbiz.types.showbridge

import com.fazecast.jSerialComm.SerialPort
import com.flooferland.showbiz.Showbiz
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import nl.adaptivity.xmlutil.core.impl.multiplatform.name

/**
 * For some reason with jSerialComm the entire server thread can freeze up when a port fails to open.
 * Runs on a separate thread, but the public things are safe to use from the main thread
 */
class SafeSerialPort(val portName: String, val baudRate: Int, private val owner: String) {
    @Volatile var status = "Not connected"
        private set

    private val worker = Executors.newSingleThreadExecutor { Thread(it, "${SafeSerialPort::class.name}-${portName}").apply { isDaemon = true } }
    private val pending = ConcurrentLinkedQueue<ByteArray>()
    @Volatile private var closed = false

    // Worker thread
    private var port: SerialPort? = null
    private var retryAt = 0L

    //region Public methods
    fun write(buffer: ByteArray) {
        if (closed) return
        pending.add(buffer)
        worker.execute { flush() }
    }
    fun close() {
        if (closed) return
        closed = true
        worker.execute { disconnect() }
        worker.shutdown()
    }
    //endregion

    //region Private methods
    private fun flush() {
        if (closed) return
        val active = port?.takeIf { it.isOpen } ?: connect() ?: return

        while (true) {
            val data = pending.poll() ?: break
            val written = runCatching { active.writeBytes(data, data.size) }.getOrNull() ?: -1
            if (written < data.size) {
                fail("Lost connection to '$portName', retrying")
                return
            }
            status = "Wrote $written bytes"
        }
    }

    private fun connect(): SerialPort? {
        if (System.currentTimeMillis() < retryAt) return null

        val holder = usedPorts.putIfAbsent(portName, this)
        if (holder != null && holder !== this) {
            return fail("'$portName' is already in use by ${holder.owner}")
        }

        try {
            val port = SerialPort.getCommPort(portName)
            port.setBaudRate(baudRate)
            port.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, WRITE_TIMEOUT_MS)
            if (!port.openPort()) {
                return fail("Failed to open '$portName', another program might be using it. (error ${port.lastErrorCode} at ${port.lastErrorLocation})")
            }
            status = "Connected to '$portName'"
            this.port = port
            return port
        } catch (e: Exception) {
            return fail("Failed to get port '$portName': ${e.message}")
        }
    }

    private fun disconnect() {
        runCatching { port?.closePort() }
        port = null
        usedPorts.remove(portName, this)
    }

    private fun fail(message: String): SerialPort? {
        disconnect()
        status = message
        retryAt = System.currentTimeMillis() + RETRY_DELAY_MS
        Showbiz.log.error("[$portName]: $message")
        return null
    }
    //endregion

    companion object {
        const val WRITE_TIMEOUT_MS = 1000
        const val RETRY_DELAY_MS = 2000L

        /** Port names (ex: COM3) to serial ports **/
        private val usedPorts = ConcurrentHashMap<String, SafeSerialPort>()

        fun closeAll() {
            for (port in usedPorts.values) {
                port.close()
            }
        }

        fun getUsedPorts(): List<String> {
            return usedPorts.keys.toList()
        }
        fun getUnusedPorts(): List<String> {
            return getAllPorts().filter { !usedPorts.contains(it) }
        }
        fun getAllPorts(): List<String> {
            return SerialPort.getCommPorts().map { it.systemPortName }
        }
    }
}