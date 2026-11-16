package com.ashmeet.hyperlauncher.theme

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import net.ashmeet.hyperlauncher.R

@Composable
fun UpdateSystemBars(isDark: Boolean, view: View) {
    if (!view.isInEditMode) {
        SideEffect {
            val context = view.context
            if (context is ComponentActivity) {
                context.enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    }
                )
            }
        }
    }
}

object ThemeUtils {
    @JvmStatic
    fun getThemePrimaryColor(context: Context): Int {
        val isCustomTheme = LauncherPreferences.PREF_CUSTOM_THEME
        val themeColor = LauncherPreferences.PREF_THEME_COLOR
        val themePref = LauncherPreferences.PREF_THEME
        val themeType = LauncherPreferences.PREF_THEME_TYPE

        val isDark = when (themePref) {
            "light" -> false
            "dark" -> true
            else -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }

        val primaryColor = if (isCustomTheme) {
            Color(themeColor)
        } else {
            val typedArray = context.obtainStyledAttributes(intArrayOf(android.R.attr.colorPrimary))
            val colorInt = typedArray.getColor(0, context.getColor(R.color.minebutton_color))
            typedArray.recycle()
            Color(colorInt)
        }

        return if (isCustomTheme) {
            generateCustomColorScheme(primaryColor, isDark, themeType).primary.toArgb()
        } else {
            primaryColor.toArgb()
        }
    }
}
