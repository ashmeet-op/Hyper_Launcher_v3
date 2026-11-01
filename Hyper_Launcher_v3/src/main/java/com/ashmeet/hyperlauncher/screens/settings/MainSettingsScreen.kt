package com.ashmeet.hyperlauncher.screens.settings

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SettingsApplications
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ashmeet.hyperlauncher.screens.settings.layouts.CardPosition
import com.ashmeet.hyperlauncher.screens.settings.layouts.SettingsCard
import com.ashmeet.hyperlauncher.screens.settings.layouts.SettingsScreenWrapper
import com.ashmeet.hyperlauncher.screens.settings.preferences.PreferenceCategory
import com.ashmeet.hyperlauncher.screens.settings.preferences.SettingsActionItem
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences

@Composable
fun MainSettingsScreen(
    onNavigateToVideo: () -> Unit,
    onNavigateToControl: () -> Unit,
    onNavigateToJava: () -> Unit,
    onNavigateToMisc: () -> Unit,
    onNavigateToExperimental: () -> Unit,
    onNavigateToDeveloper: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToRecorder: () -> Unit,
    onNavigateToAbout: () -> Unit,
    showNotificationRequest: Boolean,
    onNotificationRequestClick: () -> Unit
) {
    var developerOptionsEnabled by remember { mutableStateOf(LauncherPreferences.PREF_DEVELOPER_OPTIONS) }


    LaunchedEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "developer_options") {
                developerOptionsEnabled = LauncherPreferences.prefs.getBoolean("developer_options", false)
            }
        }
        LauncherPreferences.prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    SettingsScreenWrapper(
        title = translatedText(stringResource(R.string.mcl_options)),
        onBack = null
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingsCard(position = CardPosition.TOP, useSurface = true) {
                SettingsActionItem(
                    title = translatedText(stringResource(R.string.preference_video_title)),
                    summary = translatedText(stringResource(R.string.preference_video_description)),
                    icon = Icons.Default.Image,
                    onClick = onNavigateToVideo
                )
            }

            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsActionItem(
                    title = translatedText(stringResource(R.string.preference_control_title)),
                    summary = translatedText(stringResource(R.string.preference_control_description)),
                    icon = Icons.Default.VideogameAsset,
                    onClick = onNavigateToControl
                )
            }

            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsActionItem(
                    title = translatedText(stringResource(R.string.preference_java_title)),
                    summary = translatedText(stringResource(R.string.preference_java_description)),
                    icon = Icons.Default.Coffee,
                    onClick = onNavigateToJava
                )
            }

            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsActionItem(
                    title = translatedText(stringResource(R.string.preference_misc_title)),
                    summary = translatedText(stringResource(R.string.preference_misc_description)),
                    icon = Icons.Default.SettingsApplications,
                    onClick = onNavigateToMisc
                )
            }

            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsActionItem(
                    title = translatedText(stringResource(R.string.preference_experimental_title)),
                    summary = translatedText(stringResource(R.string.preference_experimental_description)),
                    icon = Icons.Default.Science,
                    warningTooltip = "Warning: These settings are experimental and may cause instability or crashes.",
                    onClick = onNavigateToExperimental
                )
            }

            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsActionItem(
                    title = translatedText(stringResource(R.string.preference_appearance_title)),
                    summary = translatedText(stringResource(R.string.preference_appearance_description)),
                    icon = Icons.Default.Palette,
                    onClick = onNavigateToAppearance
                )
            }

            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsActionItem(
                    title = translatedText("Recorder Settings"),
                    summary = translatedText("Resolution, microphone audio, and gallery preferences"),
                    icon = Icons.Rounded.Videocam,
                    onClick = onNavigateToRecorder
                )
            }

            SettingsCard(position = if (LauncherPreferences.PREF_DEVELOPER_OPTIONS) CardPosition.MIDDLE else CardPosition.BOTTOM, useSurface = true) {
                SettingsActionItem(
                    title = translatedText("About"),
                    summary = translatedText("Version, device and community information"),
                    icon = Icons.Rounded.Info,
                    onClick = onNavigateToAbout
                )
            }

            if (LauncherPreferences.PREF_DEVELOPER_OPTIONS) {
                SettingsCard(position = CardPosition.BOTTOM, useSurface = true) {
                    SettingsActionItem(
                        title = translatedText("Developer options"),
                        summary = translatedText("Advanced settings for developers"),
                        icon = Icons.Default.Build,
                        onClick = onNavigateToDeveloper
                    )
                }
            }
        }

        if (showNotificationRequest) {
            PreferenceCategory(title = translatedText(stringResource(R.string.preference_category_miscellaneous)))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SettingsCard(position = CardPosition.SINGLE, useSurface = true) {
                    SettingsActionItem(
                        title = translatedText(stringResource(R.string.preference_ask_for_notification_title)),
                        summary = translatedText(stringResource(R.string.preference_ask_for_notification_description)),
                        icon = Icons.Default.Notifications,
                        onClick = onNotificationRequestClick
                    )
                }
            }
        }
    }
}
