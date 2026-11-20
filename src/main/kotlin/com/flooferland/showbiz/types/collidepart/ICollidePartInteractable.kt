package com.flooferland.showbiz.types.collidepart

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity

interface ICollidePartInteractable {
    val collidePartInstance: CollidePartInstance

    open fun getInteractionMapping(): Map<String, Int> = mapOf()
    open fun getNameMapping(): Map<Int, String> = mapOf()
    open fun onInteract(key: Int, level: Level, player: Player) {}

    fun partGetLevel(): Level? = when (this) {
        is Entity -> this.level()
        is BlockEntity -> this.getLevel()
        else -> null
    }
}