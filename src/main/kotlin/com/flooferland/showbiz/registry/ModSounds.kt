package com.flooferland.showbiz.registry

import com.flooferland.showbiz.datagen.DataGenerator
import com.flooferland.showbiz.utils.rl
import net.minecraft.core.*
import net.minecraft.core.registries.*
import net.minecraft.resources.*
import net.minecraft.sounds.*

enum class ModSounds {
    Select("select", folder = "interface"),
    Deselect("deselect", folder = "interface"),
    End("end", folder = "interface"),
    ;

    constructor(name: String, folder: String = "", sounds: Array<String> = arrayOf(name)) {
        this.id = rl(if (folder.isEmpty()) name else "$folder.$name")
        this.event = SoundEvent.createVariableRangeEvent(this.id)
        this.sounds = sounds
        this.folder = folder
        if (!DataGenerator.engaged) {
            Registry.register(BuiltInRegistries.SOUND_EVENT, this.id, this.event)
        }
    }

    val id: ResourceLocation
    val event: SoundEvent
    val sounds: Array<String>
    val folder: String
}