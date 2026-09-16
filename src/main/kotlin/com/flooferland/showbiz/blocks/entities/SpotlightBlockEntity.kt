package com.flooferland.showbiz.blocks.entities

import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.network.chat.*
import net.minecraft.network.protocol.game.*
import net.minecraft.server.level.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.inventory.*
import net.minecraft.world.level.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import net.minecraft.world.phys.*
import com.flooferland.showbiz.menus.SpotlightEditMenu
import com.flooferland.showbiz.network.packets.SpotlightEditPacket
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.types.EditScreenMenu
import com.flooferland.showbiz.types.EditScreenOwner
import com.flooferland.showbiz.types.connection.ConnectionManager
import com.flooferland.showbiz.types.connection.IConnectable
import com.flooferland.showbiz.types.connection.PortDirection
import com.flooferland.showbiz.types.connection.data.PackedShowData
import com.flooferland.showbiz.types.math.Vec2f
import com.flooferland.showbiz.utils.Extensions.getBooleanOrNull
import com.flooferland.showbiz.utils.Extensions.getFloatOrNull
import com.flooferland.showbiz.utils.Extensions.getIntOrNull
import com.flooferland.showbiz.utils.Extensions.markDirtyNotifyAll
import software.bernie.geckolib.animatable.GeoBlockEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

class SpotlightBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.Spotlight.entityType!!, pos, blockState), IConnectable, GeoBlockEntity, EditScreenOwner<SpotlightEditPacket> {
    override val connectionManager = ConnectionManager(this)
    val show = connectionManager.port("show", PackedShowData(), PortDirection.In) { show ->
        lit = menuData.bitFilter.chartHasBit(show.mapping) { show.signal.frameHas(it) }
    }

    val isLit: Boolean get() = lit || redstoneSignal > 0
    var value: Float = 0f  // Used for smoothing on the client
    var redstoneSignal: Int = 0
    var supportAbove = false
    var supportBelow = false

    override var menuData = EditScreenMenu.EditScreenBuf(blockPos)
    var turn = Vec2f.ZERO
    var angle = 45f
    var kelvin: Int = 5000
    var shadows: Boolean = true

    private var lit: Boolean = false
    var startPos = Vec3.ZERO!!
    var endPos = Vec3.ZERO!!

    val geckoCache = GeckoLibUtil.createInstanceCache(this)!!

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) = Unit
    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = geckoCache

    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        if (show.data.playing || isLit) {
            markDirtyNotifyAll()
        }
    }

    override fun getDisplayName() = Component.literal("Spotlight")!!
    override fun createMenu(i: Int, inventory: Inventory, player: Player): AbstractContainerMenu? {
        val player = player as? ServerPlayer ?: return null
        return SpotlightEditMenu(i, getScreenOpeningData(player))
    }
    override fun getScreenOpeningData(player: ServerPlayer) =
        SpotlightEditPacket(EditScreenMenu.EditScreenBuf(worldPosition, menuData.bitFilter, show.data.mapping), turn, angle, kelvin, shadows)

    fun applyPacket(packet: SpotlightEditPacket) {
        menuData = packet.base
        turn = packet.turn
        angle = packet.angle
        kelvin = packet.kelvin
        shadows = packet.shadows
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        connectionManager.load(tag)
        menuData.loadAdditional(tag)
        tag.getBooleanOrNull("lit")?.let { lit = it }
        tag.getBooleanOrNull("shadows")?.let { shadows = it }
        tag.getFloatOrNull("turn_x")?.let { turn.x = it }
        tag.getFloatOrNull("turn_y")?.let { turn.y = it }
        tag.getFloatOrNull("angle")?.let { angle = it }
        tag.getIntOrNull("kelvin")?.let { kelvin = it }

        // Stuff that can be recalculated if its lost
        tag.getIntOrNull("redstone_signal")?.let { redstoneSignal = it }
        tag.getBooleanOrNull("support_above")?.let { supportAbove = it }
        tag.getBooleanOrNull("support_below")?.let { supportBelow = it }
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        connectionManager.save(tag)
        menuData.saveAdditional(tag)
        tag.putBoolean("lit", lit)
        tag.putBoolean("shadows", shadows)
        tag.putFloat("turn_x", turn.x)
        tag.putFloat("turn_y", turn.y)
        tag.putFloat("angle", angle)
        tag.putInt("kelvin", kelvin)

        // Stuff that can be recalculated if its lost
        tag.putInt("redstone_signal", redstoneSignal)
        tag.putBoolean("support_above", supportAbove)
        tag.putBoolean("support_below", supportBelow)
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = super.getUpdateTag(registries)
        saveAdditional(tag, registries)
        return tag
    }

    override fun getUpdatePacket() = ClientboundBlockEntityDataPacket.create(this)!!
}