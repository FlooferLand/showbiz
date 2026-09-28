package com.flooferland.showbiz.handbook

import net.minecraft.network.chat.*

data class HandbookEntry(val summary: Component, val facts: List<Component>, val pages: List<HandbookXml.Page>)
