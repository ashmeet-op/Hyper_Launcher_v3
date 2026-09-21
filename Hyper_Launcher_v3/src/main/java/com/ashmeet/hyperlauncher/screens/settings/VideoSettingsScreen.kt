package com.ashmeet.hyperlauncher.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
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
import com.ashmeet.hyperlauncher.activity.BaseActivity
import com.ashmeet.hyperlauncher.screens.settings.layouts.CardPosition
import com.ashmeet.hyperlauncher.screens.settings.layouts.SettingsCard
import com.ashmeet.hyperlauncher.screens.settings.layouts.SettingsScreenWrapper
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.screens.settings.preferences.SettingsActionItem
import com.ashmeet.hyperlauncher.screens.settings.preferences.SettingsSliderItem
import com.ashmeet.hyperlauncher.screens.settings.preferences.SettingsSwitchItem
import com.ashmeet.hyperlauncher.screens.settings.preferences.SingleChoiceDialog
import androidx.compose.material.icons.filled.Settings
import com.ashmeet.hyperlauncher.utils.RendererCompatUtil
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import net.ashmeet.hyperlauncher.R

@Composable
fun VideoSettingsScreen(
    onBack: () -> Unit,
    isAngleAvailable: Boolean,
    isZinkPreferSystemDriverVisible: Boolean
) {
    val context = LocalContext.current
    var renderer by remember { mutableStateOf(LauncherPreferences.PREF_RENDERER) }
    var graphicsBackend by remember { mutableStateOf(LauncherPreferences.PREF_GRAPHICS_BACKEND) }
    var ignoreNotch by remember { mutableStateOf(LauncherPreferences.PREF_IGNORE_NOTCH) }
    var fullscreenLauncher by remember { mutableStateOf(LauncherPreferences.PREF_FULLSCREEN_LAUNCHER) }
    var resolutionRatio by remember { mutableFloatStateOf(LauncherPreferences.PREF_SCALE_FACTOR * 100f) }
    var sustainedPerformance by remember { mutableStateOf(LauncherPreferences.PREF_SUSTAINED_PERFORMANCE) }
    var alternateSurface by remember { mutableStateOf(LauncherPreferences.PREF_USE_ALTERNATE_SURFACE) }
    var forceVsync by remember { mutableStateOf(LauncherPreferences.PREF_FORCE_VSYNC) }
    var useAngle by remember { mutableStateOf(LauncherPreferences.PREF_USE_ANGLE) }
    var vsyncInZink by remember { mutableStateOf(LauncherPreferences.PREF_VSYNC_IN_ZINK) }
    var zinkPreferSystemDriver by remember { mutableStateOf(LauncherPreferences.PREF_ZINK_PREFER_SYSTEM_DRIVER) }
    var zinkForceLegacy by remember { mutableStateOf(LauncherPreferences.PREF_ZINK_FORCE_LEGACY) }
    var dynamicOrientation by remember { mutableStateOf(LauncherPreferences.PREF_DYNAMIC_ORIENTATION) }
    var showRendererDialog by remember { mutableStateOf(false) }
    var showBackendDialog by remember { mutableStateOf(false) }

    SettingsScreenWrapper(
        title = translatedText(stringResource(R.string.preference_category_video)),
        onBack = onBack,
        addTopGap = true
    ) {
        val compatibleRenderers = remember(context) { RendererCompatUtil.getCompatibleRenderers(context) }
        val rendererDisplayName = remember(renderer, compatibleRenderers) {
            val index = compatibleRenderers.rendererIds.indexOf(renderer)
            if (index != -1) compatibleRenderers.rendererDisplayNames[index] else renderer
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingsCard(position = CardPosition.TOP, useSurface = true) {
                SettingsActionItem(
                    title = translatedText(stringResource(R.string.mcl_setting_category_renderer)),
                    summary = rendererDisplayName,
                    icon = Icons.Default.Brush,
                    onClick = { showRendererDialog = true }
                )
            }

            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                val backendOptions = listOf("Default", "Vulkan", "OpenGL", "Let MC Decide")
                val backendValues = listOf("default", "vulkan", "opengl", "minecraft")
                val currentBackendLabel = backendOptions[backendValues.indexOf(graphicsBackend).coerceAtLeast(0)]

                SettingsActionItem(
                    title = translatedText("Preferred Graphics Backend"),
                    summary = currentBackendLabel,
                    icon = Icons.Default.Architecture,
                    warningTooltip = if (graphicsBackend == "vulkan") "Your device must support Vulkan to use this option." else null,
                    onClick = { showBackendDialog = true }
                )
            }

            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsSwitchItem(
                    title = translatedText(stringResource(R.string.mcl_setting_title_ignore_notch)),
                    summary = translatedText(stringResource(R.string.mcl_setting_subtitle_ignore_notch)),
                    icon = Icons.Default.AspectRatio,
                    checked = ignoreNotch,
                    onCheckedChange = {
                        ignoreNotch = it
                        LauncherPreferences.prefs.edit { putBoolean("ignoreNotch", it) }
                        LauncherPreferences.loadPreferences(context)
                    }
                )
            }

            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsSwitchItem(
                    title = translatedText(stringResource(R.string.mcl_setting_title_fullscreen_ui)),
                    summary = translatedText(stringResource(R.string.mcl_setting_subtitle_fullscreen_ui)),
                    icon = Icons.Default.Fullscreen,
                    checked = fullscreenLauncher,
                    warningTooltip = "Disabling edge-to-edge display may cause layout issues on some devices",
                    onCheckedChange = {
                        fullscreenLauncher = it
                        LauncherPreferences.prefs.edit { putBoolean("fullscreen_launcher", it) }
                        LauncherPreferences.loadPreferences(context)
                    }
                )
            }

            SettingsCard(position = CardPosition.BOTTOM, useSurface = true) {
                SettingsSliderItem(
                    title = translatedText(stringResource(R.string.mcl_setting_title_resolution_scaler)),
                    summary = translatedText(stringResource(R.string.mcl_setting_subtitle_resolution_scaler)),
                    icon = Icons.Default.Monitor,
                    value = resolutionRatio,
                    valueRange = integerResource(R.integer.resolution_seekbar_min).toFloat()..100f,
                    onValueChange = {
                        resolutionRatio = it
                        LauncherPreferences.prefs.edit { putInt("resolutionRatio", it.toInt()) }
                        LauncherPreferences.loadPreferences(context)
                    },
                    valueSuffix = "%"
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingsCard(position = CardPosition.TOP, useSurface = true) {
                SettingsSwitchItem(
                    title = translatedText("Dynamic Orientation"),
                    summary = translatedText("Allow the game to change screen orientation dynamically. If disabled, horizontal mode is forced."),
                    icon = Icons.Default.AspectRatio,
                    checked = dynamicOrientation,
                    onCheckedChange = {
                        dynamicOrientation = it
                        LauncherPreferences.prefs.edit { putBoolean("dynamic_orientation", it) }
                        LauncherPreferences.loadPreferences(context)
                        (context as? BaseActivity)?.updateOrientation()
                        git.mojo.sdl.SDLActivity.setDynamicOrientationEnabled(it)
                    }
                )
            }

            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsSwitchItem(
                    title = translatedText(stringResource(R.string.mcl_setting_title_use_surface_view)),
                    summary = translatedText(stringResource(R.string.mcl_setting_subtitle_use_surface_view)),
                    icon = Icons.Default.Layers,
                    checked = alternateSurface,
                    warningTooltip = "Surface View rendering might be unstable on some devices and could lead to graphical glitches.",
                    onCheckedChange = {
                        alternateSurface = it
                        LauncherPreferences.prefs.edit { putBoolean("alternate_surface", it) }
                        LauncherPreferences.loadPreferences(context)
                    }
                )
            }

            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsSwitchItem(
                    title = translatedText(stringResource(R.string.preference_force_vsync_title)),
                    summary = translatedText(stringResource(R.string.preference_force_vsync_description)),
                    icon = Icons.Default.Sync,
                    checked = forceVsync,
                    onCheckedChange = {
                        forceVsync = it
                        LauncherPreferences.prefs.edit { putBoolean("force_vsync", it) }
                        LauncherPreferences.loadPreferences(context)
                    }
                )
            }


            SettingsCard(position = CardPosition.BOTTOM, useSurface = true) {
                SettingsSwitchItem(
                title = translatedText(stringResource(R.string.preference_sustained_performance_title)),
                summary = translatedText(stringResource(R.string.preference_sustained_performance_description)),
                icon = Icons.Default.Speed,
                checked = sustainedPerformance,
                onCheckedChange = {
                sustainedPerformance = it
                LauncherPreferences.prefs.edit { putBoolean("sustainedPerformance", it) }
                LauncherPreferences.loadPreferences(context)
                    }
                )
            }

        }

        val isZinkUsed = renderer.contains("zink")
        if (isZinkUsed || isAngleAvailable) {

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isZinkUsed) {
                    SettingsCard(position = CardPosition.TOP, useSurface = true) {
                        SettingsSwitchItem(
                            title = translatedText(stringResource(R.string.preference_vsync_in_zink_title)),
                            summary = translatedText(stringResource(R.string.preference_vsync_in_zink_description)),
                            icon = Icons.Default.Sync,
                            checked = vsyncInZink,
                            onCheckedChange = {
                                vsyncInZink = it
                                LauncherPreferences.prefs.edit { putBoolean("vsync_in_zink", it) }
                                LauncherPreferences.loadPreferences(context)
                            }
                        )
                    }

                    if (isZinkPreferSystemDriverVisible) {
                        SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                            SettingsSwitchItem(
                                title = translatedText(stringResource(R.string.preference_vulkan_driver_system_title)),
                                summary = translatedText(stringResource(R.string.preference_vulkan_driver_system_description)),
                                icon = Icons.Default.Architecture,
                                checked = zinkPreferSystemDriver,
                                onCheckedChange = {
                                    zinkPreferSystemDriver = it
                                    LauncherPreferences.prefs.edit { putBoolean("zinkPreferSystemDriver", it) }
                                    LauncherPreferences.loadPreferences(context)
                                }
                            )
                        }
                    }
                }

                if (isAngleAvailable) {
                    val anglePos = if (isZinkUsed) CardPosition.MIDDLE else CardPosition.TOP
                    SettingsCard(position = anglePos, useSurface = true) {
                        SettingsSwitchItem(
                            title = translatedText(stringResource(R.string.preference_use_angle_title)),
                            summary = translatedText(stringResource(R.string.preference_use_angle_description)),
                            icon = Icons.Default.Architecture,
                            checked = useAngle,
                            onCheckedChange = {
                                useAngle = it
                                LauncherPreferences.prefs.edit { putBoolean("use_angle", it) }
                                LauncherPreferences.loadPreferences(context)
                            }
                        )
                    }
                }

                if (isZinkUsed) {
                    SettingsCard(position = CardPosition.BOTTOM, useSurface = true) {
                        SettingsSwitchItem(
                            title = translatedText(stringResource(R.string.preference_force_legacy_zink_title)),
                            summary = translatedText(stringResource(R.string.preference_force_legacy_zink_description)),
                            icon = Icons.Default.History,
                            checked = zinkForceLegacy,
                            onCheckedChange = {
                                zinkForceLegacy = it
                                LauncherPreferences.prefs.edit { putBoolean("zinkForceLegacy", it) }
                                LauncherPreferences.loadPreferences(context)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showRendererDialog) {
        val compatibleRenderers = RendererCompatUtil.getCompatibleRenderers(context)
        SingleChoiceDialog(
            title = translatedText(stringResource(R.string.mcl_setting_category_renderer)),
            options = compatibleRenderers.rendererDisplayNames.toList(),
            optionValues = compatibleRenderers.rendererIds,
            selectedValue = renderer,
            onValueChange = { newValue ->
                renderer = newValue
                LauncherPreferences.prefs.edit { putString("renderer", newValue) }
                LauncherPreferences.loadPreferences(context)
            },
            onDismiss = { showRendererDialog = false }
        )
    }

    if (showBackendDialog) {
        SingleChoiceDialog(
            title = translatedText("Preferred Graphics Backend"),
            options = listOf("Default", "Vulkan", "OpenGL", "Let MC Decide"),
            optionValues = listOf("default", "vulkan", "opengl", "minecraft"),
            selectedValue = graphicsBackend,
            onValueChange = { newValue ->
                graphicsBackend = newValue
                LauncherPreferences.prefs.edit { putString("preferredGraphicsBackend", newValue) }
                LauncherPreferences.loadPreferences(context)
            },
            onDismiss = { showBackendDialog = false }
        )
    }
}
