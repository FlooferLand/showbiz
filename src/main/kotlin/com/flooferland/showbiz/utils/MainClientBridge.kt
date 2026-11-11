package com.flooferland.showbiz.utils

import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import java.util.*

/**
 * Contains stuff that normally can't be accessed from the main sourceset.
 * Safe way to call client-side code. Every callback does nothing if it's not running on the client
 */
object MainClientBridge {
    var getEntityByUuid: (level: Level, uuid: UUID) -> Entity? = { level, uuid -> null }
    var reloadPacks: (Set<String>) -> Unit = {}
}