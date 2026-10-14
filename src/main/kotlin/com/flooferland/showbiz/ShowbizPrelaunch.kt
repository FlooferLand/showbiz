package com.flooferland.showbiz

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint
import net.fabricmc.loader.impl.FormattedException
import kotlin.jvm.optionals.getOrNull

class ShowbizPrelaunch : PreLaunchEntrypoint {
    override fun onPreLaunch() {
        when (FabricLoader.getInstance().environmentType) {
            EnvType.CLIENT -> onClientPreLaunch()
            EnvType.SERVER -> {}
        }
    }

    @Environment(EnvType.CLIENT)
    fun onClientPreLaunch() {
        ensureDependency("irl-redactor")
    }

    @Environment(EnvType.CLIENT)
    fun ensureDependency(depId: String) {
        val container = FabricLoader.getInstance().getModContainer(Showbiz.MOD_ID).orElseThrow()
        val depVersion = container.metadata.dependencies.firstOrNull { it.modId == depId }?.versionRequirements

        val dep = FabricLoader.getInstance().getModContainer(depId).getOrNull()
        if (dep != null) {
            val version = dep.metadata.version
            val matches = depVersion?.any { it.test(version) } ?: false
            if (!matches) {
                throw FormattedException(
                    "Wrong IRLights version",
                    "The Showbiz mod requires IRLights in the version range $depVersion (found $version)"
                )
            }
        } else {
            val mod = depId + (depVersion?.let { " in the version range $it" } ?: "")
            throw FormattedException(
                "Missing dependency",
                "The Showbiz mod requires the IRLights $mod"
            )
        }
    }
}