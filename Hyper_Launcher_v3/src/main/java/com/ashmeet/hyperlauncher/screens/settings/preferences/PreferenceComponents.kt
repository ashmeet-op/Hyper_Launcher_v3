package com.ashmeet.hyperlauncher.screens.settings.preferences

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ashmeet.hyperlauncher.components.slider.SimpleTextSlider
import com.ashmeet.hyperlauncher.components.switch.DefaultSwitch

import com.ashmeet.hyperlauncher.screens.settings.layouts.TitleAndSummary
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import net.ashmeet.hyperlauncher.R
import kotlinx.coroutines.launch

data class CursorInfo(val name: String, val shapeId: Int, val suffix: String)

val cursorInfos = listOf(
    CursorInfo("Arrow", 0, "arrow"),
    CursorInfo("I-Beam", 1, "ibeam"),
    CursorInfo("Crosshair", 2, "crosshair"),
    CursorInfo("Hand", 3, "hand"),
    CursorInfo("Horizontal Resize", 4, "hresize"),
    CursorInfo("Vertical Resize", 5, "vresize"),
    CursorInfo("All Resize", 6, "all_resize"),
    CursorInfo("Not Allowed", 7, "not_allowed")
)

fun getDefaultCursorDrawable(shapeId: Int): Int {
    return when (shapeId) {
        0 -> R.drawable.img_mouse_pointer_arrow
        1 -> R.drawable.img_mouse_pointer_ibeam
        2 -> R.drawable.img_mouse_pointer_crosshair
        3 -> R.drawable.img_mouse_pointer_link
        4 -> R.drawable.img_mouse_pointer_resize_ew
        5 -> R.drawable.img_mouse_pointer_resize_ns
        6 -> R.drawable.img_mouse_pointer_resize_move
        7 -> R.drawable.img_mouse_pointer_not_allowed
        else -> R.drawable.img_mouse_pointer_arrow
    }
}

@Composable
fun CursorPreferenceItem(
    title: String,
    shapeId: Int,
    imagePath: String?,
    hotspotX: Float = -1f,
    hotspotY: Float = -1f,
    onPickImage: () -> Unit,
    onAdjustHotspot: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val painter = if (imagePath != null) {
            val bitmap = remember(imagePath) {
                try {
                    BitmapFactory.decodeFile(imagePath)
                } catch (_: Exception) {
                    null
                }
            }
            if (bitmap != null) {
                BitmapPainter(bitmap.asImageBitmap())
            } else {
                painterResource(id = getDefaultCursorDrawable(shapeId))
            }
        } else {
            painterResource(id = getDefaultCursorDrawable(shapeId))
        }

        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            val hotspotSummary = if (hotspotX != -1f && hotspotY != -1f) {
                " (Hotspot: ${hotspotX.toInt()}, ${hotspotY.toInt()})"
            } else ""
            TitleAndSummary(
                title = title,
                summary = (if (imagePath != null) translatedText("Custom active") else translatedText("Default active")) + hotspotSummary
            )
        }

        IconButton(onClick = onPickImage) {
            Icon(Icons.Rounded.AddCircleOutline, contentDescription = translatedText("Change image"), tint = MaterialTheme.colorScheme.onSurface)
        }

        IconButton(onClick = onAdjustHotspot) {
            Icon(Icons.Rounded.DragIndicator, contentDescription = translatedText("Adjust hotspot"), tint = MaterialTheme.colorScheme.onSurface)
        }

        IconButton(onClick = onReset) {
            Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = translatedText("Reset"), tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun PreferenceCategory(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 12.dp)
            .fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsActionItem(
    title: String,
    summary: String? = null,
    icon: ImageVector? = null,
    iconPainter: Painter? = null,
    enabled: Boolean = true,
    warningTooltip: String? = null,
    tintIcon: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.5f)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null || iconPainter != null) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = if (tintIcon) MaterialTheme.colorScheme.onSurface else Color.Unspecified
                )
            } else {
                Icon(
                    painter = iconPainter!!,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = if (tintIcon) MaterialTheme.colorScheme.onSurface else Color.Unspecified
                )
            }
            Spacer(modifier = Modifier.width(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            TitleAndSummary(title = title, summary = summary)
        }

        if (warningTooltip != null) {
            val tooltipState = rememberTooltipState()
            val scope = rememberCoroutineScope()
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = { PlainTooltip { Text(warningTooltip) } },
                state = tooltipState
            ) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { scope.launch { tooltipState.show() } }
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSwitchItem(
    title: String,
    summary: String? = null,
    icon: ImageVector? = null,
    iconPainter: Painter? = null,
    enabled: Boolean = true,
    checked: Boolean,
    warningTooltip: String? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .alpha(if (enabled) 1f else 0.5f)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null || iconPainter != null) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Icon(
                    painter = iconPainter!!,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            TitleAndSummary(title = title, summary = summary)
        }

        if (warningTooltip != null) {
            val tooltipState = rememberTooltipState()
            val scope = rememberCoroutineScope()
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = { PlainTooltip { Text(warningTooltip) } },
                state = tooltipState
            ) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { scope.launch { tooltipState.show() } }
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        DefaultSwitch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSliderItem(
    title: String,
    summary: String? = null,
    icon: ImageVector? = null,
    iconPainter: Painter? = null,
    enabled: Boolean = true,
    warningTooltip: String? = null,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
    onValueChange: (Float) -> Unit,
    valueSuffix: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null || iconPainter != null) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Icon(
                        painter = iconPainter!!,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                TitleAndSummary(title = title, summary = summary)
            }

            if (warningTooltip != null) {
                val tooltipState = rememberTooltipState()
                val scope = rememberCoroutineScope()
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text(warningTooltip) } },
                    state = tooltipState
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { scope.launch { tooltipState.show() } }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        SimpleTextSlider(
            value = value,
            enabled = enabled,
            onValueChange = onValueChange,
            valueRange = valueRange,
            toInt = true,
            modifier = Modifier.fillMaxWidth(),
            suffix = valueSuffix
        )
    }

}