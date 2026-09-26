package com.flooferland.showbiz.registry

import net.minecraft.client.*
import com.flooferland.showbiz.Showbiz.MOD_ID
import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import org.lwjgl.glfw.GLFW.GLFW_KEY_W

enum class ModClientInput(id: String, type: InputConstants.Type, key: Int) {
    OpenInHandbook("open_in_handbook", InputConstants.Type.KEYSYM, GLFW_KEY_W)
    ;

    val mapping: KeyMapping = KeyBindingHelper.registerKeyBinding(
        KeyMapping(
            "key.$MOD_ID.$id",
            type,
            key,
            "category.$MOD_ID"
        )
    )
}