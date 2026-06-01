package com.flooferland.showbiz.registry

import com.flooferland.showbiz.blocks.entities.GreyboxBlockEntity
import com.flooferland.showbiz.peripherals.GreyboxPeripheral
import dan200.computercraft.api.peripheral.PeripheralLookup

/** CC Tweaked integration */
object ModPeripherals {
    fun register() {
        PeripheralLookup.get().registerForBlockEntity(
            { blockEntity, side -> GreyboxPeripheral(blockEntity as GreyboxBlockEntity) },
            ModBlocks.Greybox.entityType
        )
    }
}