package com.flooferland.showbiz.registry

import net.minecraft.core.*
import net.minecraft.core.component.*
import net.minecraft.core.registries.*
import net.minecraft.resources.*
import net.minecraft.sounds.*
import net.minecraft.world.item.*
import com.flooferland.showbiz.items.base.MusicDiscItem
import com.flooferland.showbiz.utils.Extensions.secsToTicks
import com.flooferland.showbiz.utils.rl
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

/// NOTE: Data generator should be called after adding new discs here
enum class ModMusicDiscs(val key: String, val title: String, val length: Duration, lyrics: Map<Duration, String> = mapOf()) {
    Aero("aero", "FlooferLand - Aero",
        1.minutes + 22.seconds
    ),
    LockedUp("lockedup", "FlooferLand - Locked Up",
        1.minutes + 26.seconds,
        mapOf(
            (19.seconds) to "Unify my heartbeat",
            (21.seconds) to "Stop.",
            (22.seconds) to "Let's try again",
            (24.seconds) to "Put down the bits he wrote",
            (26.5.seconds) to "Don't let corporate hands dissect it",
            (28.5.seconds) to "My last breaths of pumping air",
            (31.seconds) to "Not seeking a home elsewhere",
            (33.5.seconds) to "Unify my heartbeat",
            (35.5.seconds) to "Stop.",
            (37.seconds) to "Let's try again",
            (38.seconds) to "",
        )
    ),
    // Synthy("synthy", "FlooferLand - Synthy", 1.minutes + 26.seconds),
    Y2K("y2k", "FlooferLand - Y2K",
        1.minutes
    ),
    ;

    val id = rl("music_disc_$key")
    val keyId = rl(key)
    val event = SoundEvent.createVariableRangeEvent(id)!!
    val item = MusicDiscItem(
        this,
        Item.Properties()
            .stacksTo(1)
            .rarity(Rarity.RARE)
            .jukeboxPlayable(ResourceKey.create(Registries.JUKEBOX_SONG, id))
            .component(DataComponents.RARITY, Rarity.RARE)
    )
    val lyrics = lyrics.mapKeys { it.key.toDouble(DurationUnit.SECONDS).secsToTicks().toLong() }

    init {
        Registry.register(BuiltInRegistries.SOUND_EVENT, id, event)
        Registry.register(BuiltInRegistries.ITEM, id, item)
    }

    fun getLyric(tick: Long): String {
        val key = lyrics.keys.filter { it <= tick }.maxOrNull() ?: return ""
        return lyrics[key] ?: ""
    }
}