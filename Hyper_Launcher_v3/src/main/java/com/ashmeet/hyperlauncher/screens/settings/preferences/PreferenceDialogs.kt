package com.ashmeet.hyperlauncher.screens.settings.preferences

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.edit
import com.ashmeet.hyperlauncher.components.switch.DefaultSwitch
import com.ashmeet.hyperlauncher.utils.HyperUtils
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ashmeet.hyperlauncher.components.HyperAlertDialog
import com.ashmeet.hyperlauncher.components.HyperOutlinedTextField
import com.ashmeet.hyperlauncher.components.slider.SimpleTextSlider
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperlauncher.runtime.Runtime
import java.io.File

@Composable
fun SingleChoiceDialog(
    title: String,
    options: List<String>,
    optionValues: List<String>,
    selectedValue: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    searchable: Boolean = false
) {
    var tempValue by remember { mutableStateOf(selectedValue) }
    val searchQuery = remember { androidx.compose.foundation.text.input.TextFieldState() }

    val filteredIndices = remember(options, optionValues, searchable, searchQuery.text.toString()) {
        if (!searchable || searchQuery.text.isBlank()) {
            options.indices.toList()
        } else {
            val query = searchQuery.text.toString().lowercase()
            options.indices.filter { index ->
                options[index].lowercase().contains(query) || optionValues[index].lowercase().contains(query)
            }
        }
    }

    HyperAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = translatedText(title)) },
        text = {
            Column {
                if (searchable) {
                    com.ashmeet.hyperlauncher.components.HyperSearchBar(
                        state = searchQuery,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        label = "Search..."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(filteredIndices.size) { filteredIndex ->
                        val index = filteredIndices[filteredIndex]
                        val value = optionValues[index]
                        val label = options[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    tempValue = value
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = value == tempValue,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                if (tempValue == "mobileglues") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Warning: MobileGlues does not support 26.3. Please switch to LTW.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmText = "ok",
        onConfirm = {
            onValueChange(tempValue)
            onDismiss()
        },
        dismissText = "cancel",
        onDismiss = onDismiss
    )
}

@Composable
fun RuntimeSelectionDialog(
    title: String,
    runtimes: List<Runtime>,
    selectedRuntimeName: String,
    isDeleting: Boolean,
    onRuntimeSelected: (Runtime) -> Unit,
    onRuntimeDelete: (Runtime) -> Unit,
    onAddRuntime: () -> Unit,
    onToggleDeleteMode: () -> Unit,
    onDismiss: () -> Unit
) {
    HyperAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(800.dp),
        title = { Text(text = translatedText(title)) },
        text = {
            LazyColumn {
                items(runtimes) { runtime ->
                    val isDefault = runtime.name == selectedRuntimeName
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isDeleting) {
                                if (!isDefault) onRuntimeSelected(runtime)
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (runtime.name == "auto") translatedText(stringResource(R.string.multirt_auto))
                                       else runtime.name.replace(".tar.xz", "").replace("-", " "),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (runtime.name != "auto") {
                                if (runtime.versionString != null) {
                                    Text(
                                        text = runtime.versionString,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Text(
                                        text = translatedText(stringResource(R.string.multirt_runtime_corrupt)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        if (isDeleting) {
                            if (runtime.name != "auto") {
                                IconButton(onClick = { onRuntimeDelete(runtime) }) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        } else if (isDefault) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        },
        buttons = {
            FilledTonalButton(onClick = onToggleDeleteMode) {
                Text(
                    text = translatedText(stringResource(if (isDeleting) android.R.string.ok else R.string.global_delete)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            FilledTonalButton(onClick = onDismiss) {
                Text(
                    text = translatedText(stringResource(android.R.string.cancel)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Button(onClick = onAddRuntime) {
                Text(
                    text = translatedText(stringResource(R.string.multirt_config_add)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    )
}

@Composable
fun TextViewerDialog(
    title: String,
    content: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(content) }

    HyperAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(min = 400.dp, max = 600.dp),
        title = { Text(text = translatedText(title)) },
        text = {
            HyperOutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                label = { Text(translatedText("Content")) },
                singleLine = false,
                textStyle = MaterialTheme.typography.bodySmall
            )
        },
        confirmText = stringResource(R.string.global_save),
        onConfirm = {
            onSave(text)
            onDismiss()
        },
        dismissText = stringResource(android.R.string.cancel),
        onDismiss = onDismiss
    )
}

@Composable
fun TextInputDialog(
    title: String,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }

    HyperAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = translatedText(title)) },
        text = {
            HyperOutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(translatedText(title)) },
                singleLine = true
            )
        },
        confirmText = stringResource(android.R.string.ok),
        onConfirm = { onConfirm(text) },
        dismissText = stringResource(android.R.string.cancel),
        onDismiss = onDismiss
    )
}

@Composable
fun PointerHotspotPickerDialog(
    title: String,
    imagePath: String?,
    shapeId: Int = 0,
    initialX: Float,
    initialY: Float,
    onConfirm: (Float, Float) -> Unit,
    onDismiss: () -> Unit
) {
    var hotspotX by remember { mutableFloatStateOf(initialX) }
    var hotspotY by remember { mutableFloatStateOf(initialY) }

    HyperAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = translatedText(title)) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    if (imagePath != null) {
                        AsyncImage(
                            model = File(imagePath),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.TopStart
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = getDefaultCursorDrawable(shapeId)),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val x = (hotspotX / 100f) * size.width
                        val y = (hotspotY / 100f) * size.height
                        drawCircle(
                            color = Color.Red,
                            radius = 4.dp.toPx(),
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column {
                        Text(text = translatedText("Hotspot X"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        SimpleTextSlider(
                            value = hotspotX,
                            onValueChange = { hotspotX = it },
                            valueRange = 0f..100f,
                            toInt = true,
                            suffix = "%",
                            modifier = Modifier.fillMaxWidth(),
                            shorter = true
                        )
                    }

                    Column {
                        Text(text = translatedText("Hotspot Y"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        SimpleTextSlider(
                            value = hotspotY,
                            onValueChange = { hotspotY = it },
                            valueRange = 0f..100f,
                            toInt = true,
                            suffix = "%",
                            modifier = Modifier.fillMaxWidth(),
                            shorter = true
                        )
                    }
                }
            }
        },
        confirmText = stringResource(android.R.string.ok),
        onConfirm = { onConfirm(hotspotX, hotspotY) },
        dismissText = stringResource(android.R.string.cancel),
        onDismiss = onDismiss
    )
}

data class ResolutionPreset(val ratio: Int, val label: String)

@SuppressLint("LocalContextResourcesRead")
@Composable
fun ResolutionSelectionDialog(
    title: String,
    currentRatio: Int,
    minRatio: Int = 25,
    onResolutionSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val metrics = context.resources.displayMetrics
    val maxSide = maxOf(metrics.widthPixels, metrics.heightPixels)
    val minSide = minOf(metrics.widthPixels, metrics.heightPixels)

    fun calculateResolution(ratio: Int): Pair<Int, Int> {
        val scale = ratio.coerceIn(minRatio, 100) / 100f
        val w = HyperUtils.getDisplayFriendlyRes(maxSide, scale)
        val h = HyperUtils.getDisplayFriendlyRes(minSide, scale)
        return Pair(w, h)
    }

    val presets = remember {
        listOf(
            ResolutionPreset(100, "100% (Native)"),
            ResolutionPreset(85, "85% (High)"),
            ResolutionPreset(75, "75% (Balanced)"),
            ResolutionPreset(60, "60% (Medium)"),
            ResolutionPreset(50, "50% (Performance)"),
            ResolutionPreset(35, "35% (Low)"),
            ResolutionPreset(25, "25% (Minimum)")
        )
    }

    val isCurrentPreset = remember(currentRatio) { presets.any { it.ratio == currentRatio } }
    var isCustomSelected by remember { mutableStateOf(!isCurrentPreset) }
    var selectedPresetRatio by remember {
        mutableIntStateOf(if (isCurrentPreset) currentRatio else presets.first().ratio)
    }
    var customRatio by remember {
        mutableFloatStateOf(currentRatio.toFloat().coerceIn(minRatio.toFloat(), 100f))
    }

    val effectiveRatio = if (isCustomSelected) customRatio.toInt() else selectedPresetRatio
    val (currentW, currentH) = calculateResolution(effectiveRatio)

    HyperAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = translatedText(title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${translatedText("Current")}: ${effectiveRatio}% (${currentW} × ${currentH})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(presets) { preset ->
                    val (w, h) = calculateResolution(preset.ratio)
                    val isSelected = !isCustomSelected && selectedPresetRatio == preset.ratio
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                isCustomSelected = false
                                selectedPresetRatio = preset.ratio
                            },
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        } else {
                            Color.Transparent
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    isCustomSelected = false
                                    selectedPresetRatio = preset.ratio
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = "$w × $h",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                isCustomSelected = true
                            },
                        color = if (isCustomSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        } else {
                            Color.Transparent
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isCustomSelected,
                                    onClick = { isCustomSelected = true }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = translatedText("Custom Resolution"),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    val (cw, ch) = calculateResolution(customRatio.toInt())
                                    Text(
                                        text = "${customRatio.toInt()}% ($cw × $ch)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (isCustomSelected) {
                                Spacer(modifier = Modifier.height(8.dp))
                                SimpleTextSlider(
                                    value = customRatio,
                                    onValueChange = { customRatio = it },
                                    valueRange = minRatio.toFloat()..100f,
                                    toInt = true,
                                    suffix = "%",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmText = stringResource(android.R.string.ok),
        onConfirm = {
            val finalRatio = if (isCustomSelected) customRatio.toInt() else selectedPresetRatio
            onResolutionSelected(finalRatio.coerceIn(minRatio, 100))
            onDismiss()
        },
        dismissText = stringResource(android.R.string.cancel),
        onDismiss = onDismiss
    )
}

@SuppressLint("LocalContextResourcesRead")
@Composable
fun RecorderSettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var recordMicSound by remember { mutableStateOf(LauncherPreferences.PREF_RECORD_MIC_SOUND) }
    var autoSaveGallery by remember { mutableStateOf(LauncherPreferences.PREF_AUTO_SAVE_TO_GALLERY) }
    var recordResolutionRatio by remember { mutableIntStateOf(LauncherPreferences.PREF_RECORD_RESOLUTION_RATIO) }
    var showResolutionDialog by remember { mutableStateOf(false) }

    val metrics = context.resources.displayMetrics
    val maxSide = maxOf(metrics.widthPixels, metrics.heightPixels)
    val minSide = minOf(metrics.widthPixels, metrics.heightPixels)
    val resW = HyperUtils.getDisplayFriendlyRes(maxSide, recordResolutionRatio / 100f)
    val resH = HyperUtils.getDisplayFriendlyRes(minSide, recordResolutionRatio / 100f)

    HyperAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = translatedText("Recorder Settings"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showResolutionDialog = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = translatedText("Recording Resolution"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$recordResolutionRatio% ($resW × $resH)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    FilledTonalButton(
                        onClick = { showResolutionDialog = true }
                    ) {
                        Text(
                            text = translatedText("Change"),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            recordMicSound = !recordMicSound
                            LauncherPreferences.PREF_RECORD_MIC_SOUND = recordMicSound
                            LauncherPreferences.prefs.edit { putBoolean("record_mic_sound", recordMicSound) }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = translatedText("Record Microphone Sound"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = translatedText("Record mic audio along with game video"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DefaultSwitch(
                        checked = recordMicSound,
                        onCheckedChange = { checked ->
                            recordMicSound = checked
                            LauncherPreferences.PREF_RECORD_MIC_SOUND = checked
                            LauncherPreferences.prefs.edit { putBoolean("record_mic_sound", checked) }
                        }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            autoSaveGallery = !autoSaveGallery
                            LauncherPreferences.PREF_AUTO_SAVE_TO_GALLERY = autoSaveGallery
                            LauncherPreferences.prefs.edit { putBoolean("auto_save_to_gallery", autoSaveGallery) }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = translatedText("Always Save to Gallery"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = translatedText("Automatically save new game recordings to your Gallery"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DefaultSwitch(
                        checked = autoSaveGallery,
                        onCheckedChange = { checked ->
                            autoSaveGallery = checked
                            LauncherPreferences.PREF_AUTO_SAVE_TO_GALLERY = checked
                            LauncherPreferences.prefs.edit { putBoolean("auto_save_to_gallery", checked) }
                        }
                    )
                }
            }
        },
        confirmText = translatedText("Done"),
        onConfirm = onDismiss
    )

    if (showResolutionDialog) {
        ResolutionSelectionDialog(
            title = "Recording Resolution",
            currentRatio = recordResolutionRatio,
            onResolutionSelected = { newRatio ->
                recordResolutionRatio = newRatio
                LauncherPreferences.PREF_RECORD_RESOLUTION_RATIO = newRatio
                LauncherPreferences.prefs.edit { putInt("record_resolution_ratio", newRatio) }
            },
            onDismiss = { showResolutionDialog = false }
        )
    }
}
