package com.flooferland.showbiz.blocks.entities

import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.show.SignalFrame
import com.flooferland.showbiz.types.connection.ConnectionManager
import com.flooferland.showbiz.types.connection.IConnectable
import com.flooferland.showbiz.types.connection.Ports
import com.flooferland.showbiz.utils.Extensions.getIntArrayOrNull
import com.flooferland.showbiz.utils.Extensions.getStringOrNull
import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.network.protocol.game.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import software.bernie.geckolib.animatable.GeoBlockEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

class StagedBotBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.StagedBot.entity!!, pos, blockState), GeoBlockEntity, IConnectable {
    override val connectionManager = ConnectionManager(this) {
        bind(Ports.PlayingIn) { isPlaying = it }
        bind(Ports.SignalIn) { signalFrame.load(it.save()) }
    }
    val signalFrame = SignalFrame()
    var isPlaying: Boolean = false

    val cache = GeckoLibUtil.createInstanceCache(this)!!
    var botId: String? = findFirstBot()

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) = Unit
    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = cache

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        connectionManager.save(tag)

        tag.putBoolean("Is-Playing", isPlaying)
        tag.putIntArray("Signal-Frame", signalFrame.save())

        if (level?.isClientSide == false) {
            botId?.let { tag.putString("Bot-Id", it) }
        }
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        connectionManager.load(tag)

        isPlaying = tag.getBoolean("Is-Playing")
        tag.getIntArrayOrNull("Signal-Frame")?.let { signalFrame.load(it) }

        var botId = tag.getStringOrNull("Bot-Id")
        if (botId == null && level?.isClientSide == false) {
            botId = findFirstBot()
        }
        botId?.let { this.botId = it }
    }

    fun findFirstBot() = Showbiz.addons.firstOrNull()?.bots?.entries?.firstOrNull()?.key

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = CompoundTag()
        saveAdditional(tag, registries)
        return tag
    }

    override fun getUpdatePacket() =
        ClientboundBlockEntityDataPacket.create(this)!!
}