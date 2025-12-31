package com.flooferland.showbiz.blocks.entities

import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.types.IModelPartInteractable
import com.flooferland.showbiz.types.ModelPartManager
import com.flooferland.showbiz.types.connection.ConnectionManager
import com.flooferland.showbiz.types.connection.IConnectable
import software.bernie.geckolib.animatable.GeoBlockEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

class ShowSelectorBlockEntity(pos: BlockPos, state: BlockState) : BlockEntity(ModBlocks.ShowSelector.entityType!!, pos, state), IConnectable, IModelPartInteractable, GeoBlockEntity {
    override val connectionManager = ConnectionManager(this)
    val cache = GeckoLibUtil.createInstanceCache(this)!!

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) = Unit
    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = cache

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        connectionManager.save(tag)
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        connectionManager.load(tag)
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = super.getUpdateTag(registries)
        saveAdditional(tag, registries)
        return tag
    }

    override fun getUpdatePacket() = ClientboundBlockEntityDataPacket.create(this)!!

    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        modelPartInstance.tick(level, pos, state)
    }

    override fun setRemoved() {
        modelPartInstance.kill()
    }

    override val modelPartInstance = ModelPartManager.create(this, ModBlocks.ShowSelector)
    override fun getInteractionMapping() = hashMapOf<String, Int>().also { map ->
        (0..12).forEach { map["buttonOn/$it"] = it }
    }
    override fun onInteract(key: Int, level: Level, player: Player) {
        player.sendSystemMessage(Component.literal("Key $key interacted"))
        println("Interacted with key $key")
    }
}