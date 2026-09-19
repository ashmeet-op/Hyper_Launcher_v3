package com.ashmeet.hyperlauncher.theme

import android.content.SharedPreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences

@Composable
fun PojavTheme(
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit
) {
    val isInPreview = LocalInspectionMode.current
    var themePref by remember {
        mutableStateOf(if (isInPreview) "system" else LauncherPreferences.PREF_THEME)
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
        val color = Color(themeColor)
        val lum = color.luminance()
        if (isDark) {
            if (lum < 0.1f) colorResource(R.color.minebutton_color) else color
        } else {
            if (lum > 0.9f) colorResource(R.color.minebutton_color) else color
        }
    } else {
        colorResource(R.color.minebutton_color)
    }

    val colorScheme = if (isCustomTheme) {
        generateCustomColorScheme(primaryColor, isDark)
    } else {
        val darkenedPrimary = Color(
            red = primaryColor.red * 0.3f,
            green = primaryColor.green * 0.3f,
            blue = primaryColor.blue * 0.3f,
            alpha = 1f
        )
        val lightenedPrimary = Color(
            red = primaryColor.red * 0.2f + 0.8f,
            green = primaryColor.green * 0.2f + 0.8f,
            blue = primaryColor.blue * 0.2f + 0.8f,
            alpha = 1f
        )

        val errorColor = colorResource(R.color.warning)
        val darkenedError = Color(
            red = errorColor.red * 0.3f,
            green = errorColor.green * 0.3f,
            blue = errorColor.blue * 0.3f,
            alpha = 1f
        )

        if (isDark) {
            darkColorScheme(
                primary = primaryColor,
                onPrimary = if (primaryColor.luminance() > 0.5f) darkenedPrimary else lightenedPrimary,
                primaryContainer = primaryColor.copy(alpha = 0.3f),
                onPrimaryContainer = lightenedPrimary,
                secondary = primaryColor,
                onSecondary = if (primaryColor.luminance() > 0.5f) darkenedPrimary else lightenedPrimary,
                secondaryContainer = primaryColor.copy(alpha = 0.2f),
                onSecondaryContainer = lightenedPrimary,
                tertiary = primaryColor,
                onTertiary = if (primaryColor.luminance() > 0.5f) darkenedPrimary else lightenedPrimary,
                tertiaryContainer = primaryColor.copy(alpha = 0.15f),
                onTertiaryContainer = lightenedPrimary,
                error = errorColor,
                onError = darkenedError,
                errorContainer = Color(0xFF93000A),
                onErrorContainer = Color(0xFFFFDAD6),
                background = colorResource(R.color.background_app),
                onBackground = lightenedPrimary,
                surface = colorResource(R.color.background_status_bar),
                onSurface = lightenedPrimary,
                surfaceVariant = colorResource(R.color.background_overlay),
                onSurfaceVariant = lightenedPrimary.copy(alpha = 0.7f),
                outline = colorResource(R.color.divider),
                outlineVariant = colorResource(R.color.divider).copy(alpha = 0.5f),
                scrim = Color.Black,
                inverseSurface = lightenedPrimary,
                inverseOnSurface = darkenedPrimary,
                inversePrimary = primaryColor,
                surfaceDim = Color(0xFF1A1A1A),
                surfaceBright = Color(0xFF3B3B3B),
                surfaceContainerLowest = Color(0xFF0A0A0A),
                surfaceContainerLow = Color(0xFF1A1A1A),
                surfaceContainer = Color(0xFF212121),
                surfaceContainerHigh = Color(0xFF2B2B2B),
                surfaceContainerHighest = Color(0xFF333333)
            )
        } else {
            lightColorScheme(
                primary = primaryColor,
                onPrimary = if (primaryColor.luminance() > 0.5f) darkenedPrimary else Color.White,
                primaryContainer = primaryColor.copy(alpha = 0.1f),
                onPrimaryContainer = darkenedPrimary,
                secondary = primaryColor,
                onSecondary = if (primaryColor.luminance() > 0.5f) darkenedPrimary else Color.White,
                secondaryContainer = primaryColor.copy(alpha = 0.05f),
                onSecondaryContainer = darkenedPrimary,
                tertiary = primaryColor,
                onTertiary = if (primaryColor.luminance() > 0.5f) darkenedPrimary else Color.White,
                tertiaryContainer = primaryColor.copy(alpha = 0.03f),
                onTertiaryContainer = darkenedPrimary,
                error = errorColor,
                onError = darkenedError,
                errorContainer = Color(0xFFFFDAD6),
                onErrorContainer = Color(0xFF410002),
                background = colorResource(R.color.background_app),
                onBackground = darkenedPrimary,
                surface = colorResource(R.color.background_status_bar),
                onSurface = darkenedPrimary,
                surfaceVariant = colorResource(R.color.background_overlay),
                onSurfaceVariant = darkenedPrimary.copy(alpha = 0.7f),
                outline = colorResource(R.color.divider),
                outlineVariant = colorResource(R.color.divider).copy(alpha = 0.5f),
                scrim = Color.Black,
                inverseSurface = Color(0xFF313033),
                inverseOnSurface = Color(0xFFF4EFF4),
                inversePrimary = primaryColor,
                surfaceDim = Color(0xFFDED8E1),
                surfaceBright = Color(0xFFFEF7FF),
                surfaceContainerLowest = Color.White,
                surfaceContainerLow = Color(0xFFF7F2FA),
                surfaceContainer = Color(0xFFF3EDF7),
                surfaceContainerHigh = Color(0xFFECE6F0),
                surfaceContainerHighest = Color(0xFFE6E0E9)
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

private fun generateCustomColorScheme(primary: Color, isDark: Boolean): ColorScheme {
    val darkenedPrimary = Color(
        red = primary.red * 0.3f,
        green = primary.green * 0.3f,
        blue = primary.blue * 0.3f,
        alpha = 1f
    )
    val lightenedPrimary = Color(
        red = primary.red * 0.2f + 0.8f,
        green = primary.green * 0.2f + 0.8f,
        blue = primary.blue * 0.2f + 0.8f,
        alpha = 1f
    )
    val onPrimary = if (primary.luminance() > 0.5f) darkenedPrimary else if (isDark) lightenedPrimary else Color.White

    val errorColor = Color(if (isDark) 0xFFCF6679 else 0xFFB00020)
    val darkenedError = Color(
        red = errorColor.red * 0.3f,
        green = errorColor.green * 0.3f,
        blue = errorColor.blue * 0.3f,
        alpha = 1f
    )

    return if (isDark) {
        val darkBackground = Color(0xFF121212)
        val tintedBackground = primary.copy(alpha = 0.08f).compositeOver(darkBackground)
        val tintedSurface = primary.copy(alpha = 0.12f).compositeOver(darkBackground)
        val onSurface = lightenedPrimary.copy(alpha = 0.9f)

        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primary.copy(alpha = 0.3f).compositeOver(darkBackground),
            onPrimaryContainer = lightenedPrimary,
            secondary = primary,
            onSecondary = onPrimary,
            secondaryContainer = primary.copy(alpha = 0.2f).compositeOver(darkBackground),
            onSecondaryContainer = lightenedPrimary,
            tertiary = primary,
            onTertiary = onPrimary,
            tertiaryContainer = primary.copy(alpha = 0.15f).compositeOver(darkBackground),
            onTertiaryContainer = lightenedPrimary,
            error = errorColor,
            onError = darkenedError,
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            background = tintedBackground,
            onBackground = onSurface,
            surface = tintedSurface,
            onSurface = onSurface,
            surfaceVariant = primary.copy(alpha = 0.16f).compositeOver(darkBackground),
            onSurfaceVariant = lightenedPrimary.copy(alpha = 0.7f),
            outline = primary.copy(alpha = 0.5f),
            outlineVariant = primary.copy(alpha = 0.2f),
            scrim = Color.Black,
            inverseSurface = lightenedPrimary,
            inverseOnSurface = darkenedPrimary,
            inversePrimary = primary,
            surfaceDim = primary.copy(alpha = 0.1f).compositeOver(darkBackground),
            surfaceBright = primary.copy(alpha = 0.2f).compositeOver(darkBackground),
            surfaceContainerLowest = Color(0xFF0F0F0F),
            surfaceContainerLow = Color(0xFF1A1A1A),
            surfaceContainer = Color(0xFF212121),
            surfaceContainerHigh = Color(0xFF2B2B2B),
            surfaceContainerHighest = Color(0xFF333333)
        )
    } else {
        val lightBackground = Color(0xFFF2F2F2)
        val tintedBackground = primary.copy(alpha = 0.05f).compositeOver(lightBackground)
        val tintedSurface = primary.copy(alpha = 0.08f).compositeOver(Color.White)
        val onSurface = darkenedPrimary.copy(alpha = 0.9f)

        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primary.copy(alpha = 0.15f).compositeOver(Color.White),
            onPrimaryContainer = darkenedPrimary,
            secondary = primary,
            onSecondary = onPrimary,
            secondaryContainer = primary.copy(alpha = 0.1f).compositeOver(Color.White),
            onSecondaryContainer = darkenedPrimary,
            tertiary = primary,
            onTertiary = onPrimary,
            tertiaryContainer = primary.copy(alpha = 0.07f).compositeOver(Color.White),
            onTertiaryContainer = darkenedPrimary,
            error = errorColor,
            onError = darkenedError,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            background = tintedBackground,
            onBackground = onSurface,
            surface = tintedSurface,
            onSurface = onSurface,
            surfaceVariant = primary.copy(alpha = 0.12f).compositeOver(lightBackground),
            onSurfaceVariant = darkenedPrimary.copy(alpha = 0.7f),
            outline = primary.copy(alpha = 0.4f),
            outlineVariant = primary.copy(alpha = 0.15f),
            scrim = Color.Black,
            inverseSurface = Color(0xFF313033),
            inverseOnSurface = lightenedPrimary,
            inversePrimary = primary,
            surfaceDim = Color(0xFFDED8E1),
            surfaceBright = Color(0xFFFEF7FF),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color(0xFFF7F2FA),
            surfaceContainer = Color(0xFFF3EDF7),
            surfaceContainerHigh = Color(0xFFECE6F0),
            surfaceContainerHighest = Color(0xFFE6E0E9)
        )
    }
}
