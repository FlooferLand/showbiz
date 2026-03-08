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
import com.flooferland.showbiz.menus.SpotlightEditMenu
import com.flooferland.showbiz.network.packets.SpotlightEditPacket
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.show.BitId
import com.flooferland.showbiz.show.toBitId
import com.flooferland.showbiz.types.math.Vec3fc
import com.flooferland.showbiz.types.connection.ConnectionManager
import com.flooferland.showbiz.types.connection.IConnectable
import com.flooferland.showbiz.types.connection.PortDirection
import com.flooferland.showbiz.types.connection.data.PackedShowData
import com.flooferland.showbiz.types.math.Vec2f
import com.flooferland.showbiz.utils.Extensions.getBooleanOrNull
import com.flooferland.showbiz.utils.Extensions.getFloatOrNull
import com.flooferland.showbiz.utils.Extensions.getIntArrayOrNull
import com.flooferland.showbiz.utils.Extensions.getIntOrNull
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import software.bernie.geckolib.animatable.GeoBlockEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

class SpotlightBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.SpotlightBlock.entityType!!, pos, blockState), IConnectable, GeoBlockEntity, ExtendedScreenHandlerFactory<SpotlightEditPacket> {
    override val connectionManager = ConnectionManager(this)

    val show = connectionManager.port("show", PackedShowData(), PortDirection.In) { show ->
        isOn = bitFilter.any { show.signal.frameHas(it) }
    }

    var isOn: Boolean = false
    var color: Int = 0xffffff
    var turn: Vec2f = Vec2f.ZERO
    var bitFilter = mutableListOf<BitId>()

    val geckoCache = GeckoLibUtil.createInstanceCache(this)!!

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) = Unit
    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = geckoCache

    override fun getDisplayName() = Component.literal("Spotlight")!!
    override fun createMenu(i: Int, inventory: Inventory, player: Player): AbstractContainerMenu? {
        val player = player as? ServerPlayer ?: return null
        return SpotlightEditMenu(i, getScreenOpeningData(player))
    }
    override fun getScreenOpeningData(player: ServerPlayer) =
        SpotlightEditPacket(worldPosition, bitFilter, turn, show.data.mapping)

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        connectionManager.load(tag)
        bitFilter = (tag.getIntArrayOrNull("bit_filter") ?: intArrayOf()).map { it.toBitId() }.toMutableList()
        tag.getBooleanOrNull("is_on")?.let { isOn = it }
        tag.getIntOrNull("color")?.let { color = it }
        tag.getFloatOrNull("turn_x")?.let { turn.x = it }
        tag.getFloatOrNull("turn_y")?.let { turn.y = it }
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        connectionManager.save(tag)
        tag.putIntArray("bit_filter", bitFilter.map { it.toInt() })
        tag.putBoolean("is_on", isOn)
        tag.putInt("color", color)
        tag.putFloat("turn_x", turn.x)
        tag.putFloat("turn_y", turn.y)
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = super.getUpdateTag(registries)
        saveAdditional(tag, registries)
        return tag
    }

    override fun getUpdatePacket() = ClientboundBlockEntityDataPacket.create(this)!!
}