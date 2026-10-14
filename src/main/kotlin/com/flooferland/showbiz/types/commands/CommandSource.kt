package com.flooferland.showbiz.types.commands

import net.minecraft.commands.*
import net.minecraft.server.*
import net.minecraft.server.level.*
import net.minecraft.world.phys.*

class CommandSource(val inner: CommandSourceStack) {
    val position: Vec3? = inner.position
    val server: MinecraftServer = inner.server

    /** Null if running a command from the server console */
    val player: ServerPlayer? = inner.player
}