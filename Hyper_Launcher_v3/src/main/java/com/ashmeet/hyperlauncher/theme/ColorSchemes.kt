package com.ashmeet.hyperlauncher.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.colorResource
import net.ashmeet.hyperlauncher.R

fun generateCustomColorScheme(primary: Color, isDark: Boolean, themeType: String = "tonal"): ColorScheme {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(primary.toArgb(), hsv)
    
    var hue = hsv[0]
    var satMult = 1.0f
    var valMult = 1.0f

    when (themeType) {
        "vibrant" -> satMult = 1.5f
        "monochrome" -> satMult = 0.1f
        "neutral" -> satMult = 0.4f
        "tertiary" -> hue = (hue + 60f) % 360f
        "content" -> {
            satMult = 1.2f
            valMult = 0.9f
        }
    }

    val tertiaryHue = (hue + 60f) % 360f
    val errorHue = 0f

    val s = { s: Float -> (s * satMult).coerceIn(0f, 1f) }
    val v = { v: Float -> (v * valMult).coerceIn(0f, 1f) }

    return if (isDark) {
        darkColorScheme(
            primary = Color.hsv(hue, s(0.4f), v(0.9f)),
            onPrimary = Color.hsv(hue, s(0.8f), v(0.2f)),
            primaryContainer = Color.hsv(hue, s(0.6f), v(0.35f)),
            onPrimaryContainer = Color.hsv(hue, s(0.1f), v(0.95f)),
            inversePrimary = Color.hsv(hue, s(0.8f), v(0.4f)),
            secondary = Color.hsv(hue, s(0.3f), v(0.8f)),
            onSecondary = Color.hsv(hue, s(0.8f), v(0.2f)),
            secondaryContainer = Color.hsv(hue, s(0.5f), v(0.3f)),
            onSecondaryContainer = Color.hsv(hue, s(0.1f), v(0.9f)),
            tertiary = Color.hsv(tertiaryHue, s(0.3f), v(0.8f)),
            onTertiary = Color.hsv(tertiaryHue, s(0.8f), v(0.2f)),
            tertiaryContainer = Color.hsv(tertiaryHue, s(0.5f), v(0.3f)),
            onTertiaryContainer = Color.hsv(tertiaryHue, s(0.1f), v(0.9f)),
            background = Color.hsv(hue, s(0.1f), v(0.08f)),
            onBackground = Color.hsv(hue, s(0.05f), v(0.9f)),
            surface = Color.hsv(hue, s(0.12f), v(0.1f)),
            onSurface = Color.hsv(hue, s(0.05f), v(0.9f)),
            surfaceVariant = Color.hsv(hue, s(0.15f), v(0.2f)),
            onSurfaceVariant = Color.hsv(hue, s(0.05f), v(0.8f)),
            surfaceTint = Color.hsv(hue, s(0.4f), v(0.9f)),
            inverseSurface = Color.hsv(hue, s(0.05f), v(0.9f)),
            inverseOnSurface = Color.hsv(hue, s(0.1f), v(0.1f)),
            error = Color.hsv(errorHue, 0.6f, 0.8f),
            onError = Color.hsv(errorHue, 0.9f, 0.2f),
            errorContainer = Color.hsv(errorHue, 0.8f, 0.3f),
            onErrorContainer = Color.hsv(errorHue, 0.1f, 0.9f),
            outline = Color.hsv(hue, s(0.2f), v(0.6f)),
            outlineVariant = Color.hsv(hue, s(0.15f), v(0.3f)),
            scrim = Color.Black,
            surfaceBright = Color.hsv(hue, s(0.1f), v(0.2f)),
            surfaceDim = Color.hsv(hue, s(0.1f), v(0.06f)),
            surfaceContainer = Color.hsv(hue, s(0.1f), v(0.12f)),
            surfaceContainerHigh = Color.hsv(hue, s(0.1f), v(0.17f)),
            surfaceContainerHighest = Color.hsv(hue, s(0.1f), v(0.22f)),
            surfaceContainerLow = Color.hsv(hue, s(0.1f), v(0.1f)),
            surfaceContainerLowest = Color.hsv(hue, s(0.1f), v(0.04f)),
            primaryFixed = Color.hsv(hue, s(0.4f), v(0.9f)),
            primaryFixedDim = Color.hsv(hue, s(0.4f), v(0.8f)),
            onPrimaryFixed = Color.hsv(hue, s(0.8f), v(0.1f)),
            onPrimaryFixedVariant = Color.hsv(hue, s(0.6f), v(0.2f)),
            secondaryFixed = Color.hsv(hue, s(0.3f), v(0.8f)),
            secondaryFixedDim = Color.hsv(hue, s(0.3f), v(0.7f)),
            onSecondaryFixed = Color.hsv(hue, s(0.8f), v(0.1f)),
            onSecondaryFixedVariant = Color.hsv(hue, s(0.6f), v(0.2f)),
            tertiaryFixed = Color.hsv(tertiaryHue, s(0.3f), v(0.8f)),
            tertiaryFixedDim = Color.hsv(tertiaryHue, s(0.3f), v(0.7f)),
            onTertiaryFixed = Color.hsv(tertiaryHue, s(0.8f), v(0.1f)),
            onTertiaryFixedVariant = Color.hsv(tertiaryHue, s(0.6f), v(0.2f))
        )
    } else {
        lightColorScheme(
            primary = Color.hsv(hue, s(0.8f), v(0.4f)),
            onPrimary = Color.White,
            primaryContainer = Color.hsv(hue, s(0.15f), v(0.9f)),
            onPrimaryContainer = Color.hsv(hue, s(0.9f), v(0.1f)),
            inversePrimary = Color.hsv(hue, s(0.4f), v(0.9f)),
            secondary = Color.hsv(hue, s(0.4f), v(0.5f)),
            onSecondary = Color.White,
            secondaryContainer = Color.hsv(hue, s(0.1f), v(0.95f)),
            onSecondaryContainer = Color.hsv(hue, s(0.9f), v(0.1f)),
            tertiary = Color.hsv(tertiaryHue, s(0.4f), v(0.5f)),
            onTertiary = Color.White,
            tertiaryContainer = Color.hsv(tertiaryHue, s(0.1f), v(0.95f)),
            onTertiaryContainer = Color.hsv(tertiaryHue, s(0.9f), v(0.1f)),
            background = Color.hsv(hue, s(0.05f), v(0.98f)),
            onBackground = Color.hsv(hue, s(0.9f), v(0.1f)),
            surface = Color.hsv(hue, s(0.03f), v(1.0f)),
            onSurface = Color.hsv(hue, s(0.9f), v(0.1f)),
            surfaceVariant = Color.hsv(hue, s(0.1f), v(0.9f)),
            onSurfaceVariant = Color.hsv(hue, s(0.7f), v(0.3f)),
            surfaceTint = Color.hsv(hue, s(0.8f), v(0.4f)),
            inverseSurface = Color.hsv(hue, s(0.8f), v(0.2f)),
            inverseOnSurface = Color.hsv(hue, s(0.05f), v(0.95f)),
            error = Color.hsv(errorHue, 0.8f, 0.4f),
            onError = Color.White,
            errorContainer = Color.hsv(errorHue, 0.1f, 0.95f),
            onErrorContainer = Color.hsv(errorHue, 0.9f, 0.1f),
            outline = Color.hsv(hue, s(0.4f), v(0.5f)),
            outlineVariant = Color.hsv(hue, s(0.2f), v(0.8f)),
            scrim = Color.Black,
            surfaceBright = Color.hsv(hue, s(0.02f), v(0.98f)),
            surfaceDim = Color.hsv(hue, s(0.1f), v(0.87f)),
            surfaceContainer = Color.hsv(hue, s(0.05f), v(0.94f)),
            surfaceContainerHigh = Color.hsv(hue, s(0.05f), v(0.92f)),
            surfaceContainerHighest = Color.hsv(hue, s(0.05f), v(0.89f)),
            surfaceContainerLow = Color.hsv(hue, s(0.05f), v(0.96f)),
            surfaceContainerLowest = Color.White,
            primaryFixed = Color.hsv(hue, s(0.15f), v(0.9f)),
            primaryFixedDim = Color.hsv(hue, s(0.3f), v(0.8f)),
            onPrimaryFixed = Color.hsv(hue, s(0.9f), v(0.1f)),
            onPrimaryFixedVariant = Color.hsv(hue, s(0.7f), v(0.3f)),
            secondaryFixed = Color.hsv(hue, s(0.1f), v(0.95f)),
            secondaryFixedDim = Color.hsv(hue, s(0.2f), v(0.85f)),
            onSecondaryFixed = Color.hsv(hue, s(0.9f), v(0.1f)),
            onSecondaryFixedVariant = Color.hsv(hue, s(0.7f), v(0.3f)),
            tertiaryFixed = Color.hsv(tertiaryHue, s(0.1f), v(0.95f)),
            tertiaryFixedDim = Color.hsv(tertiaryHue, s(0.2f), v(0.85f)),
            onTertiaryFixed = Color.hsv(tertiaryHue, s(0.9f), v(0.1f)),
            onTertiaryFixedVariant = Color.hsv(tertiaryHue, s(0.7f), v(0.3f))
        )
    }
}

@Composable
fun getMonochromeColorScheme(primaryColor: Color, isDark: Boolean): ColorScheme {
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

    return if (isDark) {
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
