package com.ashmeet.hyperlauncher.components.list

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashmeet.hyperlauncher.utils.installer.ModrinthVersion
import com.ashmeet.hyperlauncher.utils.installer.cleanLoadersList
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionItemView(
    version: ModrinthVersion,
    isCompatible: Boolean,
    isLoaderCompatible: Boolean = true,
    onClick: () -> Unit
) {
    val animatedAlpha = remember { Animatable(0f) }
    LaunchedEffect(version.id) {
        animatedAlpha.snapTo(0f)
        animatedAlpha.animateTo(1f, animationSpec = tween(400))
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = animatedAlpha.value },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VersionStatusBadge(version.versionType)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = version.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 15.sp
                )
                Text(
                    text = cleanLoadersList(version.loaders),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!isLoaderCompatible) {
                val tooltipState = rememberTooltipState()
                val scope = rememberCoroutineScope()
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text("Incompatible loader") } },
                    state = tooltipState
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = translatedText("Incompatible loader"),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(end = 2.dp)
                            .size(24.dp)
                            .clickable { scope.launch { tooltipState.show() } }
                    )
                }
            }
        }
    }
}

@Composable
fun VersionStatusBadge(type: String) {
    val (text, color, bgColor) = when (type.lowercase()) {
        "alpha" -> Triple("A", Color(0xFFE57373), Color(0xFFC62828).copy(alpha = 0.2f))
        "beta" -> Triple("B", Color(0xFFFFD54F), Color(0xFFF9A825).copy(alpha = 0.2f))
        else -> Triple("R", Color(0xFF81C784), Color(0xFF2E7D32).copy(alpha = 0.2f))
    }

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp
        )
    }
}

@Composable
fun SubVersionItemView(
    text: String,
    isCompatible: Boolean,
    onClick: () -> Unit
) {
    val animatedAlpha = remember { Animatable(0f) }
    LaunchedEffect(text) {
        animatedAlpha.snapTo(0f)
        animatedAlpha.animateTo(1f, animationSpec = tween(400))
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = animatedAlpha.value },
        shape = RoundedCornerShape(12.dp),
        color = if (isCompatible) Color(0xFF2E7D32).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            fontWeight = if (isCompatible) FontWeight.Bold else FontWeight.Normal,
            color = if (isCompatible) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
        )
    }
}
