package com.flooferland.showbiz.datagen.providers

import com.flooferland.showbiz.registry.ModMusicDiscs
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.DurationUnit

/** https://minecraft.wiki/w/Jukebox_song_definition */
object JukeboxSongsProvider {
    fun generateSong(disc: ModMusicDiscs) = buildJsonObject {
        put("comparator_output", disc.ordinal % 15)
        put("description", disc.title)
        put("length_in_seconds", disc.length.toDouble(DurationUnit.SECONDS))
        put("sound_event", disc.id.toString())
    }
}