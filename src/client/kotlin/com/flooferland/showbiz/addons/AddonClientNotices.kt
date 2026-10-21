package com.flooferland.showbiz.addons

import com.flooferland.showbiz.Showbiz

object AddonClientNotices {
    private val inner = hashMapOf<String, MutableList<AddonNotice>>()

    fun shouldShowScreen(): Boolean =
        Showbiz.config.addons.showNoticeScreenOnReload
        && inner.any { addon ->
            addon.value.any { notice -> notice.severity.ordinal > AddonNotice.Severity.Info.ordinal }
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