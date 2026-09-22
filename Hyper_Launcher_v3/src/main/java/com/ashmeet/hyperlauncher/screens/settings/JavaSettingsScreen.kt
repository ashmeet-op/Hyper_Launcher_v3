package com.ashmeet.hyperlauncher.screens.settings

import com.ashmeet.hyperlauncher.utils.translation.translatedText

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.ashmeet.hyperlauncher.components.HyperAlertDialog
import com.ashmeet.hyperlauncher.screens.settings.layouts.CardPosition
import com.ashmeet.hyperlauncher.screens.settings.layouts.SettingsCard
import com.ashmeet.hyperlauncher.screens.settings.layouts.SettingsScreenWrapper
import com.ashmeet.hyperlauncher.screens.settings.preferences.RuntimeSelectionDialog
import com.ashmeet.hyperlauncher.screens.settings.preferences.SettingsActionItem
import com.ashmeet.hyperlauncher.screens.settings.preferences.SettingsSliderItem
import com.ashmeet.hyperlauncher.screens.settings.preferences.SettingsSwitchItem
import com.ashmeet.hyperlauncher.screens.settings.preferences.TextInputDialog
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.multirt.MultiRTUtils
import net.kdt.pojavlaunch.multirt.Runtime
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences

@Composable
fun JavaSettingsScreen(
    onBack: () -> Unit,
    onAddRuntime: () -> Unit,
    onDeleteRuntime: (Runtime, onDeleted: () -> Unit) -> Unit,
    maxRam: Int
) {
    val context = LocalContext.current
    var ramAllocation by remember { mutableFloatStateOf(LauncherPreferences.PREF_RAM_ALLOCATION.toFloat()) }
    var javaSandbox by remember { mutableStateOf(LauncherPreferences.PREF_JAVA_SANDBOX) }
    var defaultRuntimeName by remember { mutableStateOf(LauncherPreferences.PREF_DEFAULT_RUNTIME ?: "") }
    var javaArgs by remember { mutableStateOf(LauncherPreferences.PREF_CUSTOM_JAVA_ARGS ?: "") }

    var runtimes by remember { mutableStateOf(MultiRTUtils.getRuntimes()) }
    var showRuntimeDialog by remember { mutableStateOf(false) }
    var isDeletingRuntimes by remember { mutableStateOf(false) }
    var showJavaArgsDialog by remember { mutableStateOf(false) }
    var errorDialogMessage by remember { mutableStateOf<String?>(null) }

    SettingsScreenWrapper(
        title = translatedText(stringResource(R.string.preference_java_title)),
        onBack = onBack,
        addTopGap = true
    ) {
        val currentRuntime = remember(defaultRuntimeName) {
            if (defaultRuntimeName.isNotEmpty()) {
                MultiRTUtils.read(defaultRuntimeName)
            } else null
        }
        val runtimeSummary = remember(currentRuntime) {
            if (currentRuntime?.versionString != null) {
                currentRuntime.name.replace(".tar.xz", "").replace("-", " ") + " (" + currentRuntime.versionString + ")"
            } else currentRuntime?.name ?: ""
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingsCard(position = CardPosition.TOP, useSurface = true) {
                SettingsActionItem(
                    title = translatedText(stringResource(R.string.multirt_title)),
                    summary = runtimeSummary.ifEmpty { translatedText(stringResource(R.string.multirt_subtitle)) },
                    icon = Icons.Default.Coffee,
                    onClick = { showRuntimeDialog = true }
                )
            }

            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsActionItem(
                    title = translatedText(stringResource(R.string.mcl_setting_title_javaargs)),
                    summary = javaArgs.ifEmpty { translatedText(stringResource(R.string.mcl_setting_subtitle_javaargs)) },
                    icon = Icons.Default.Terminal,
                    warningTooltip = "Improper JVM arguments can cause the game to crash or perform poorly. Only modify if you know what you are doing.",
                    onClick = { showJavaArgsDialog = true }
                )
            }

            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsSliderItem(
                    title = translatedText(stringResource(R.string.mcl_memory_allocation)),
                    summary = translatedText(stringResource(R.string.mcl_memory_allocation_subtitle)),
                    icon = Icons.Default.Memory,
                    value = ramAllocation,
                    valueRange = integerResource(R.integer.memory_seekbar_min).toFloat()..maxRam.toFloat(),
                    onValueChange = {
                        ramAllocation = it
                        LauncherPreferences.prefs.edit { putInt("allocation", it.toInt()) }
                        LauncherPreferences.loadPreferences(context)
                    },
                    valueSuffix = " MB"
                )
            }

            SettingsCard(position = CardPosition.BOTTOM, useSurface = true) {
                SettingsSwitchItem(
                    title = translatedText(stringResource(R.string.mcl_setting_java_sandbox)),
                    summary = translatedText(stringResource(R.string.mcl_setting_java_sandbox_subtitle)),
                    icon = Icons.Default.Code,
                    checked = javaSandbox,
                    onCheckedChange = {
                        javaSandbox = it
                        LauncherPreferences.prefs.edit { putBoolean("java_sandbox", it) }
                        LauncherPreferences.loadPreferences(context)
                    }
                )
            }
        }
    }

    if (showRuntimeDialog) {
        RuntimeSelectionDialog(
            title = translatedText(stringResource(R.string.multirt_title)),
            runtimes = runtimes,
            selectedRuntimeName = defaultRuntimeName,
            isDeleting = isDeletingRuntimes,
            onRuntimeSelected = { runtime ->
                defaultRuntimeName = runtime.name
                LauncherPreferences.prefs.edit { putString("defaultRuntime", runtime.name) }
                LauncherPreferences.loadPreferences(context)
                showRuntimeDialog = false
            },
            onRuntimeDelete = { runtime ->
                if (runtimes.size < 2) {
                    errorDialogMessage = context.getString(R.string.multirt_config_removeerror_last)
                } else {
                    onDeleteRuntime(runtime) {
                        runtimes = MultiRTUtils.getRuntimes()
                    }
                }
            },
            onAddRuntime = {
                onAddRuntime()
                runtimes = MultiRTUtils.getRuntimes()
            },
            onToggleDeleteMode = { isDeletingRuntimes = !isDeletingRuntimes },
            onDismiss = {
                showRuntimeDialog = false
                isDeletingRuntimes = false
            }
        )
    }

    if (showJavaArgsDialog) {
        TextInputDialog(
            title = translatedText(stringResource(R.string.mcl_setting_title_javaargs)),
            initialValue = javaArgs,
            onConfirm = { newValue ->
                javaArgs = newValue
                LauncherPreferences.prefs.edit { putString("javaArgs", newValue) }
                LauncherPreferences.loadPreferences(context)
                showJavaArgsDialog = false
            },
            onDismiss = { showJavaArgsDialog = false }
        )
    }

    if (errorDialogMessage != null) {
        HyperAlertDialog(
            onDismissRequest = { errorDialogMessage = null },
            title = { Text(translatedText(stringResource(R.string.global_error))) },
            text = { Text(translatedText(errorDialogMessage!!)) },
            confirmText = stringResource(android.R.string.ok),
            onConfirm = { errorDialogMessage = null }
        )
    }
}
