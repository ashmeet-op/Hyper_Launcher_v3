package com.ashmeet.hyperlauncher.components.button

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    height: Dp = 48.dp,
    shape: Shape = CircleShape,
    isUppercase: Boolean = false,
    tintIcon: Boolean = false
) {
    val isCustomTheme = remember { LauncherPreferences.PREF_CUSTOM_THEME }
    val primaryColor = MaterialTheme.colorScheme.primary
    val contentColor = calculateMineButtonContentColor(isCustomTheme, primaryColor)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "ButtonScale"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .height(height)
            .graphicsLayer(scaleX = scale, scaleY = scale),
        shape = shape,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = primaryColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (tintIcon) androidx.compose.material3.LocalContentColor.current else Color.Unspecified
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = if (isUppercase) text.uppercase() else text,
                fontWeight = if (isCustomTheme) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun calculateMineButtonContentColor(isCustomTheme: Boolean, primaryColor: Color): Color {
    val isLightMode = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    return if (isCustomTheme) {
        if (isLightMode) {
            Color(
                red = primaryColor.red * 0.3f,
                green = primaryColor.green * 0.3f,
                blue = primaryColor.blue * 0.3f,
                alpha = 1f
            )
        } else {
            if (primaryColor.luminance() > 0.5f) {
                Color(
                    red = primaryColor.red * 0.3f,
                    green = primaryColor.green * 0.3f,
                    blue = primaryColor.blue * 0.3f,
                    alpha = 1f
                )
            } else {
                Color(
                    red = primaryColor.red * 0.2f + 0.8f,
                    green = primaryColor.green * 0.2f + 0.8f,
                    blue = primaryColor.blue * 0.2f + 0.8f,
                    alpha = 1f
                )
            }
        }
    } else {
        MaterialTheme.colorScheme.onPrimary
    }
}
