package com.flooferland.showbiz.types.commands

import net.minecraft.commands.*
import net.minecraft.server.*
import net.minecraft.world.entity.*
import net.minecraft.world.phys.*

class CommandSource(val inner: CommandSourceStack) {
    val position: Vec3? = inner.position
    val entity: Entity? = inner.entity
    val server: MinecraftServer = inner.server
}