package com.flooferland.showbiz.registry

import com.flooferland.showbiz.Showbiz.MOD_ID
import com.flooferland.showbiz.utils.Extensions.forceLoad
import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import org.lwjgl.glfw.GLFW.GLFW_KEY_H

enum class ModClientInput(id: String, type: InputConstants.Type, key: Int) {
    OpenInHandbook("open_in_handbook", InputConstants.Type.KEYSYM, GLFW_KEY_H)
    ;

    val mapping: KeyMapping = KeyBindingHelper.registerKeyBinding(
        KeyMapping(
            "key.$MOD_ID.$id",
            type,
            key,
            "category.$MOD_ID"
        )
    )

    companion object {
        fun register() {
            ModClientInput::class.forceLoad()
        }
    }
}