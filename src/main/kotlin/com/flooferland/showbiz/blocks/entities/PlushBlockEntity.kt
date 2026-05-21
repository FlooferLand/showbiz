package com.flooferland.showbiz.blocks.entities

import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.utils.Extensions.getOrNull
import software.bernie.geckolib.animatable.GeoBlockEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil
import kotlin.jvm.optionals.getOrNull

class PlushBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlocks.Plush.entityType, pos, blockState), GeoBlockEntity {
    val geckoCache = GeckoLibUtil.createInstanceCache(this)!!
    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) = Unit
    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = geckoCache

    var stack: ItemStack? = null

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        stack?.save(registries)?.let { tag.put("item", it) }
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        stack = tag.getOrNull("item")?.let { ItemStack.parse(registries, it).getOrNull() }
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = super.getUpdateTag(registries)
        saveAdditional(tag, registries)
        return tag
    }

    override fun getUpdatePacket() = ClientboundBlockEntityDataPacket.create(this)!!
}