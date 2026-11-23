package com.flooferland.showbiz.handbook

import net.minecraft.network.chat.Component

data class HandbookEntry(
    val summary: HandbookXml.Element.Summary,
    val facts: List<Component>,
    val pages: List<HandbookXml.Page>,
    val styles: Map<String, HandbookXml.StyleDefs>
)
