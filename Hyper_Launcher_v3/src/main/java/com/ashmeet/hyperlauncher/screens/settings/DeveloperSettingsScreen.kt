package com.ashmeet.hyperlauncher.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.core.content.edit
import coil.compose.rememberAsyncImagePainter
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.screens.settings.layouts.CardPosition
import com.ashmeet.hyperlauncher.screens.settings.layouts.SettingsCard
import com.ashmeet.hyperlauncher.screens.settings.layouts.SettingsScreenWrapper
import com.ashmeet.hyperlauncher.screens.settings.preferences.PreferenceCategory
import com.ashmeet.hyperlauncher.screens.settings.preferences.SettingsActionItem
import com.ashmeet.hyperlauncher.screens.settings.preferences.SettingsSwitchItem
import com.ashmeet.hyperlauncher.utils.translation.Translator
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperlauncher.plugins.natives.LibraryPlugin

@Composable
fun DeveloperSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val fclPlugins = remember { LibraryPlugin.discoverAllPlugins(context) }
    var forceEnglish by remember { mutableStateOf(LauncherPreferences.PREF_FORCE_ENGLISH) }

    SettingsScreenWrapper(
        title = translatedText("Developer options"),
        onBack = onBack,
        addTopGap = true
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingsCard(
                position = CardPosition.SINGLE,
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = translatedText("Warning: These settings are for developers only. Changing them may break the application or cause data loss."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            PreferenceCategory(title = translatedText("Development"))

            SettingsCard(position = CardPosition.SINGLE, useSurface = true) {
                SettingsSwitchItem(
                    title = translatedText(stringResource(R.string.preference_force_english_title)),
                    summary = translatedText(stringResource(R.string.preference_force_english_description)),
                    icon = Icons.Default.Translate,
                    checked = forceEnglish,
                    onCheckedChange = {
                        forceEnglish = it
                        LauncherPreferences.prefs.edit { putBoolean("force_english", it) }
                        LauncherPreferences.loadPreferences(context)
                        if (!it) {
                            Translator.prefetchTranslations(context)
                        }
                    }
                )
            }

            PreferenceCategory(title = translatedText("Plugins"))

            if (fclPlugins.isEmpty()) {
                SettingsCard(position = CardPosition.SINGLE, useSurface = true) {
                    SettingsActionItem(
                        title = translatedText("No plugins detected"),
                        summary = translatedText("Additional features are integrated directly into the application."),
                        icon = Icons.Default.Info,
                        onClick = {}
                    )
                }
            } else {
                fclPlugins.forEachIndexed { index, plugin ->
                    val position = when {
                        fclPlugins.size == 1 -> CardPosition.SINGLE
                        index == 0 -> CardPosition.TOP
                        index == fclPlugins.size - 1 -> CardPosition.BOTTOM
                        else -> CardPosition.MIDDLE
                    }

                    SettingsCard(position = position, useSurface = true) {
                        val description = plugin.getMetaData().getString(LibraryPlugin.METADATA_FCL_DESCRIPTION)
                        val pluginIcon = remember(plugin.appId) {
                            try {
                                context.packageManager.getApplicationIcon(plugin.appId)
                            } catch (_: Exception) {
                                null
                            }
                        }

                        SettingsActionItem(
                            title = plugin.appId,
                            summary = translatedText(description ?: "No description provided."),
                            icon = if (pluginIcon == null) Icons.Default.Extension else null,
                            iconPainter = if (pluginIcon != null) rememberAsyncImagePainter(pluginIcon) else null,
                            tintIcon = pluginIcon == null,
                            onClick = {}
                        )
                    }
                }
            }
        }
    }
}
