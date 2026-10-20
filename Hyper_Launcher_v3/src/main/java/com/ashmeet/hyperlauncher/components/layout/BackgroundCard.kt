package com.ashmeet.hyperlauncher.components.layout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.utils.blur.backgroundGlass

@Composable
fun BackgroundCard(
    modifier: Modifier = Modifier,
    influencedByBackground: Boolean = true,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ),
    elevation: CardElevation = CardDefaults.cardElevation(),
    blur: Int = if (LauncherPreferences.PREF_LAUNCHER_BACKGROUND_BLUR > 0) LauncherPreferences.PREF_LAUNCHER_BACKGROUND_BLUR else 25,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = colors.copy(containerColor = Color.Transparent),
        elevation = elevation,
        border = border,
    ) {
        Column(
            modifier = Modifier
                .clip(shape)
                .backgroundGlass(blur, colors.containerColor, enabled = influencedByBackground && LauncherPreferences.PREF_LIQUID_GLASS_ELEMENTS_ENABLED),
            content = content
        )
    }
}

@Composable
fun BackgroundCard(
    modifier: Modifier = Modifier,
    influencedByBackground: Boolean = true,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
    ),
    elevation: CardElevation = CardDefaults.cardElevation(),
    blur: Int = if (LauncherPreferences.PREF_LAUNCHER_BACKGROUND_BLUR > 0) LauncherPreferences.PREF_LAUNCHER_BACKGROUND_BLUR else 25,
    border: BorderStroke? = null,
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable @UiComposable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
        elevation = elevation,
        border = border,
        onClick = onClick,
        enabled = enabled,
    ) {
        Column(
            modifier = Modifier
                .clip(shape)
                .backgroundGlass(blur, colors.containerColor, enabled = influencedByBackground && LauncherPreferences.PREF_LIQUID_GLASS_ELEMENTS_ENABLED),
            content = content
        )
    }
}
