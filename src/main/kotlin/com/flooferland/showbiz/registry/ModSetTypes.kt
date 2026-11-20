package com.flooferland.showbiz.registry

import com.flooferland.showbiz.utils.Extensions.forceLoad
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.properties.BlockSetType

enum class ModSetTypes(val type: BlockSetType) {
    ShowbizWood(BlockSetType(
        "showbiz",
        true,
        true,
        true,
        BlockSetType.PressurePlateSensitivity.EVERYTHING,
        SoundType.WOOD,
        ModSounds.DoorClose.event,
        ModSounds.DoorOpen.event,
        SoundEvents.WOODEN_TRAPDOOR_CLOSE,
        SoundEvents.WOODEN_TRAPDOOR_OPEN,
        SoundEvents.WOODEN_PRESSURE_PLATE_CLICK_OFF,
        SoundEvents.WOODEN_PRESSURE_PLATE_CLICK_ON,
        SoundEvents.WOODEN_BUTTON_CLICK_OFF,
        SoundEvents.WOODEN_BUTTON_CLICK_ON
    ))
    ;

    companion object {
        fun register() {
            ModSetTypes::class.forceLoad()
        }
    }
}