package com.flooferland.showbiz.types

import net.minecraft.core.*
import net.minecraft.world.level.block.state.*

interface IRedstoneExtras {
    fun wireShouldConnectTo(state: BlockState, direction: Direction): Boolean
}