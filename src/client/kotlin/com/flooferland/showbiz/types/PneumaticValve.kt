package com.flooferland.showbiz.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.abs

/**
 * Simulates a single pneumatic air cylinder, which is how the real bots actually move.
 *
 * Gives you spool-up, different speeds going in and out, and a bounce off the endstops.
 * Faz-Anim's cylinder step, quirks included; every value comes from the physics.toml ([Params])
 * Steps [Supply.rate] times a second, a frame draws between the last two steps ([drawn])
 *
 * Ported from Faz-Anim's Character_Valves, credit to https://github.com/The64thGamer/Faz-Anim
 */
class PneumaticValve {
    var pos: Float = 0f
        public get
        private set

    // where the last step started, so a frame can draw between steps
    private var lastPos: Float = 0f

    // How fast the last step hit an end, in strokes a second (0 if it didn't), for its thunk
    var hit: Float = 0f
        public get
        private set

    //air spooling up. Only one direction is ever live, the other gets zeroed every step
    private var heavyOut: Float = 0f
    private var heavyIn: Float = 0f

    //goes negative to kick the cylinder back off an endstop, then climbs back up to 1
    private var invert: Float = 1f
    private var smashIter: Int = 1
    private var lastState: Boolean = false

    // Stepping once. [dt60] is the step length in 60 fps frames, 1 at 60 steps a second
    fun step(on: Boolean, dt60: Float, psi: Float, dualPressure: Boolean, params: Params) {
        lastPos = pos
        var next = pos
        var drive = psi / 1050f * params.psiScale * dt60
        if (dualPressure) drive *= params.dualPressureScale

        val smash: Float
        val smashSpeed: Float
        if (on) {
            //extending
            heavyOut = minOf(heavyOut + params.flowOut * params.flowOut / 2f, 1f + (1f - params.flowOut) * 0.3f)
            heavyIn = 0f
            next += drive * params.flowOut * heavyOut * params.gravityOut * invert
            smash = params.smashOut
            smashSpeed = params.smashSpeedOut
        } else {
            //retracting
            heavyIn = minOf(heavyIn + params.flowIn * params.flowIn / 2f, 1f + (1f - params.flowIn) * 0.3f)
            heavyOut = 0f
            next -= drive * params.flowIn * heavyIn * params.gravityIn * invert
            smash = params.smashIn
            smashSpeed = params.smashSpeedIn
        }

        if (lastState != on) {
            lastState = on
            smashIter = 1
            invert = 1f
        }
        if (invert < 1f) invert = minOf(invert + dt60 * smashSpeed, 1f)

        if (next < 0f && smash != 0f && smashIter < 4) {
            invert = -smash * ((abs(next + 1f) / smashIter.toFloat()) / dt60)
            smashIter++
        }
        if (next > 1f && smash != 0f && smashIter < 4) {
            invert = -smash * ((next / smashIter.toFloat()) / dt60)
            smashIter++
        }
        hit = if ((next >= 1f && lastPos < 1f) || (next <= 0f && lastPos > 0f)) abs(next - lastPos) * 60f / dt60 else 0f
        pos = minOf(maxOf(next, 0f), 1f)
    }

    fun drawn(alpha: Float): Float = lastPos + (pos - lastPos) * alpha

    @Serializable
    data class Supply(
        val psi: Float,     // the air supply, 80 in Faz-Anim
        val rate: Float     // steps a second
    )

    // One bit's cylinder, a line of a [bits.<mapping>] table. No defaults here, every value comes from the physics.toml
    @Serializable
    data class Params(
        @SerialName("flow_in") val flowIn: Float,                         // air flow going in (bit off): how fast it spools up and travels
        @SerialName("flow_out") val flowOut: Float,                       // air flow going out (bit on): how fast it spools up and travels
        @SerialName("gravity_in") val gravityIn: Float,                   // a scale on the speed going in: a heavy limb falls faster than it lifts
        @SerialName("gravity_out") val gravityOut: Float,                 // a scale on the speed going out
        @SerialName("psi_scale") val psiScale: Float,                     // this cylinder's share of the supply's psi
        @SerialName("smash_in") val smashIn: Float,                       // the bounce off the in end, 0 is none
        @SerialName("smash_out") val smashOut: Float,                     // the bounce off the out end, 0 is none
        @SerialName("smash_speed_in") val smashSpeedIn: Float,            // how fast it recovers from a bounce off the in end
        @SerialName("smash_speed_out") val smashSpeedOut: Float,          // how fast it recovers from a bounce off the out end
        @SerialName("dual_pressure_bit") val dualPressureBit: Int,        // while this bit is on, psi x dual_pressure_scale. 0: none
        @SerialName("dual_pressure_scale") val dualPressureScale: Float,  // the psi scale while dual_pressure_bit is on
        val curve: Curve                                                  // how the bones follow the cylinder, see Curve
    )

    // One [hits.<mapping>] table: the thunk at either end of a stroke, louder the faster it hits. [bits] overrides it per bit
    @Serializable
    data class Hit(
        @SerialName("sound_out") val soundOut: String,       // a sound id, when it hits the out end
        @SerialName("sound_in") val soundIn: String,         // when it hits the in end
        val volume: Float,                                   // at full_speed or faster
        val pitch: Float,                                    // 1 is the sound as recorded
        @SerialName("pitch_spread") val pitchSpread: Float,  // up to this much off the pitch, at random, so a row of hits doesn't sound copied
        @SerialName("min_speed") val minSpeed: Float,        // strokes a second, slower hits make no sound
        @SerialName("full_speed") val fullSpeed: Float,      // strokes a second, this fast or faster is full volume
        val bits: Map<String, HitBit> = emptyMap()           // one bit's changes, a line of [hits.<mapping>.bits] each
    )

    // One bit's changes to its [Hit]: whatever is given replaces the mapping's
    @Serializable
    data class HitBit(
        @SerialName("sound_out") val soundOut: String? = null,
        @SerialName("sound_in") val soundIn: String? = null,
        val volume: Float? = null,
        val pitch: Float? = null,
        @SerialName("pitch_spread") val pitchSpread: Float? = null,
        @SerialName("min_speed") val minSpeed: Float? = null,
        @SerialName("full_speed") val fullSpeed: Float? = null
    )

    // From the cylinder's position to how far the bones go
    @Serializable
    enum class Curve {
        @SerialName("smooth") Smooth,   // Faz-Anim's animation clips ease like this
        @SerialName("linear") Linear,   // the bones follow the cylinder as it is
        @SerialName("bitmap") Bitmap    // the bitmap's own flow easing
    }
}