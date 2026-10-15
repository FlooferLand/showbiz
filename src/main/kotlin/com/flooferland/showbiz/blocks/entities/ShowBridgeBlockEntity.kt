package com.flooferland.showbiz.blocks.entities

import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.network.chat.*
import net.minecraft.network.protocol.game.*
import net.minecraft.server.level.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.inventory.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import com.fazecast.jSerialComm.SerialPort
import com.flooferland.showbiz.Permissions
import com.flooferland.showbiz.menus.ShowBridgeEditMenu
import com.flooferland.showbiz.network.packets.editscreen.ShowBridgeEditPacket
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.types.EditScreenMenu
import com.flooferland.showbiz.types.EditScreenOwner
import com.flooferland.showbiz.types.OwnerId
import com.flooferland.showbiz.types.connection.ConnectionManager
import com.flooferland.showbiz.types.connection.IConnectable
import com.flooferland.showbiz.types.connection.PortDirection
import com.flooferland.showbiz.types.connection.data.PackedShowData
import com.flooferland.showbiz.types.showbridge.ShowBridgeMethod
import com.flooferland.showbiz.types.showbridge.ShowBridgeProfile
import com.flooferland.showbiz.utils.Extensions.getCompoundOrNull
import java.util.UUID

class ShowBridgeBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.ShowBridge.entityType!!, pos, blockState), IConnectable, EditScreenOwner<ShowBridgeEditPacket> {
    override val connectionManager = ConnectionManager(this)
    val show = connectionManager.port("show", PackedShowData(), PortDirection.In)

    var serverProfile = ShowBridgeProfile()
    val playerProfiles = mutableMapOf<UUID, ShowBridgeProfile>()

    override var menuData = EditScreenMenu.EditScreenBuf(OwnerId.of(blockPos))
    override fun getDisplayName() = Component.literal("Show Bridge")!!
    override fun createMenu(i: Int, inventory: Inventory, player: Player): AbstractContainerMenu? {
        val player = player as? ServerPlayer ?: return null
        return ShowBridgeEditMenu(i, getScreenOpeningData(player))
    }
    override fun getScreenOpeningData(player: ServerPlayer): ShowBridgeEditPacket {
        var serverPorts = emptyList<String>()
        if (Permissions.EditServerShowBridge.test(player)) {
            serverPorts = SerialPort.getCommPorts().map { it.systemPortName }
        }
        return ShowBridgeEditPacket(
            EditScreenMenu.EditScreenBuf(OwnerId.of(worldPosition)),
            serverProfile = serverProfile,
            playerProfile = playerProfiles.getOrPut(player.uuid) { ShowBridgeProfile() },
            serverPorts = serverPorts
        )
    }

    fun tick() {
        val level = level ?: return
        if (!show.data.playing) {
            serverProfile.serial?.closePort()
            playerProfiles.values.forEach { it.serial?.closePort() }
            return
        }

        if (level.isClientSide) {
            for (profile in playerProfiles.values) {
                tickProfile(profile)
            }
        } else {
            tickProfile(serverProfile)
        }
    }
    fun tickProfile(profile: ShowBridgeProfile) {
        if (!profile.enabled) return

        // Opening the port
        if (profile.serial == null) {
            try {
                val port = SerialPort.getCommPort(profile.serialPort)
                port.setBaudRate(profile.serialBaudRate)
                port.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0)
                if (!port.openPort()) {
                    profile.output = "Failed to open port '${profile.serialPort}' (error ${port.lastErrorCode} at ${port.lastErrorLocation})"
                    return
                }
                profile.serial = port
            } catch (e: Exception) {
                profile.output = "Failed to get port '${profile.serialPort}': $e"
                return
            }
        }

        // Writing data
        val serial = profile.serial ?: return
        fun serialWrite(buffer: ByteArray): Int {
            val written = serial.writeBytes(buffer, buffer.size)
            profile.output = "Wrote $written bytes"
            if (written < 0) {
                serial.closePort()
                profile.serial = null
                profile.output = "The port was closed"
            }
            return written
        }
        when (profile.method) {
            ShowBridgeMethod.TextList -> {
                val text = show.data.signal.joinToString(profile.textList.separator)
                val bytes = text.encodeToByteArray()
                serialWrite(bytes)
            }
            ShowBridgeMethod.BinaryList -> { profile.output = "Method not implemented" }
        }
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        connectionManager.save(tag)
        tag.put("profiles", CompoundTag().also { profiles ->
            profiles.put("server", CompoundTag().also { serverProfile.save(it) })
            profiles.put("players", CompoundTag().also { players ->
                playerProfiles.forEach { (id, profile) ->
                    players.put(id.toString(), CompoundTag().also { profile.save(it) })
                }
            })
        })
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        connectionManager.load(tag)
        tag.getCompoundOrNull("profiles")?.let { profiles ->
            profiles.getCompoundOrNull("server")?.let { serverProfile.load(it) }
            profiles.getCompoundOrNull("players")?.let { players ->
                playerProfiles.clear()
                players.allKeys.forEach { key ->
                    val id = runCatching { UUID.fromString(key) }.getOrNull() ?: return@forEach
                    playerProfiles[id] = ShowBridgeProfile().also { it.load(players.getCompound(key)) }
                }
            }
        }
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag? {
        val tag = super.getUpdateTag(registries)
        saveAdditional(tag, registries)
        return tag
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket =
        ClientboundBlockEntityDataPacket.create(this)
}