package com.ashmeet.hyperlauncher.screens.settings

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.ashmeet.hyperlauncher.screens.settings.layouts.CardPosition
import com.ashmeet.hyperlauncher.screens.settings.layouts.SettingsCard
import com.ashmeet.hyperlauncher.screens.settings.layouts.SettingsScreenWrapper
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.screens.settings.preferences.ResolutionSelectionDialog
import com.ashmeet.hyperlauncher.screens.settings.preferences.SettingsActionItem
import com.ashmeet.hyperlauncher.screens.settings.preferences.SettingsSwitchItem
import com.ashmeet.hyperlauncher.utils.HyperUtils
import com.ashmeet.hyperlauncher.utils.translation.translatedText

@SuppressLint("LocalContextResourcesRead")
@Composable
fun RecorderSettingsScreen(
    onBack: () -> Unit
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

    SettingsScreenWrapper(
        title = translatedText("Recorder Settings"),
        onBack = onBack,
        addTopGap = true
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingsCard(position = CardPosition.TOP, useSurface = true) {
                SettingsActionItem(
                    title = translatedText("Recording Resolution"),
                    summary = "$recordResolutionRatio% ($resW × $resH)",
                    icon = Icons.Default.Monitor,
                    onClick = { showResolutionDialog = true }
                )
            }

            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsSwitchItem(
                    title = translatedText("Record Microphone Sound"),
                    summary = translatedText("Record mic audio along with game video"),
                    icon = Icons.Default.Mic,
                    checked = recordMicSound,
                    onCheckedChange = { checked ->
                        recordMicSound = checked
                        LauncherPreferences.PREF_RECORD_MIC_SOUND = checked
                        LauncherPreferences.prefs.edit { putBoolean("record_mic_sound", checked) }
                    }
                )
            }

            SettingsCard(position = CardPosition.BOTTOM, useSurface = true) {
                SettingsSwitchItem(
                    title = translatedText("Always Save to Gallery"),
                    summary = translatedText("Automatically save new game recordings to your Gallery"),
                    icon = Icons.Default.Download,
                    checked = autoSaveGallery,
                    onCheckedChange = { checked ->
                        autoSaveGallery = checked
                        LauncherPreferences.PREF_AUTO_SAVE_TO_GALLERY = checked
                        LauncherPreferences.prefs.edit { putBoolean("auto_save_to_gallery", checked) }
                    }
                )
            }
        }
    }

    if (showResolutionDialog) {
        ResolutionSelectionDialog(
            title = translatedText("Recording Resolution"),
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
