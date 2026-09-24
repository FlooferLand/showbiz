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
import com.flooferland.showbiz.menus.CurtainControllerEditMenu
import com.flooferland.showbiz.network.packets.CurtainControllerEditPacket
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.show.toBitId
import com.flooferland.showbiz.types.*
import com.flooferland.showbiz.types.connection.ConnectionManager
import com.flooferland.showbiz.types.connection.IConnectable
import com.flooferland.showbiz.types.connection.PortDirection
import com.flooferland.showbiz.types.connection.data.PackedControlData
import com.flooferland.showbiz.types.connection.data.PackedShowData
import com.flooferland.showbiz.utils.Extensions.getIntArrayOrNull
import kotlin.jvm.optionals.getOrNull

class CurtainControllerBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.CurtainController.entityType!!, pos, blockState), IConnectable, EditScreenOwner<CurtainControllerEditPacket> {
    override val connectionManager = ConnectionManager(this)

    val control = connectionManager.port("control", PackedControlData(), PortDirection.Out)
    val show = connectionManager.port("show", PackedShowData(), PortDirection.In) { data ->
        val mapping = data.mapping ?: return@port
        if (!bitFilterOpen.containsKey(mapping) && !bitFilterClose.containsKey(mapping)) return@port

        val hasCloseBit = bitFilterClose[mapping]?.any() { data.signal.frameHas(it) } ?: false
        val hasOpenBit = bitFilterOpen[mapping]?.any() { data.signal.frameHas(it) } ?: false
        if (hasCloseBit || hasOpenBit) {
            control.data.writeCurtain(hasOpenBit && !hasCloseBit)
            control.send()
            control.data.clearCurtain()
        }
    }

    override var menuData = EditScreenMenu.EditScreenBuf(OwnerId.of(pos))
    var bitFilterOpen: MappedBits = MappedBits()
    var bitFilterClose: MappedBits = MappedBits()

    override fun getDisplayName() = Component.literal("Curtain Controller")!!
    override fun createMenu(i: Int, inventory: Inventory, player: Player): AbstractContainerMenu? {
        val player = player as? ServerPlayer ?: return null
        return CurtainControllerEditMenu(i, getScreenOpeningData(player))
    }
    override fun getScreenOpeningData(player: ServerPlayer) =
        CurtainControllerEditPacket(EditScreenMenu.EditScreenBuf(OwnerId.of(worldPosition), menuData.bitFilter, show.data.mapping), bitFilterOpen, bitFilterClose)

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        connectionManager.load(tag)
        menuData.loadAdditional(tag)

        // Backwards compatibility with pre-0.6.0
        if (tag.contains("bit_filter_open", Tag.TAG_INT_ARRAY.toInt())) {
            tag.getIntArrayOrNull("bit_filter_open")?.forEach {
                bitFilterOpen.addBit(BitChartStore.DEFAULT, it.toBitId())
            }
        } else {
            // Reading post-0.6.0
            tag.get("bit_filter_open")?.let { filterTag ->
                MappedBits.CODEC.parse(NbtOps.INSTANCE, filterTag).result().getOrNull()?.let { loaded ->
                    loaded.charts.forEach { chartId ->
                        loaded.getOrPutDefault(chartId).forEach { bitFilterOpen.addBit(chartId, it) }
                    }
                }
            }
        }

        // Backwards compatibility with pre-0.6.0
        if (tag.contains("bit_filter_close", Tag.TAG_INT_ARRAY.toInt())) {
            tag.getIntArrayOrNull("bit_filter_close")?.forEach {
                bitFilterClose.addBit(BitChartStore.DEFAULT, it.toBitId())
            }
        } else {
            // Reading post-0.6.0
            tag.get("bit_filter_close")?.let { filterTag ->
                MappedBits.CODEC.parse(NbtOps.INSTANCE, filterTag).result().getOrNull()?.let { loaded ->
                    loaded.charts.forEach { chartId ->
                        loaded.getOrPutDefault(chartId).forEach { bitFilterClose.addBit(chartId, it) }
                    }
                }
            }
        }
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        connectionManager.save(tag)
        menuData.saveAdditional(tag)
        MappedBits.CODEC.encodeStart(NbtOps.INSTANCE, bitFilterOpen).result().getOrNull()?.let {
            tag.put("bit_filter_open", it)
        }
        MappedBits.CODEC.encodeStart(NbtOps.INSTANCE, bitFilterClose).result().getOrNull()?.let {
            tag.put("bit_filter_close", it)
        }
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = super.getUpdateTag(registries)
        saveAdditional(tag, registries)
        return tag
    }

    override fun getUpdatePacket() = ClientboundBlockEntityDataPacket.create(this)!!
}