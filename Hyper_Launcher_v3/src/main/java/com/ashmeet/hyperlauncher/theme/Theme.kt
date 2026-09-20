package com.ashmeet.hyperlauncher.theme

import android.content.SharedPreferences
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
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
        Color(themeColor)
    } else {
        colorResource(R.color.minebutton_color)
    }

    val colorScheme = when {
        isCustomTheme -> generateCustomColorScheme(primaryColor, isDark)
        themePref == "dynamic" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current)
        }
        else -> {
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
                    error = colorResource(R.color.warning),
                    onError = darkenedPrimary,
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
                    error = colorResource(R.color.warning),
                    onError = lightenedPrimary,
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
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

private fun generateCustomColorScheme(primary: Color, isDark: Boolean): ColorScheme {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(primary.toArgb(), hsv)
    val hue = hsv[0]

    val primaryHue = hue
    val secondaryHue = hue
    val tertiaryHue = (hue + 60f) % 360f
    val errorHue = 0f

    return if (isDark) {
        darkColorScheme(
            primary = Color.hsv(primaryHue, 0.4f, 0.9f),
            onPrimary = Color.hsv(primaryHue, 0.8f, 0.2f),
            primaryContainer = Color.hsv(primaryHue, 0.6f, 0.35f),
            onPrimaryContainer = Color.hsv(primaryHue, 0.1f, 0.95f),
            inversePrimary = Color.hsv(primaryHue, 0.8f, 0.4f),
            secondary = Color.hsv(secondaryHue, 0.3f, 0.8f),
            onSecondary = Color.hsv(secondaryHue, 0.8f, 0.2f),
            secondaryContainer = Color.hsv(secondaryHue, 0.5f, 0.3f),
            onSecondaryContainer = Color.hsv(secondaryHue, 0.1f, 0.9f),
            tertiary = Color.hsv(tertiaryHue, 0.3f, 0.8f),
            onTertiary = Color.hsv(tertiaryHue, 0.8f, 0.2f),
            tertiaryContainer = Color.hsv(tertiaryHue, 0.5f, 0.3f),
            onTertiaryContainer = Color.hsv(tertiaryHue, 0.1f, 0.9f),
            background = Color.hsv(primaryHue, 0.1f, 0.08f),
            onBackground = Color.hsv(primaryHue, 0.05f, 0.9f),
            surface = Color.hsv(primaryHue, 0.12f, 0.1f),
            onSurface = Color.hsv(primaryHue, 0.05f, 0.9f),
            surfaceVariant = Color.hsv(primaryHue, 0.15f, 0.2f),
            onSurfaceVariant = Color.hsv(primaryHue, 0.05f, 0.8f),
            surfaceTint = Color.hsv(primaryHue, 0.4f, 0.9f),
            inverseSurface = Color.hsv(primaryHue, 0.05f, 0.9f),
            inverseOnSurface = Color.hsv(primaryHue, 0.1f, 0.1f),
            error = Color.hsv(errorHue, 0.6f, 0.8f),
            onError = Color.hsv(errorHue, 0.9f, 0.2f),
            errorContainer = Color.hsv(errorHue, 0.8f, 0.3f),
            onErrorContainer = Color.hsv(errorHue, 0.1f, 0.9f),
            outline = Color.hsv(primaryHue, 0.2f, 0.6f),
            outlineVariant = Color.hsv(primaryHue, 0.15f, 0.3f),
            scrim = Color.Black,
            surfaceBright = Color.hsv(primaryHue, 0.1f, 0.2f),
            surfaceDim = Color.hsv(primaryHue, 0.1f, 0.06f),
            surfaceContainer = Color.hsv(primaryHue, 0.1f, 0.12f),
            surfaceContainerHigh = Color.hsv(primaryHue, 0.1f, 0.17f),
            surfaceContainerHighest = Color.hsv(primaryHue, 0.1f, 0.22f),
            surfaceContainerLow = Color.hsv(primaryHue, 0.1f, 0.1f),
            surfaceContainerLowest = Color.hsv(primaryHue, 0.1f, 0.04f),
            primaryFixed = Color.hsv(primaryHue, 0.4f, 0.9f),
            primaryFixedDim = Color.hsv(primaryHue, 0.4f, 0.8f),
            onPrimaryFixed = Color.hsv(primaryHue, 0.8f, 0.1f),
            onPrimaryFixedVariant = Color.hsv(primaryHue, 0.6f, 0.2f),
            secondaryFixed = Color.hsv(secondaryHue, 0.3f, 0.8f),
            secondaryFixedDim = Color.hsv(secondaryHue, 0.3f, 0.7f),
            onSecondaryFixed = Color.hsv(secondaryHue, 0.8f, 0.1f),
            onSecondaryFixedVariant = Color.hsv(secondaryHue, 0.6f, 0.2f),
            tertiaryFixed = Color.hsv(tertiaryHue, 0.3f, 0.8f),
            tertiaryFixedDim = Color.hsv(tertiaryHue, 0.3f, 0.7f),
            onTertiaryFixed = Color.hsv(tertiaryHue, 0.8f, 0.1f),
            onTertiaryFixedVariant = Color.hsv(tertiaryHue, 0.6f, 0.2f)
        )
    } else {
        lightColorScheme(
            primary = Color.hsv(primaryHue, 0.8f, 0.4f),
            onPrimary = Color.White,
            primaryContainer = Color.hsv(primaryHue, 0.15f, 0.9f),
            onPrimaryContainer = Color.hsv(primaryHue, 0.9f, 0.1f),
            inversePrimary = Color.hsv(primaryHue, 0.4f, 0.9f),
            secondary = Color.hsv(secondaryHue, 0.4f, 0.5f),
            onSecondary = Color.White,
            secondaryContainer = Color.hsv(secondaryHue, 0.1f, 0.95f),
            onSecondaryContainer = Color.hsv(secondaryHue, 0.9f, 0.1f),
            tertiary = Color.hsv(tertiaryHue, 0.4f, 0.5f),
            onTertiary = Color.White,
            tertiaryContainer = Color.hsv(tertiaryHue, 0.1f, 0.95f),
            onTertiaryContainer = Color.hsv(tertiaryHue, 0.9f, 0.1f),
            background = Color.hsv(primaryHue, 0.05f, 0.98f),
            onBackground = Color.hsv(primaryHue, 0.9f, 0.1f),
            surface = Color.hsv(primaryHue, 0.03f, 1.0f),
            onSurface = Color.hsv(primaryHue, 0.9f, 0.1f),
            surfaceVariant = Color.hsv(primaryHue, 0.1f, 0.9f),
            onSurfaceVariant = Color.hsv(primaryHue, 0.7f, 0.3f),
            surfaceTint = Color.hsv(primaryHue, 0.8f, 0.4f),
            inverseSurface = Color.hsv(primaryHue, 0.8f, 0.2f),
            inverseOnSurface = Color.hsv(primaryHue, 0.05f, 0.95f),
            error = Color.hsv(errorHue, 0.8f, 0.4f),
            onError = Color.White,
            errorContainer = Color.hsv(errorHue, 0.1f, 0.95f),
            onErrorContainer = Color.hsv(errorHue, 0.9f, 0.1f),
            outline = Color.hsv(primaryHue, 0.4f, 0.5f),
            outlineVariant = Color.hsv(primaryHue, 0.2f, 0.8f),
            scrim = Color.Black,
            surfaceBright = Color.hsv(primaryHue, 0.02f, 0.98f),
            surfaceDim = Color.hsv(primaryHue, 0.1f, 0.87f),
            surfaceContainer = Color.hsv(primaryHue, 0.05f, 0.94f),
            surfaceContainerHigh = Color.hsv(primaryHue, 0.05f, 0.92f),
            surfaceContainerHighest = Color.hsv(primaryHue, 0.05f, 0.89f),
            surfaceContainerLow = Color.hsv(primaryHue, 0.05f, 0.96f),
            surfaceContainerLowest = Color.White,
            primaryFixed = Color.hsv(primaryHue, 0.15f, 0.9f),
            primaryFixedDim = Color.hsv(primaryHue, 0.3f, 0.8f),
            onPrimaryFixed = Color.hsv(primaryHue, 0.9f, 0.1f),
            onPrimaryFixedVariant = Color.hsv(primaryHue, 0.7f, 0.3f),
            secondaryFixed = Color.hsv(secondaryHue, 0.1f, 0.95f),
            secondaryFixedDim = Color.hsv(secondaryHue, 0.2f, 0.85f),
            onSecondaryFixed = Color.hsv(secondaryHue, 0.9f, 0.1f),
            onSecondaryFixedVariant = Color.hsv(secondaryHue, 0.7f, 0.3f),
            tertiaryFixed = Color.hsv(tertiaryHue, 0.1f, 0.95f),
            tertiaryFixedDim = Color.hsv(tertiaryHue, 0.2f, 0.85f),
            onTertiaryFixed = Color.hsv(tertiaryHue, 0.9f, 0.1f),
            onTertiaryFixedVariant = Color.hsv(tertiaryHue, 0.7f, 0.3f)
        )
    }
}
