package com.ashmeet.hyperlauncher.theme

import android.content.SharedPreferences
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences

@Composable
fun PojavTheme(
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit
) {
    val isInPreview = LocalInspectionMode.current
    val context = LocalContext.current
    val view = LocalView.current

    var themePref by remember {
        mutableStateOf(if (isInPreview) "system" else LauncherPreferences.PREF_THEME)
    }
    var themeType by remember {
        mutableStateOf(if (isInPreview) "tonal" else LauncherPreferences.PREF_THEME_TYPE)
    }
    var isCustomTheme by remember {
        mutableStateOf(if (isInPreview) false else LauncherPreferences.PREF_CUSTOM_THEME)
    }
    var themeColor by remember {
        mutableIntStateOf(if (isInPreview) 0xFF3F51B5.toInt() else LauncherPreferences.PREF_THEME_COLOR)
    }

    if (!isInPreview) {
        DisposableEffect(Unit) {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                when (key) {
                    "app_theme" -> themePref = LauncherPreferences.prefs.getString("app_theme", "system") ?: "system"
                    "app_theme_type" -> themeType = LauncherPreferences.prefs.getString("app_theme_type", "tonal") ?: "tonal"
                    "app_custom_theme" -> isCustomTheme = LauncherPreferences.prefs.getBoolean("app_custom_theme", false)
                    "app_theme_color" -> themeColor = LauncherPreferences.prefs.getInt("app_theme_color", 0xFF3F51B5.toInt())
                }
            }
            LauncherPreferences.prefs.registerOnSharedPreferenceChangeListener(listener)
            onDispose {
                LauncherPreferences.prefs.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }
    }

    val isDark = darkTheme ?: when (themePref) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    val primaryColor = if (isCustomTheme) {
        Color(themeColor)
    } else {
        colorResource(R.color.minebutton_color)
    }

    val colorScheme = when {
        isCustomTheme -> generateCustomColorScheme(primaryColor, isDark, themeType)
        themePref == "dynamic" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> getMonochromeColorScheme(primaryColor, isDark)
    }

    UpdateSystemBars(isDark, view)

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
