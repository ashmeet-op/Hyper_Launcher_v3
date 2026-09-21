package com.ashmeet.hyperlauncher.screens.settings

import com.ashmeet.hyperlauncher.utils.translation.translatedText

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Verified
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.ashmeet.hyperlauncher.screens.settings.layouts.CardPosition
import com.ashmeet.hyperlauncher.screens.settings.layouts.SettingsCard
import com.ashmeet.hyperlauncher.screens.settings.layouts.SettingsScreenWrapper
import com.ashmeet.hyperlauncher.screens.settings.preferences.SettingsActionItem
import com.ashmeet.hyperlauncher.screens.settings.preferences.SettingsSwitchItem
import com.ashmeet.hyperlauncher.screens.settings.preferences.SingleChoiceDialog
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences

@Composable
fun MiscSettingsScreen(
    onBack: () -> Unit,
    isMicrophonePermissionGranted: Boolean,
    onMicrophoneAccessClick: () -> Unit,
    onRunDataMigrationClick: () -> Unit
) {
    val context = LocalContext.current
    var checkGameFiles by remember { mutableStateOf(LauncherPreferences.PREF_VERIFY_FILES) }
    var fastStartupCheck by remember { mutableStateOf(LauncherPreferences.PREF_RAPID_START) }
    var downloadSource by remember { mutableStateOf(LauncherPreferences.PREF_DOWNLOAD_SOURCE) }
    var verifyManifest by remember { mutableStateOf(LauncherPreferences.PREF_VERIFY_MANIFEST) }

    var showDownloadSourceDialog by remember { mutableStateOf(false) }

    if (showDownloadSourceDialog) {
        val names = stringArrayResource(R.array.download_source_names)
        val values = stringArrayResource(R.array.download_source_values)
        SingleChoiceDialog(
            title = translatedText(stringResource(R.string.preference_download_source_title)),
            options = names.toList(),
            optionValues = values.toList(),
            selectedValue = downloadSource,
            onValueChange = { newValue ->
                downloadSource = newValue
                LauncherPreferences.prefs.edit { putString("downloadSource", newValue) }
                LauncherPreferences.loadPreferences(context)
            },
            onDismiss = { showDownloadSourceDialog = false }
        )
    }

    SettingsScreenWrapper(
        title = translatedText(stringResource(R.string.preference_misc_title)),
        onBack = onBack,
        addTopGap = true
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingsCard(position = CardPosition.TOP, useSurface = true) {
                SettingsSwitchItem(
                    title = translatedText(stringResource(R.string.preference_verify_game_files_title)),
                    summary = translatedText(stringResource(R.string.preference_verify_game_files_description)),
                    icon = Icons.Default.Numbers,
                    checked = checkGameFiles,
                    onCheckedChange = {
                        checkGameFiles = it
                        LauncherPreferences.prefs.edit { putBoolean("checkGameFiles", it) }
                        LauncherPreferences.loadPreferences(context)
                    }
                )
            }

            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsSwitchItem(
                    title = translatedText(stringResource(R.string.preference_go_vroom_title)),
                    summary = translatedText(stringResource(R.string.preference_go_vroom_description)),
                    icon = Icons.Default.Bolt,
                    checked = fastStartupCheck,
                    onCheckedChange = {
                        fastStartupCheck = it
                        LauncherPreferences.prefs.edit { putBoolean("fastStartupCheck", it) }
                        LauncherPreferences.loadPreferences(context)
                    }
                )
            }

            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsActionItem(
                    title = translatedText(stringResource(R.string.preference_download_source_title)),
                    summary = downloadSource,
                    icon = Icons.Default.Download,
                    onClick = { showDownloadSourceDialog = true }
                )
            }

            SettingsCard(position = CardPosition.BOTTOM, useSurface = true) {
                SettingsSwitchItem(
                    title = translatedText(stringResource(R.string.preference_verify_manifest_title)),
                    summary = translatedText(stringResource(R.string.preference_verify_manifest_description)),
                    icon = Icons.Default.Verified,
                    checked = verifyManifest,
                    onCheckedChange = {
                        verifyManifest = it
                        LauncherPreferences.prefs.edit { putBoolean("verifyManifest", it) }
                        LauncherPreferences.loadPreferences(context)
                    }
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val visibleItems = mutableListOf<@Composable () -> Unit>()

            if (!isMicrophonePermissionGranted) {
                visibleItems.add {
                    SettingsActionItem(
                        title = translatedText(stringResource(R.string.preference_microphone_access_title)),
                        summary = translatedText(stringResource(R.string.preference_microphone_access_description)),
                        icon = Icons.Default.Mic,
                        onClick = onMicrophoneAccessClick
                    )
                }
            }

            visibleItems.add {
                SettingsActionItem(
                    title = translatedText(stringResource(R.string.preference_data_migration_title)),
                    summary = translatedText(stringResource(R.string.preference_data_migration_summary)),
                    icon = Icons.AutoMirrored.Filled.Input,
                    warningTooltip = "Data migration may overwrite existing instances and control maps. Ensure you have backups before proceeding.",
                    onClick = onRunDataMigrationClick
                )
            }

            visibleItems.forEachIndexed { index, content ->
                val position = when {
                    visibleItems.size == 1 -> CardPosition.SINGLE
                    index == 0 -> CardPosition.TOP
                    index == visibleItems.size - 1 -> CardPosition.BOTTOM
                    else -> CardPosition.MIDDLE
                }
                SettingsCard(position = position, useSurface = true) {
                    content()
                }
            }
        }
    }
}
