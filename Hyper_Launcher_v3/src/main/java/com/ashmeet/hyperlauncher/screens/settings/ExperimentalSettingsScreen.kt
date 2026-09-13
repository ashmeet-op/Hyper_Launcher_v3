package com.ashmeet.hyperlauncher.screens.settings

import com.ashmeet.hyperlauncher.utils.translation.translatedText

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.core.content.edit
import androidx.compose.ui.unit.dp
import com.ashmeet.hyperlauncher.screens.settings.layouts.CardPosition
import com.ashmeet.hyperlauncher.screens.settings.layouts.SettingsCard
import com.ashmeet.hyperlauncher.screens.settings.layouts.SettingsScreenWrapper
import com.ashmeet.hyperlauncher.screens.settings.preferences.SettingsSwitchItem
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences

@Composable
fun ExperimentalSettingsScreen(
    onBack: () -> Unit,
    isFreedrenoAvailable: Boolean
) {
    val context = LocalContext.current
    var dumpShaders by remember { mutableStateOf(LauncherPreferences.PREF_DUMP_SHADERS) }
    var bigCoreAffinity by remember { mutableStateOf(LauncherPreferences.PREF_BIG_CORE_AFFINITY) }
    var freedrenoSysmem by remember { mutableStateOf(LauncherPreferences.PREF_FREEDRENO_SYSMEM) }
    var alsoftForceOpenSL by remember { mutableStateOf(LauncherPreferences.PREF_ALSOFT_FORCE_OPENSL) }

    SettingsScreenWrapper(
        title = translatedText(stringResource(R.string.preference_experimental_title)),
        onBack = onBack,
        addTopGap = true
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val items = mutableListOf<@Composable () -> Unit>()

            items.add {
                SettingsSwitchItem(
                    title = translatedText(stringResource(R.string.preference_shader_dump_title)),
                    summary = translatedText(stringResource(R.string.preference_shader_dump_description)),
                    icon = Icons.Default.Settings,
                    checked = dumpShaders,
                    onCheckedChange = {
                        dumpShaders = it
                        LauncherPreferences.prefs.edit { putBoolean("dump_shaders", it) }
                        LauncherPreferences.loadPreferences(context)
                    }
                )
            }

            items.add {
                SettingsSwitchItem(
                    title = translatedText(stringResource(R.string.preference_force_big_core_title)),
                    summary = translatedText(stringResource(R.string.preference_force_big_core_desc)),
                    icon = Icons.Default.Settings,
                    checked = bigCoreAffinity,
                    onCheckedChange = {
                        bigCoreAffinity = it
                        LauncherPreferences.prefs.edit { putBoolean("bigCoreAffinity", it) }
                        LauncherPreferences.loadPreferences(context)
                    }
                )
            }

            if (isFreedrenoAvailable) {
                items.add {
                    SettingsSwitchItem(
                        title = translatedText(stringResource(R.string.preference_sysmem_title)),
                        summary = translatedText(stringResource(R.string.preference_sysmem_summary)),
                        icon = Icons.Default.Settings,
                        checked = freedrenoSysmem,
                        onCheckedChange = {
                            freedrenoSysmem = it
                            LauncherPreferences.prefs.edit { putBoolean("freedrenoSysmem", it) }
                            LauncherPreferences.loadPreferences(context)
                        }
                    )
                }
            }

            items.add {
                SettingsSwitchItem(
                    title = translatedText(stringResource(R.string.preference_alsoft_opensl_title)),
                    summary = translatedText(stringResource(R.string.preference_alsoft_opensl_summary)),
                    icon = Icons.Default.Speed,
                    checked = alsoftForceOpenSL,
                    onCheckedChange = {
                        alsoftForceOpenSL = it
                        LauncherPreferences.prefs.edit { putBoolean("alsoftForceOpenSL", it) }
                        LauncherPreferences.loadPreferences(context)
                    }
                )
            }

            items.forEachIndexed { index, content ->
                val position = when {
                    items.size == 1 -> CardPosition.SINGLE
                    index == 0 -> CardPosition.TOP
                    index == items.size - 1 -> CardPosition.BOTTOM
                    else -> CardPosition.MIDDLE
                }
                SettingsCard(position = position, useSurface = true) {
                    content()
                }
            }
        }
    }
}
