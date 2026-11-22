package com.flooferland.showbiz.blocks.entities

import com.flooferland.showbiz.Permissions.Companion.mayInteractWith
import com.flooferland.showbiz.items.ReelItem
import com.flooferland.showbiz.registry.ModBlocks
import com.flooferland.showbiz.registry.ModSounds
import com.flooferland.showbiz.types.collidepart.CollidePartId
import com.flooferland.showbiz.types.collidepart.CollidePartManager
import com.flooferland.showbiz.types.collidepart.ICollidePartInteractable
import com.flooferland.showbiz.utils.Sounds
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.Container
import net.minecraft.world.ContainerHelper
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import software.bernie.geckolib.animatable.GeoBlockEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

class ReelHolderBlockEntity(pos: BlockPos, state: BlockState) : BlockEntity(ModBlocks.ReelHolder.entityType!!, pos, state), Container, ICollidePartInteractable, GeoBlockEntity {
    val cache = GeckoLibUtil.createInstanceCache(this)!!
    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) = Unit
    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = cache

    override val collidePartInstance = CollidePartManager.create(this) {
        (0..6).forEach { map("reel$it", CollidePartId.Kick) }
    }

    val inventory = NonNullList.withSize(7, ItemStack.EMPTY)!!

    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        collidePartInstance.tick()
    }

    override fun setRemoved() {
        super.setRemoved()
        collidePartInstance.kill()
    }

    override fun getInteractionMapping() = hashMapOf<String, Int>().also { map ->
        (0..6).forEach { map["reel$it"] = it }
    }

    override fun getNameMapping() = hashMapOf<Int, String>().also { map ->
        (0..6).forEach { map[it] = if (getItem(it).isEmpty) "Empty" else getItem(it).displayName.string }
    }

    override fun onInteract(key: Int, level: Level, player: Player) {
        val slot = key
        if (slot !in 0 until containerSize) return
        if (!player.mayInteractWith(this)) return

        val currentStack = getItem(slot)
        val hand = player.usedItemHand ?: InteractionHand.MAIN_HAND
        val heldItem = player.getItemInHand(hand)

        if (!currentStack.isEmpty && heldItem.item is ReelItem) {
            player.setItemInHand(hand, currentStack)
            setItem(slot, heldItem)
        } else if (currentStack.isEmpty && heldItem.item is ReelItem) {
            setItem(slot, heldItem)
            player.setItemInHand(hand, ItemStack.EMPTY)
        } else if (!currentStack.isEmpty && heldItem.isEmpty) {
            player.setItemInHand(hand, currentStack)
            setItem(slot, ItemStack.EMPTY)
        }

        Sounds.serverPlayer(player).play(ModSounds.ReelPlay)
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        ContainerHelper.saveAllItems(tag, inventory, registries)
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        inventory.clear()
        ContainerHelper.loadAllItems(tag, inventory, registries)
    }

    // region | Container
    override fun getContainerSize() = inventory.size
    override fun getMaxStackSize() = 1
    override fun isEmpty() =
        inventory.all { it.isEmpty }
    override fun getItem(slot: Int): ItemStack =
        inventory.getOrElse(slot) { ItemStack.EMPTY!! }
    override fun canPlaceItem(slot: Int, stack: ItemStack) =
        (slot < containerSize) && stack.item is ReelItem
    override fun setItem(slot: Int, stack: ItemStack) {
        inventory[slot] = stack
        stack.limitSize(getMaxStackSize(stack))
        setChanged()
    }
    override fun removeItem(slot: Int, amount: Int): ItemStack {
        val itemStack = ContainerHelper.removeItem(inventory, slot, amount)
        if (!itemStack.isEmpty()) setChanged()
        return itemStack
    }
    override fun removeItemNoUpdate(slot: Int): ItemStack =
        ContainerHelper.takeItem(inventory, slot);
    override fun stillValid(player: Player): Boolean =
        Container.stillValidBlockEntity(this, player)
    override fun clearContent() {
        inventory.fill(ItemStack.EMPTY)
        setChanged()
    }
    // endregion
}