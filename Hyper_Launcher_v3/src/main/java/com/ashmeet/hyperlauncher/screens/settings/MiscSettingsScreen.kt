package com.ashmeet.hyperlauncher.screens.settings

import com.ashmeet.hyperlauncher.utils.translation.translatedText

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Share
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
import com.ashmeet.hyperlauncher.screens.settings.preferences.TextInputDialog
import com.ashmeet.hyperlauncher.utils.DiscordRpcManager
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.utils.Tools
import com.ashmeet.hyperbackend.game.renderer.RendererCache
import java.io.File

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
    var disableBlocklist by remember { mutableStateOf(LauncherPreferences.PREF_DISABLE_BLOCKLIST) }
    var enableRpc by remember { mutableStateOf(LauncherPreferences.PREF_ENABLE_RPC) }
    var discordToken by remember { mutableStateOf(LauncherPreferences.PREF_DISCORD_TOKEN ?: "") }

    var showDownloadSourceDialog by remember { mutableStateOf(false) }
    var showDiscordTokenDialog by remember { mutableStateOf(false) }

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

    if (showDiscordTokenDialog) {
        TextInputDialog(
            title = translatedText("Discord Token"),
            initialValue = discordToken,
            onConfirm = { newValue ->
                discordToken = newValue.trim()
                LauncherPreferences.prefs.edit { putString("discord_token", discordToken) }
                LauncherPreferences.loadPreferences(context)
                showDiscordTokenDialog = false
                if (enableRpc) {
                    DiscordRpcManager.startRpc()
                }
            },
            onDismiss = { showDiscordTokenDialog = false }
        )
    }

    val clearCacheSuccessMessage = stringResource(R.string.preference_clear_cache_success)

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

            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
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

            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsSwitchItem(
                    title = translatedText(stringResource(R.string.pref_blocklist_disable_title)),
                    summary = translatedText(stringResource(R.string.pref_blocklist_disable_description)),
                    icon = Icons.Default.Block,
                    checked = disableBlocklist,
                    onCheckedChange = {
                        disableBlocklist = it
                        LauncherPreferences.prefs.edit { putBoolean("disableServerBlocklist", it) }
                        LauncherPreferences.loadPreferences(context)
                    }
                )
            }

            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsSwitchItem(
                    title = translatedText("Discord Rich Presence"),
                    summary = translatedText("Show your Minecraft activity on Discord using KizzyRPC"),
                    icon = Icons.Default.Share,
                    checked = enableRpc,
                    onCheckedChange = {
                        enableRpc = it
                        LauncherPreferences.prefs.edit { putBoolean("enable_discord_rpc", it) }
                        LauncherPreferences.loadPreferences(context)
                        if (it) {
                            DiscordRpcManager.startRpc()
                        } else {
                            DiscordRpcManager.stopRpc()
                        }
                    }
                )
            }

            SettingsCard(position = CardPosition.BOTTOM, useSurface = true) {
                SettingsActionItem(
                    title = translatedText("Discord Token"),
                    summary = if (discordToken.isNotBlank()) {
                        if (discordToken.length > 8) "••••••••" + discordToken.takeLast(4) else "••••••••"
                    } else {
                        translatedText("Tap to set Discord account token for Rich Presence")
                    },
                    icon = Icons.Default.Key,
                    onClick = { showDiscordTokenDialog = true }
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

            visibleItems.add {
                SettingsActionItem(
                    title = translatedText(stringResource(R.string.preference_clear_cache_title)),
                    summary = translatedText(stringResource(R.string.preference_clear_cache_description)),
                    icon = Icons.Default.CleaningServices,
                    onClick = {
                        try {
                            context.cacheDir?.deleteRecursively()
                            context.externalCacheDir?.deleteRecursively()
                            context.cacheDir?.mkdirs()
                            Tools.DIR_CACHE_APP_ICON = File(context.cacheDir, "app_icons").apply { mkdirs() }
                            RendererCache.releaseRendererCache()
                            Toast.makeText(
                                context,
                                clearCacheSuccessMessage,
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: Exception) {
                            Log.e("MiscSettings", "Failed to clear launcher cache", e)
                        }
                    }
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
