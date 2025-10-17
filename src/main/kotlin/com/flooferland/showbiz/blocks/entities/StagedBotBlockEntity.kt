package com.flooferland.showbiz.blocks.entities

import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.utils.Extensions.getBooleanOrNull
import net.minecraft.core.*
import net.minecraft.nbt.*
import net.minecraft.network.protocol.game.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.*
import software.bernie.geckolib.animatable.GeoBlockEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

class StagedBotBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.StagedBot.entity!!, pos, blockState), GeoBlockEntity {
    val cache = GeckoLibUtil.createInstanceCache(this)!!

    public var playing = false

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) = Unit
    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = cache

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putBoolean("playing", playing)
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        playing = tag.getBooleanOrNull("playing") ?: false
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = CompoundTag()
        saveAdditional(tag, registries)
        return tag
    }

    override fun getUpdatePacket() =
        ClientboundBlockEntityDataPacket.create(this)!!
}