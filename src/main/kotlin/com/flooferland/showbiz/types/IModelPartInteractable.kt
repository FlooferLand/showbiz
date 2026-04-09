package com.flooferland.showbiz.types

import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level

interface IModelPartInteractable {
    val modelPartInstance: ModelPartInstance
    fun getInteractionMapping(): Map<String, Int>
    fun getNameMapping(): Map<Int, String>
    fun onInteract(key: Int, level: Level, player: Player)
}