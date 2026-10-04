package com.ashmeet.hyperlauncher.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ashmeet.hyperlauncher.components.HyperAlertDialog
import com.ashmeet.hyperlauncher.components.switch.DefaultSwitch
import com.ashmeet.hyperlauncher.plugin.renderer_v2.RendererV2Data
import com.ashmeet.hyperlauncher.plugin.renderer_v2.RendererV2PluginManager
import com.ashmeet.hyperlauncher.plugin.renderer_v2.data.EnvSettingUnit
import com.ashmeet.hyperlauncher.screens.settings.layouts.CardPosition
import com.ashmeet.hyperlauncher.screens.settings.layouts.SettingsCard
import com.ashmeet.hyperlauncher.screens.settings.layouts.SettingsScreenWrapper
import com.ashmeet.hyperlauncher.screens.settings.preferences.PreferenceCategory
import com.ashmeet.hyperlauncher.screens.settings.preferences.SettingsActionItem
import com.ashmeet.hyperlauncher.screens.settings.preferences.SettingsSwitchItem
import com.ashmeet.hyperlauncher.screens.settings.preferences.SingleChoiceDialog
import com.ashmeet.hyperlauncher.screens.settings.preferences.TextInputDialog
import com.ashmeet.hyperlauncher.utils.translation.translatedText

@Composable
fun RendererConfigScreen(
    onBack: () -> Unit
) {
    val plugins = remember { RendererV2PluginManager.getRendererList() }
    var selectedPluginIndex by remember { mutableStateOf(0) }
    var showPluginSelectDialog by remember { mutableStateOf(false) }

    var activeSelectableUnit by remember { mutableStateOf<EnvSettingUnit.Selectable?>(null) }
    var activeCustomizableUnit by remember { mutableStateOf<EnvSettingUnit.Customizable?>(null) }

    SettingsScreenWrapper(
        title = translatedText("Renderer Plugin Settings"),
        onBack = onBack,
        addTopGap = true
    ) {
        if (plugins.isEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SettingsCard(position = CardPosition.SINGLE, useSurface = true) {
                    SettingsActionItem(
                        title = translatedText("No Renderer Plugins Found"),
                        summary = translatedText("Install a renderer plugin (V2) to configure environment settings."),
                        icon = Icons.Default.Tune,
                        onClick = {}
                    )
                }
            }
        } else {
            val currentPlugin: RendererV2Data? = plugins.getOrNull(selectedPluginIndex.coerceIn(0, plugins.lastIndex))

            if (plugins.size > 1) {
                PreferenceCategory(title = translatedText("Selected Plugin"))
                SettingsCard(position = CardPosition.SINGLE, useSurface = true) {
                    SettingsActionItem(
                        title = currentPlugin?.renderer?.displayName ?: translatedText("Select Plugin"),
                        summary = currentPlugin?.summary ?: translatedText("Choose which renderer plugin to configure"),
                        icon = Icons.Default.Build,
                        onClick = { showPluginSelectDialog = true }
                    )
                }
            }

            if (currentPlugin != null) {
                val configurableUnits = remember(currentPlugin) {
                    currentPlugin.env.getConfigurableUnits()
                }

                PreferenceCategory(title = translatedText("${currentPlugin.renderer.displayName} Options"))

                if (configurableUnits.isEmpty()) {
                    SettingsCard(position = CardPosition.SINGLE, useSurface = true) {
                        SettingsActionItem(
                            title = translatedText("No Configurable Options"),
                            summary = translatedText("This plugin has no configurable environment variables."),
                            icon = Icons.Default.Tune,
                            onClick = {}
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        configurableUnits.forEachIndexed { index, unit ->
                            val position = when {
                                configurableUnits.size == 1 -> CardPosition.SINGLE
                                index == 0 -> CardPosition.TOP
                                index == configurableUnits.lastIndex -> CardPosition.BOTTOM
                                else -> CardPosition.MIDDLE
                            }

                            val title = unit.summary?.takeIf { it.isNotBlank() } ?: unit.key

                            SettingsCard(position = position, useSurface = true) {
                                when (unit) {
                                    is EnvSettingUnit.Selectable -> {
                                        val displaySummary = if (unit.rawEnv.check != null && !unit.isEnabled) {
                                            translatedText("Disabled")
                                        } else {
                                            translatedText("Value: ${unit.state}")
                                        }

                                        SettingsActionItem(
                                            title = title,
                                            summary = displaySummary,
                                            icon = Icons.AutoMirrored.Filled.List,
                                            onClick = { activeSelectableUnit = unit }
                                        )
                                    }

                                    is EnvSettingUnit.Customizable -> {
                                        SettingsActionItem(
                                            title = title,
                                            summary = if (unit.state.isNotEmpty()) unit.state else translatedText("Default: ${unit.defaultValue}"),
                                            icon = Icons.Default.Edit,
                                            onClick = { activeCustomizableUnit = unit }
                                        )
                                    }

                                    is EnvSettingUnit.Toggleable -> {
                                        SettingsSwitchItem(
                                            title = title,
                                            summary = if (unit.isEnabled) translatedText("Enabled") else translatedText("Disabled"),
                                            icon = Icons.Default.Tune,
                                            checked = unit.isEnabled,
                                            onCheckedChange = { checked ->
                                                unit.save(if (checked) unit.envValue else "")
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPluginSelectDialog && plugins.size > 1) {
        SingleChoiceDialog(
            title = translatedText("Select Renderer Plugin"),
            options = plugins.map { it.renderer.displayName },
            optionValues = plugins.indices.map { it.toString() },
            selectedValue = selectedPluginIndex.toString(),
            onValueChange = { newValue ->
                selectedPluginIndex = newValue.toIntOrNull() ?: 0
            },
            onDismiss = { showPluginSelectDialog = false }
        )
    }

    activeSelectableUnit?.let { unit ->
        SelectableConfigDialog(
            unit = unit,
            onDismiss = { activeSelectableUnit = null }
        )
    }

    activeCustomizableUnit?.let { unit ->
        TextInputDialog(
            title = unit.summary?.takeIf { it.isNotBlank() } ?: unit.key,
            initialValue = unit.state,
            onConfirm = { newValue ->
                unit.save(newValue)
                activeCustomizableUnit = null
            },
            onDismiss = { activeCustomizableUnit = null }
        )
    }
}

@Composable
private fun SelectableConfigDialog(
    unit: EnvSettingUnit.Selectable,
    onDismiss: () -> Unit
) {
    var enabled by remember { mutableStateOf(unit.isEnabled) }
    var tempValue by remember { mutableStateOf(unit.state) }
    val title = unit.summary?.takeIf { it.isNotBlank() } ?: unit.key

    HyperAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = translatedText(title)) },
        text = {
            Column {
                if (unit.rawEnv.check != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = translatedText("Enable variable"),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        DefaultSwitch(
                            checked = enabled,
                            onCheckedChange = {
                                enabled = it
                                unit.saveCheck(it)
                            }
                        )
                    }
                }
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(unit.values.size) { index ->
                        val value = unit.values[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = enabled) {
                                    tempValue = value
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = value == tempValue,
                                onClick = null,
                                enabled = enabled
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        },
        confirmText = "ok",
        onConfirm = {
            unit.save(tempValue)
            onDismiss()
        },
        dismissText = "cancel",
        onDismiss = onDismiss
    )
}
