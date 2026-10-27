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
import com.flooferland.showbiz.Permissions
import com.flooferland.showbiz.menus.ShowBridgeEditMenu
import com.flooferland.showbiz.network.packets.editscreen.ShowBridgeEditPacket
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.show.Drawer
import com.flooferland.showbiz.show.bitIdArrayOf
import com.flooferland.showbiz.types.EditScreenMenu
import com.flooferland.showbiz.types.EditScreenOwner
import com.flooferland.showbiz.types.OwnerId
import com.flooferland.showbiz.types.connection.ConnectionManager
import com.flooferland.showbiz.types.connection.IConnectable
import com.flooferland.showbiz.types.connection.PortDirection
import com.flooferland.showbiz.types.connection.data.PackedShowData
import com.flooferland.showbiz.types.showbridge.SafeSerialPort
import com.flooferland.showbiz.types.showbridge.ShowBridgeMethod
import com.flooferland.showbiz.types.showbridge.ShowBridgeProfile
import com.flooferland.showbiz.utils.Extensions.getCompoundOrNull
import java.util.UUID

class ShowBridgeBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.ShowBridge.entityType!!, pos, blockState), IConnectable, EditScreenOwner<ShowBridgeEditPacket> {
    override val connectionManager = ConnectionManager(this)
    val show = connectionManager.port("show", PackedShowData(), PortDirection.In)

    var serverProfile = ShowBridgeProfile()
    val playerProfiles = mutableMapOf<UUID, ShowBridgeProfile>()

    val serialPorts = mutableMapOf<String, SafeSerialPort>()
    var lastPlaying = false

    fun tick() {
        val level = level ?: return
        val playing = show.data.playing
        val keepPlaying = playing || lastPlaying

        if (level.isClientSide) {
            for ((id, profile) in playerProfiles) {
                if (!keepPlaying && !profile.stayOpen) {
                    serialPorts[profile.serialPort]?.close()
                    continue
                }
                tickProfile(id.toString(), profile)
            }
        } else if (keepPlaying || serverProfile.stayOpen) {
            tickProfile("server", serverProfile)
        }
        lastPlaying = playing
    }

    fun tickProfile(key: String, profile: ShowBridgeProfile) {
        serialPorts[key]?.let { port ->
            val changed = port.portName != profile.serialPort || port.baudRate != profile.serialBaudRate
            if (!profile.enabled || changed) {
                serialPorts.remove(key)?.close()
                profile.output = "Closed the port"
            }
        }

        if (!profile.enabled) return
        if (profile.serialPort.isBlank()) {
            profile.output = "No serial port set"
            return
        }

        val bits = run {
            if (profile.bitFilter.isEmpty()) return@run show.data.signal.clone()
            var filtered = bitIdArrayOf()
            profile.bitFilter.bits.forEach { if (show.data.signal.frameHas(it)) filtered += it }
            filtered
        }

        // Writing data
        val serial = serialPorts.getOrPut(key) {
            SafeSerialPort(profile.serialPort, profile.serialBaudRate, "Show Bridge at ${blockPos.toShortString()}")
        }
        when (profile.method) {
            ShowBridgeMethod.TextList -> {
                // Command
                val playing = show.data.playing
                if (lastPlaying != playing) {
                    val word = if (playing) "unpause" else "pause"
                    serial.write("c $word\n".encodeToByteArray())
                }

                // Signal
                val text = "s " + bits.joinToString(",") + "\n"
                serial.write(text.encodeToByteArray())
            }
            ShowBridgeMethod.BinaryList -> {
                // Signal
                val buffer = mutableListOf<Byte>()
                for (bit in bits) {
                    val localBit = Drawer.toLocalBit(bit)
                    buffer += when (localBit.second) {
                        Drawer.Top -> 0x01
                        Drawer.Bottom -> 0x02
                    }
                    buffer += localBit.first.toByte()
                }
                buffer += 0x00
                serial.write(buffer.toByteArray())
            }
        }
        profile.output = serial.status
    }

    override fun setRemoved() {
        serialPorts.values.forEach { it.close() }
        serialPorts.clear()
        super.setRemoved()
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

    //region Menu stuff
    override var menuData = EditScreenMenu.EditScreenBuf(OwnerId.of(blockPos))
    override fun getDisplayName() = Component.literal("Show Bridge")!!
    override fun createMenu(i: Int, inventory: Inventory, player: Player): AbstractContainerMenu? {
        val player = player as? ServerPlayer ?: return null
        return ShowBridgeEditMenu(i, getScreenOpeningData(player))
    }
    override fun getScreenOpeningData(player: ServerPlayer): ShowBridgeEditPacket {
        var serverAllPorts = emptyList<String>()
        var serverFreePorts = emptyList<String>()
        if (Permissions.EditServerShowBridge.test(player)) {
            serverAllPorts = SafeSerialPort.getAllPorts()
            serverFreePorts = SafeSerialPort.getUnusedPorts()
        }
        return ShowBridgeEditPacket(
            EditScreenMenu.EditScreenBuf(OwnerId.of(worldPosition)),
            serverProfile = serverProfile,
            playerProfile = playerProfiles.getOrPut(player.uuid) { ShowBridgeProfile() },
            serverAllPorts = serverAllPorts,
            serverFreePorts = serverFreePorts
        )
    }
    //endregion

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag? {
        val tag = super.getUpdateTag(registries)
        saveAdditional(tag, registries)
        return tag
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket =
        ClientboundBlockEntityDataPacket.create(this)
}