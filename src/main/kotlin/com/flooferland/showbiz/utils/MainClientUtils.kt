package com.flooferland.showbiz.utils

import net.minecraft.world.entity.*
import net.minecraft.world.level.*
import java.util.UUID

/**
 * Contains stuff that normally can't be accessed from the main sourceset.
 * Safe way to call client-side code. Everything returns null if it's not running on the client
 */
object MainClientUtils {
    var getEntityByUuid: (level: Level, uuid: UUID) -> Entity? = { level, uuid -> null }
}