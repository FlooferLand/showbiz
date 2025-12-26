package com.flooferland.showbiz

import com.flooferland.showbiz.screens.ShowbizConfigScreen
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi

object ShowbizModMenu : ModMenuApi {
    override fun getModConfigScreenFactory() = ConfigScreenFactory { parent ->
        ShowbizConfigScreen(parent)
    }
}