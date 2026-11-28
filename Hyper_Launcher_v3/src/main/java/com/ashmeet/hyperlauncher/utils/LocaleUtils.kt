@file:Suppress("DEPRECATION", "UsePropertyAccessSyntax")

package com.ashmeet.hyperlauncher.utils

import android.content.Context
import android.content.ContextWrapper
import android.os.LocaleList
import androidx.preference.PreferenceManager
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import java.util.Locale

open class LocaleUtils(base: Context?) : ContextWrapper(base) {

    companion object {
        @JvmStatic
        fun setLocale(context: Context): ContextWrapper {
            if (LauncherPreferences.DEFAULT_PREF == null) {
                val pref = PreferenceManager.getDefaultSharedPreferences(context)
                LauncherPreferences.DEFAULT_PREF = pref
                LauncherPreferences.PREF_FORCE_ENGLISH = pref.getBoolean("force_english", false)
                LauncherPreferences.PREF_LANGUAGE = pref.getString("app_language", "en") ?: "en"
            }

            val forceEnglish = LauncherPreferences.PREF_FORCE_ENGLISH
            val prefLang = LauncherPreferences.PREF_LANGUAGE

            val locale: Locale = if (forceEnglish) {
                Locale.ENGLISH
            } else if (prefLang != "system") {
                if (prefLang.contains("-")) {
                    val parts = prefLang.split("-")
                    Locale.Builder().setLanguage(parts[0]).setRegion(parts[1]).build()
                } else {
                    Locale.Builder().setLanguage(prefLang).build()
                }
            } else {
                return LocaleUtils(context)
            }

            val resources = context.resources
            val configuration = resources.configuration

            configuration.setLocale(locale)
            Locale.setDefault(locale)
            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)
            configuration.setLocales(localeList)


            resources.updateConfiguration(configuration, resources.displayMetrics)
            val finalContext = context.createConfigurationContext(configuration)

            return LocaleUtils(finalContext)
        }
    }
}
