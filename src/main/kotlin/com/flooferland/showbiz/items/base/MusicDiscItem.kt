package com.flooferland.showbiz.items.base

import net.minecraft.network.chat.*
import net.minecraft.network.chat.Component.*
import net.minecraft.world.*
import net.minecraft.world.entity.player.*
import net.minecraft.world.item.*
import net.minecraft.world.level.*
import com.flooferland.showbiz.Showbiz.MOD_ID
import com.flooferland.showbiz.registry.ModMusicDiscs

class MusicDiscItem(val disc: ModMusicDiscs, properties: Item.Properties) : Item(properties) {
    override fun getName(stack: ItemStack) = getDisplayName()
    fun getDisplayName() = translatable("item.$MOD_ID.music_disc")!!

    override fun use(level: Level, player: Player, usedHand: InteractionHand): InteractionResultHolder<ItemStack?>? {
        player.displayClientMessage(Component.literal("You need a jukebox!"), true)
        return super.use(level, player, usedHand)
    }
}