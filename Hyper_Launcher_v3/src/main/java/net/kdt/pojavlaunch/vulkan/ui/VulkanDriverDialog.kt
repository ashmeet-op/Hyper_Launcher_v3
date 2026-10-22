package net.kdt.pojavlaunch.vulkan.ui

import android.annotation.SuppressLint
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ashmeet.hyperlauncher.components.HyperAlertDialog
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.vulkan.DriverInstaller
import net.kdt.pojavlaunch.vulkan.DriverManager
import net.kdt.pojavlaunch.vulkan.driver.BuiltinDriver
import net.kdt.pojavlaunch.vulkan.driver.DisplayDriver

@SuppressLint("MutableCollectionMutableState")
@Composable
fun VulkanDriverDialog(
    onDismiss: () -> Unit,
    onDriverChanged: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var drivers by remember { mutableStateOf(DriverManager.getDrivers()) }
    var preferredDriver by remember { mutableStateOf(DriverManager.getPreferredDriver()) }

    fun refreshDrivers() {
        drivers = DriverManager.getDrivers()
        preferredDriver = DriverManager.getPreferredDriver()
        onDriverChanged?.invoke()
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            Toast.makeText(context, R.string.driver_config_import_notify, Toast.LENGTH_SHORT).show()
            DriverInstaller.installDriverFromUri(context, uri) {
                refreshDrivers()
            }
        }
    }

    HyperAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = translatedText(stringResource(R.string.driver_config_title))) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(drivers) { driver ->
                        val isSelected = DriverManager.isPreferredDriver(driver)
                        val isBuiltin = driver is BuiltinDriver

                        val titleText = if (isBuiltin) {
                            stringResource(R.string.driver_default_name)
                        } else {
                            driver.name ?: "Unknown Driver"
                        }

                        val summaryText = if (isBuiltin) {
                            stringResource(R.string.driver_default_extra_name)
                        } else if (driver is DisplayDriver) {
                            val author = driver.author ?: ""
                            val version = driver.driverVersion ?: ""
                            if (author.isNotEmpty() || version.isNotEmpty()) "$author - $version" else driver.hash ?: ""
                        } else {
                            driver.hash ?: ""
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    DriverManager.setPreferredDriver(driver)
                                    refreshDrivers()
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Architecture,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = titleText,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (summaryText.isNotEmpty()) {
                                        Text(
                                            text = summaryText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = stringResource(R.string.driver_config_already_used),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    TextButton(
                                        onClick = {
                                            DriverManager.setPreferredDriver(driver)
                                            refreshDrivers()
                                        }
                                    ) {
                                        Text(stringResource(R.string.driver_config_use))
                                    }
                                }

                                if (!isBuiltin && driver.hash != null) {
                                    IconButton(
                                        onClick = {
                                            DriverManager.removeDriver(driver.hash)
                                            refreshDrivers()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.driver_config_delete),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                FilledTonalButton(
                    onClick = { importLauncher.launch("application/zip") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.driver_config_import))
                }
            }
        },
        confirmText = "ok",
        onConfirm = onDismiss
    )
}
