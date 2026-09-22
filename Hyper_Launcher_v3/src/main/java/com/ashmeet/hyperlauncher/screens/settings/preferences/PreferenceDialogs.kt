package com.ashmeet.hyperlauncher.screens.settings.preferences

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ashmeet.hyperlauncher.components.HyperAlertDialog
import com.ashmeet.hyperlauncher.components.HyperOutlinedTextField
import com.ashmeet.hyperlauncher.components.slider.SimpleTextSlider
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.multirt.Runtime
import java.io.File

@Composable
fun SingleChoiceDialog(
    title: String,
    options: List<String>,
    optionValues: List<String>,
    selectedValue: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var tempValue by remember { mutableStateOf(selectedValue) }

    HyperAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = translatedText(title)) },
        text = {
            LazyColumn {
                items(options.size) { index ->
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
        },
        confirmText = stringResource(android.R.string.ok),
        onConfirm = {
            onValueChange(tempValue)
            onDismiss()
        },
        dismissText = stringResource(android.R.string.cancel),
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
                                text = runtime.name.replace(".tar.xz", "").replace("-", " "),
                                style = MaterialTheme.typography.bodyLarge
                            )
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
                        if (isDeleting) {
                            IconButton(onClick = { onRuntimeDelete(runtime) }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
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
            val scrollState = rememberScrollState()
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
