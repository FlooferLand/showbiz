package com.flooferland.showbiz.registry

import com.flooferland.showbiz.utils.Extensions.forceLoad
import com.flooferland.showbiz.utils.rl
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent

/// NOTE: Data generator should be called after adding new sounds here
enum class ModSounds {
    Select("select", folder = "interface"),
    Deselect("deselect", folder = "interface"),
    End("end", folder = "interface"),
    PneumaticFire("pneumatic_fire"),
    PneumaticRelease("pneumatic_release"),
    ReelEnter("reel_enter"),
    ReelExit("reel_exit"),
    ReelPlay("reel_play"),
    HihatClosed("hihat_closed"),
    HihatOpen("hihat_open"),
    Cymbal("cymbal"),
    Snare("snare"),
    Kick("kick"),
    EnderEarl("ender_earl"),
    Boop("boop"),
    Honk("honk"),
    HonkBye("honk_bye"),
    DoorOpen("door_open"),
    DoorClose("door_close")
    ;

    constructor(name: String, folder: String? = null, sounds: Array<String> = arrayOf(name), procedural: Boolean = false) {
        this.id = rl(if (folder == null) name else "$folder.$name")
        this.event = SoundEvent.createVariableRangeEvent(this.id)
        this.sounds = sounds.map { rl(it) }.toTypedArray()
        this.folder = folder
        this.procedural = procedural
        Registry.register(BuiltInRegistries.SOUND_EVENT, this.id, this.event)
    }

    val id: ResourceLocation
    val event: SoundEvent
    val sounds: Array<ResourceLocation>
    val folder: String?
    val procedural: Boolean

    companion object {
        fun register() {
            ModSounds::class.forceLoad()
        }
    }
}