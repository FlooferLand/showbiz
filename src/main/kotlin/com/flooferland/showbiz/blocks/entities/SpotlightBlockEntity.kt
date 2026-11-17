package com.flooferland.showbiz.blocks.entities

import com.flooferland.showbiz.menus.SpotlightEditMenu
import com.flooferland.showbiz.network.packets.editscreen.SpotlightEditPacket
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.types.EditScreenMenu
import com.flooferland.showbiz.types.EditScreenOwner
import com.flooferland.showbiz.types.MappedBits
import com.flooferland.showbiz.types.OwnerId
import com.flooferland.showbiz.types.connection.ConnectionManager
import com.flooferland.showbiz.types.connection.IConnectable
import com.flooferland.showbiz.types.connection.PortDirection
import com.flooferland.showbiz.types.connection.data.PackedShowData
import com.flooferland.showbiz.types.light.ILightEmitter
import com.flooferland.showbiz.types.light.LightManager
import com.flooferland.showbiz.types.light.SpotLight
import com.flooferland.showbiz.types.math.Color4
import com.flooferland.showbiz.types.math.Kelvin
import com.flooferland.showbiz.types.math.Vec2f
import com.flooferland.showbiz.utils.Extensions.getBooleanOrNull
import com.flooferland.showbiz.utils.Extensions.getFloatOrNull
import com.flooferland.showbiz.utils.Extensions.getIntOrNull
import com.flooferland.showbiz.utils.Extensions.markDirtyNotifyAll
import com.flooferland.showbiz.utils.lerp
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.redstone.Redstone
import software.bernie.geckolib.animatable.GeoBlockEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

class SpotlightBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.Spotlight.entityType!!, pos, blockState), IConnectable, ILightEmitter, GeoBlockEntity, EditScreenOwner<SpotlightEditPacket> {
    override var menuData = EditScreenMenu.EditScreenBuf(OwnerId.of(blockPos))
    override val connectionManager = ConnectionManager(this)
    val show = connectionManager.port("show", PackedShowData(), PortDirection.In) { show ->
        lit = bitFilter.chartHasBit(show.mapping) { show.signal.frameHas(it) }
    }

    override val lightManager = LightManager(this)
    val light = lightManager.add(SpotLight())

    val isLit: Boolean get() = lit || redstoneSignal > 0
    var clientValue: Float = 0f  // Used for smoothing on the client
    var redstoneSignal: Int = 0
    var supportAbove = false
    var supportBelow = false

    val bitFilter = MappedBits()
    var turn = Vec2f.ZERO
    var angle = 45f
    var shadows: Boolean = true
    var kelvin: Int = 3200
    var brightness: Float = 1.0f

    private var lit: Boolean = false
    var clientStartPos = blockPos.center!!
    var clientEndPos = blockPos.center!!

    val geckoCache = GeckoLibUtil.createInstanceCache(this)!!

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) = Unit
    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = geckoCache

    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        if (show.data.playing || isLit) {
            markDirtyNotifyAll()
        }
    }

    override fun clientUpdateLights(delta: Float) {
        val kelvinColor = Kelvin.toColor(kelvin)
        val power = if (redstoneSignal > Redstone.SIGNAL_NONE) redstoneSignal / Redstone.SIGNAL_MAX.toFloat() else 1f

        val target = (if (isLit) power else 0f) * brightness
        val fadingOut = target < clientValue
        val speed = 0.2f - (if (fadingOut) 0.03f else 0.0f)
        clientValue = lerp(clientValue, target, speed * delta)
        clientValue = clientValue.coerceIn(if (isLit) 0.5f else 0f, 1f)

        light?.enabled = clientValue >= 0f
        light?.range = 20f
        light?.angle = angle
        light?.shadows = shadows
        light?.intensity = clientValue
        light?.color = Color4.fromPacked(kelvinColor)
        light?.localPos = clientEndPos.subtract(blockPos.center)
        light?.dir = clientEndPos.subtract(clientStartPos).normalize()
    }

    override fun getDisplayName() = Component.literal("Spotlight")!!
    override fun createMenu(i: Int, inventory: Inventory, player: Player): AbstractContainerMenu? {
        val player = player as? ServerPlayer ?: return null
        return SpotlightEditMenu(i, getScreenOpeningData(player))
    }
    override fun getScreenOpeningData(player: ServerPlayer) =
        SpotlightEditPacket(EditScreenMenu.EditScreenBuf(OwnerId.of(worldPosition)), bitFilter, turn, angle, shadows, kelvin, brightness)

    fun applyPacket(packet: SpotlightEditPacket) {
        menuData = packet.base
        bitFilter.set(packet.bitFilter)
        turn = packet.turn
        angle = packet.angle
        kelvin = packet.kelvin
        brightness = packet.brightness
        shadows = packet.shadows
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        connectionManager.load(tag)
        bitFilter.load(tag, "bit_filter")
        tag.getBooleanOrNull("lit")?.let { lit = it }
        tag.getBooleanOrNull("shadows")?.let { shadows = it }
        tag.getFloatOrNull("turn_x")?.let { turn.x = it }
        tag.getFloatOrNull("turn_y")?.let { turn.y = it }
        tag.getFloatOrNull("angle")?.let { angle = it }
        tag.getFloatOrNull("brightness")?.let { brightness = it }
        tag.getIntOrNull("kelvin")?.let { kelvin = it }

        // Stuff that can be recalculated if its lost
        tag.getIntOrNull("redstone_signal")?.let { redstoneSignal = it }
        tag.getBooleanOrNull("support_above")?.let { supportAbove = it }
        tag.getBooleanOrNull("support_below")?.let { supportBelow = it }
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        connectionManager.save(tag)
        bitFilter.save(tag, "bit_filter")
        tag.putBoolean("lit", lit)
        tag.putBoolean("shadows", shadows)
        tag.putFloat("turn_x", turn.x)
        tag.putFloat("turn_y", turn.y)
        tag.putFloat("angle", angle)
        tag.putFloat("brightness", brightness)
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