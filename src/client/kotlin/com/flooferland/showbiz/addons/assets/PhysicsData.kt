package com.flooferland.showbiz.addons.assets

import com.akuleshov7.ktoml.Toml
import com.flooferland.showbiz.types.PneumaticValve
import com.flooferland.showbiz.types.physics.ChainSolver
import com.flooferland.showbiz.types.physics.DynBone
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

// A bot's physics.toml, one loader for every system: [chains], [cylinders.<mapping>] and [bits.<mapping>] for the cylinders,
// [hits.<mapping>] for their thunks, [dynbones."<bone>"] for the floppy parts
@Serializable
data class PhysicsData(
    val format: Int = 1,
    val chains: Map<String, ChainSolver.Params> = emptyMap(),
    val cylinders: Map<String, PneumaticValve.Supply> = emptyMap(),
    val bits: Map<String, Map<String, PneumaticValve.Params>> = emptyMap(),
    val hits: Map<String, PneumaticValve.Hit> = emptyMap(),
    val dynbones: Map<String, DynBone.Params> = emptyMap()
) {
    companion object {
        private val toml: Toml = Toml
        fun readOrThrow(text: String): PhysicsData = toml.decodeFromString<PhysicsData>(text)
    }
}