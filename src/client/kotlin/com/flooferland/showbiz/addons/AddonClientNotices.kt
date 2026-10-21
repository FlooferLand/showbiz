package com.flooferland.showbiz.addons

import com.flooferland.showbiz.Showbiz

object AddonClientNotices {
    private val inner = hashMapOf<String, MutableList<AddonNotice>>()

    fun shouldShowScreen(): Boolean {
        val severities = inner.flatMap { notices -> notices.value.map { it.severity } }
        val hasProblems = severities.any { it.ordinal > AddonNotice.Severity.Info.ordinal }
        val hasErrors = severities.any { it == AddonNotice.Severity.Error }
        return hasErrors || (Showbiz.config.addons.showNoticeScreen && hasProblems)
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

    fun clear() {
        inner.clear()
    }
    //endregion
}