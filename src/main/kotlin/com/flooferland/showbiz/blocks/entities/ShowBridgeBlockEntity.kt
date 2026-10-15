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
    override fun getScreenOpeningData(player: ServerPlayer) =
        ShowBridgeEditPacket(
            EditScreenMenu.EditScreenBuf(OwnerId.of(worldPosition)),
            serverProfile = serverProfile,
            playerProfile = playerProfiles.getOrPut(player.uuid) { ShowBridgeProfile() }
        )

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