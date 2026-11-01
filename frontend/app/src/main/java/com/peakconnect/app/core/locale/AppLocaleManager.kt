package com.peakconnect.app.core.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

enum class AppLanguage(val tag: String) {
    SYSTEM_DEFAULT(""),
    ENGLISH("en"),
    HINDI("hi");

    companion object {
        fun fromTag(tag: String?): AppLanguage {
            if (tag.isNullOrBlank()) return SYSTEM_DEFAULT
            return when {
                tag.startsWith("hi", ignoreCase = true) -> HINDI
                tag.startsWith("en", ignoreCase = true) -> ENGLISH
                else -> SYSTEM_DEFAULT
            }
        }
    }
}

object AppLocaleManager {
    private var trackedLanguage: AppLanguage? = null

    fun getCurrentLanguage(): AppLanguage {
        trackedLanguage?.let { return it }
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return AppLanguage.SYSTEM_DEFAULT
        val primaryTag = locales.toLanguageTags()
        return AppLanguage.fromTag(primaryTag)
    }

    fun setLanguage(language: AppLanguage) {
        trackedLanguage = language
        val localeList = when (language) {
            AppLanguage.SYSTEM_DEFAULT -> LocaleListCompat.getEmptyLocaleList()
            AppLanguage.ENGLISH -> LocaleListCompat.forLanguageTags("en")
            AppLanguage.HINDI -> LocaleListCompat.forLanguageTags("hi")
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun resetForTest() {
        trackedLanguage = null
    }
}
