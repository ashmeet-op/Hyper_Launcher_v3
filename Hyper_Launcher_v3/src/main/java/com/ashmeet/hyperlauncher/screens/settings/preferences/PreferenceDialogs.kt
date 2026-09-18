package com.ashmeet.hyperlauncher.screens.settings.preferences

import com.ashmeet.hyperlauncher.utils.translation.translatedText

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ashmeet.hyperlauncher.components.slider.SimpleTextSlider
import io.ktor.http.ContentType

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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            LazyColumn {
                items(options.size) { index ->
                    val value = optionValues[index]
                    val label = options[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onValueChange(value)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = value == selectedValue,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
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
        confirmButton = {
            TextButton(onClick = onAddRuntime) {
                Text(translatedText(stringResource(R.string.multirt_config_add)))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onToggleDeleteMode) {
                    Text(stringResource(if (isDeleting) R.string.multirt_config_setdefault else R.string.global_delete))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true

            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
fun PointerHotspotPickerDialog(
    title: String,
    imagePath: String?,
    initialX: Float,
    initialY: Float,
    onConfirm: (Float, Float) -> Unit,
    onDismiss: () -> Unit
) {
    var hotspotX by remember { mutableFloatStateOf(initialX) }
    var hotspotY by remember { mutableFloatStateOf(initialY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
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
                            painter = painterResource(id = R.drawable.img_mouse_pointer_arrow),
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
        confirmButton = {
            TextButton(onClick = { onConfirm(hotspotX, hotspotY) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
