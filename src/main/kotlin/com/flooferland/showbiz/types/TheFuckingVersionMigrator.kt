package com.flooferland.showbiz.types

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents

object VersionMigrator {
    val queuedMigrations = ArrayDeque<() -> Unit>()
    fun register() {
        ServerTickEvents.END_SERVER_TICK.register {
            while (queuedMigrations.isNotEmpty()) {
                queuedMigrations.removeFirst()()
            }
        }
    }
}
