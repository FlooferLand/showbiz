package com.flooferland.showbiz.blocks.entities

import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.network.protocol.game.*
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.types.IBotSoundHandler
import com.flooferland.showbiz.types.connection.ConnectionManager
import com.flooferland.showbiz.types.connection.IConnectable
import com.flooferland.showbiz.types.connection.PortDirection
import com.flooferland.showbiz.types.connection.data.PackedShowData
import com.flooferland.showbiz.utils.Extensions.getStringOrNull
import software.bernie.geckolib.animatable.GeoBlockEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

class StagedBotBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.StagedBot.entityType!!, pos, blockState), GeoBlockEntity, IConnectable {
    override val connectionManager = ConnectionManager(this)
    val show = connectionManager.port("show", PackedShowData(), PortDirection.In)

    val cache = GeckoLibUtil.createInstanceCache(this)!!
    var botId: String? = findFirstBot()

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) = Unit
    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = cache

    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        soundHandler?.tick(this, level, pos, state)
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        connectionManager.save(tag)

        if (level?.isClientSide == false) {
            botId?.let { tag.putString("bot_id", it) }
        }
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        connectionManager.load(tag)

        var botId = tag.getStringOrNull("bot_id")
        if (botId == null && level?.isClientSide == false) {
            botId = findFirstBot()
        }
        botId?.let { this.botId = it }
    }

    fun findFirstBot() = Showbiz.addons.firstOrNull()?.bots?.entries?.firstOrNull()?.key

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag? {
        val tag = super.getUpdateTag(registries)
        saveAdditional(tag, registries)
        return tag
    }

    override fun getUpdatePacket() = ClientboundBlockEntityDataPacket.create(this)!!

    companion object {
        var soundHandler: IBotSoundHandler? = null
    }
}