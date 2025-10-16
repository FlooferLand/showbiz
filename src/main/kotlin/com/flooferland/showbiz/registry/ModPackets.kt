package com.flooferland.showbiz.registry

import com.flooferland.showbiz.utils.rl
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.resources.*

enum class ModPackets(name: String) {
    ;

    val id: ResourceLocation = rl(name)

    companion object {
        fun registerS2C() {

        }

        @Environment(EnvType.CLIENT)
        fun registerC2S() {

        }
    }
}