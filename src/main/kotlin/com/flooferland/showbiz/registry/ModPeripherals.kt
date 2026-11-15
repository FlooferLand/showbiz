package com.flooferland.showbiz.registry

import com.flooferland.showbiz.peripherals.GreyboxPeripheral
import dan200.computercraft.api.ComputerCraftAPI

/** CC Tweaked integration */
object ModPeripherals {
    fun register() {
        ComputerCraftAPI.registerGenericSource(GreyboxPeripheral())
    }
}