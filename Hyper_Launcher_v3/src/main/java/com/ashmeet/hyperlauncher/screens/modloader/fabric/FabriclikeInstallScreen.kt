package com.ashmeet.hyperlauncher.screens.modloader.fabric

import com.ashmeet.hyperlauncher.utils.translation.translatedText

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import com.ashmeet.hyperlauncher.components.layout.ExperimentalMaterial3ExpressiveApi
import com.ashmeet.hyperlauncher.components.layout.LoadingIndicator
import com.ashmeet.hyperlauncher.components.layout.LinearWavyProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ashmeet.hyperlauncher.components.switch.DefaultSwitch
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.modloaders.FabricVersion

import com.ashmeet.hyperlauncher.utils.installer.ModrinthVersion
import net.kdt.pojavlaunch.modloaders.modpacks.api.ModrinthService

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FabriclikeInstallScreen(
    loaderName: String,
    isLoading: Boolean,
    isInstalling: Boolean,
    gameVersions: List<FabricVersion>,
    loaderVersions: List<FabricVersion>,
    onInstall: (gameVersion: String, loaderVersion: String, isHyperClientEnabled: Boolean, hyperClientVersionId: String?) -> Unit
) {
    var selectedGameVersion by remember { mutableStateOf<FabricVersion?>(null) }
    var selectedLoaderVersion by remember { mutableStateOf<FabricVersion?>(null) }
    var onlyStable by remember { mutableStateOf(true) }
    var isHyperClientEnabled by remember { mutableStateOf(false) }

    var hyperClientVersions by remember { mutableStateOf<List<ModrinthVersion>>(emptyList()) }
    var selectedHyperClientVersion by remember { mutableStateOf<ModrinthVersion?>(null) }
    var isHyperClientLoading by remember { mutableStateOf(false) }

    LaunchedEffect(selectedGameVersion) {
        val gv = selectedGameVersion?.version ?: return@LaunchedEffect
        isHyperClientLoading = true
        try {
            val allVersions = ModrinthService.getProjectVersions("hyperclient")
            val compatible = allVersions.filter { it.gameVersions.contains(gv) && it.loaders.any { l -> l.equals("fabric", ignoreCase = true) } }
            hyperClientVersions = compatible
            selectedHyperClientVersion = compatible.firstOrNull()
        } catch (e: Exception) {
            hyperClientVersions = emptyList()
            selectedHyperClientVersion = null
        } finally {
            isHyperClientLoading = false
        }
    }

    val filteredGameVersions = remember(gameVersions, onlyStable) {
        if (onlyStable) gameVersions.filter { it.stable } else gameVersions
    }

    val filteredLoaderVersions = remember(loaderVersions, onlyStable) {
        if (onlyStable) loaderVersions.filter { it.stable } else loaderVersions
    }

    LaunchedEffect(filteredGameVersions) {
        if (selectedGameVersion == null && filteredGameVersions.isNotEmpty()) {
            selectedGameVersion = filteredGameVersions.first()
        }
    }

    LaunchedEffect(filteredLoaderVersions) {
        if (selectedLoaderVersion == null && filteredLoaderVersions.isNotEmpty()) {
            selectedLoaderVersion = filteredLoaderVersions.first()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (LauncherPreferences.PREF_LAUNCHER_BACKGROUND_PATH != null) Color.Transparent else MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    VersionSpinner(
                        label = translatedText(stringResource(R.string.fabric_dl_game_version)),
                        versions = filteredGameVersions,
                        selectedVersion = selectedGameVersion,
                        onVersionSelected = { selectedGameVersion = it }
                    )

                    VersionSpinner(
                        label = translatedText(stringResource(R.string.fabric_dl_loader_version, loaderName)),
                        versions = filteredLoaderVersions,
                        selectedVersion = selectedLoaderVersion,
                        onVersionSelected = { selectedLoaderVersion = it }
                    )

                    if (loaderName.lowercase() == "fabric" && isHyperClientEnabled && hyperClientVersions.isNotEmpty()) {
                        VersionSpinnerGeneric(
                            label = "Hyper Client Version",
                            items = hyperClientVersions,
                            selectedItem = selectedHyperClientVersion,
                            itemLabel = { it.name },
                            onItemSelected = { selectedHyperClientVersion = it }
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onlyStable = !onlyStable }
                            .padding(8.dp)
                    ) {
                        Text(
                            text = translatedText(stringResource(R.string.fabric_dl_only_stable)),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        DefaultSwitch(
                            checked = onlyStable,
                            onCheckedChange = { onlyStable = it }
                        )
                    }

                    if (loaderName.lowercase() == "fabric") {
                        val isAvailable = hyperClientVersions.isNotEmpty() || isHyperClientLoading
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = isAvailable) { isHyperClientEnabled = !isHyperClientEnabled }
                                .padding(8.dp)
                                .alpha(if (isAvailable) 1f else 0.5f)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = translatedText("Hyper Client"),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    if (!isAvailable && !isHyperClientLoading) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Rounded.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (!isAvailable && !isHyperClientLoading)
                                        "Not available for ${selectedGameVersion?.version ?: "this version"}"
                                    else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (!isAvailable && !isHyperClientLoading) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isHyperClientLoading) {
                                LoadingIndicator(modifier = Modifier.size(20.dp))
                            } else {
                                DefaultSwitch(
                                    checked = isHyperClientEnabled && isAvailable,
                                    enabled = isAvailable,
                                    onCheckedChange = { isHyperClientEnabled = it }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val gv = selectedGameVersion?.version
                            val lv = selectedLoaderVersion?.version
                            if (gv != null && lv != null) {
                                onInstall(gv, lv, isHyperClientEnabled, selectedHyperClientVersion?.id)
                            }
                        },
                        enabled = !isLoading && !isInstalling && selectedGameVersion != null && selectedLoaderVersion != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        if (isInstalling) {
                            LoadingIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(translatedText(stringResource(R.string.global_save)))
                        }
                    }
                }
            }

            if (isLoading) {
                LinearWavyProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun VersionSpinner(
    label: String,
    versions: List<FabricVersion>,
    selectedVersion: FabricVersion?,
    onVersionSelected: (FabricVersion) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedVersion?.version ?: "",
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            enabled = true,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.4f)
        ) {
            versions.forEach { version ->
                DropdownMenuItem(
                    text = { Text(version.version) },
                    onClick = {
                        onVersionSelected(version)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun <T> VersionSpinnerGeneric(
    label: String,
    items: List<T>,
    selectedItem: T?,
    itemLabel: (T) -> String,
    onItemSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedItem?.let { itemLabel(it) } ?: "",
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            enabled = true,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.4f)
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(itemLabel(item)) },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}
