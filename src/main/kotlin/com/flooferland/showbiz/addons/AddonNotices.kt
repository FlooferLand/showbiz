package com.flooferland.showbiz.addons

import com.flooferland.showbiz.Showbiz
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

object AddonNotices {
    private val inner = hashMapOf<String, MutableList<AddonNotice>>()
    var clientShowScreen = {}

    fun shouldShowScreen(side: AddonNotice.Side? = null): Boolean {
        val severities = inner.flatMap { notices -> notices.value.map { it.severity } }
        val hasProblems = severities.any { it.ordinal > AddonNotice.Severity.Info.ordinal }
        val hasErrors = severities.any { it == AddonNotice.Severity.Error }
        return hasErrors || (Showbiz.config.addons.showNoticeScreen && hasProblems)
    }

    fun showScreen() {
        clientShowScreen()
    }

    fun getText(): Component {
        val text = Component.empty()
        val addonIds = getAddonIds()
        val errors = addonIds.filter { addonId -> getAddon(addonId).any { it.severity == AddonNotice.Severity.Error } }
        val warnings = addonIds.filter { addonId -> getAddon(addonId).any { it.severity == AddonNotice.Severity.Warning } }
        if (errors.isNotEmpty() || warnings.isNotEmpty()) {
            text.append(Component.literal("\n").withStyle(ChatFormatting.RESET))
            if (errors.isNotEmpty()) text.append(
                Component.literal("Addons with errors: ${errors.joinToString(", ")}\n"
                ).withStyle(ChatFormatting.RED))
            if (warnings.isNotEmpty()) text.append(
                Component.literal("Addons with warnings: ${warnings.joinToString(", ")}\n"
                ).withStyle(ChatFormatting.YELLOW))
        }
        return text
    }

    //region Collection stuff
    fun add(addonId: String, notice: AddonNotice) {
        val notices = inner.getOrPut(addonId) { mutableListOf() }
        notices.add(notice)
    }

    fun getAddonIds() = inner.keys
    fun getAddon(addonId: String): List<AddonNotice> {
        return inner.getOrElse(addonId) { emptyList() }
    }

    fun clear(side: AddonNotice.Side) {
        inner.values.forEach { v -> v.removeIf { it.side == side } }
        inner.entries.removeIf { it.value.isEmpty() }
    }
    //endregion
}