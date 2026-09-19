package com.ashmeet.hyperlauncher.screens.settings

import android.view.KeyEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ControlCamera
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SettingsOverscan
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.ashmeet.hyperlauncher.components.dialog.KeycodePickerDialog
import com.ashmeet.hyperlauncher.screens.settings.layouts.CardPosition
import com.ashmeet.hyperlauncher.screens.settings.layouts.SettingsCard
import com.ashmeet.hyperlauncher.screens.settings.layouts.SettingsScreenWrapper
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.screens.settings.preferences.PreferenceCategory
import com.ashmeet.hyperlauncher.screens.settings.preferences.SettingsActionItem
import com.ashmeet.hyperlauncher.screens.settings.preferences.SettingsSliderItem
import com.ashmeet.hyperlauncher.screens.settings.preferences.SettingsSwitchItem
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.utils.KeycodeUtils

@Composable
fun ControlSettingsScreen(
    onBack: () -> Unit,
    onNavigateToCustomControls: () -> Unit,
    onNavigateToGamepadMapper: () -> Unit,
    onWipeController: () -> Unit,
    isGyroAvailable: Boolean
) {
    val context = LocalContext.current
    var disableGestures by remember { mutableStateOf(LauncherPreferences.PREF_DISABLE_GESTURES) }
    var disableDoubleTap by remember { mutableStateOf(LauncherPreferences.PREF_DISABLE_SWAP_HAND) }
    var longPressTrigger by remember { mutableFloatStateOf(LauncherPreferences.PREF_LONGPRESS_TRIGGER.toFloat()) }
    var buttonOpacity by remember { mutableFloatStateOf(LauncherPreferences.PREF_BUTTON_TRANSPARENCY) }
    var buttonScale by remember { mutableFloatStateOf(LauncherPreferences.PREF_BUTTONSIZE) }
    var buttonAllCaps by remember { mutableStateOf(LauncherPreferences.PREF_BUTTON_ALL_CAPS) }
    var mouseScale by remember { mutableFloatStateOf(LauncherPreferences.PREF_MOUSESCALE * 100f) }
    var mouseSpeed by remember { mutableFloatStateOf(LauncherPreferences.PREF_MOUSESPEED * 100f) }
    var mouseStart by remember { mutableStateOf(LauncherPreferences.PREF_VIRTUAL_MOUSE_START) }
    var enableGyro by remember { mutableStateOf(LauncherPreferences.PREF_ENABLE_GYRO) }
    var gyroSensitivity by remember { mutableFloatStateOf(LauncherPreferences.PREF_GYRO_SENSITIVITY * 100f) }
    var gyroSampleRate by remember { mutableFloatStateOf(LauncherPreferences.PREF_GYRO_SAMPLE_RATE.toFloat()) }
    var gyroSmoothing by remember { mutableStateOf(LauncherPreferences.PREF_GYRO_SMOOTHING) }
    var gyroInvertX by remember { mutableStateOf(LauncherPreferences.PREF_GYRO_INVERT_X) }
    var gyroInvertY by remember { mutableStateOf(LauncherPreferences.PREF_GYRO_INVERT_Y) }
    var deadzoneScale by remember { mutableFloatStateOf(LauncherPreferences.PREF_DEADZONE_SCALE * 100f) }
    var keyboardAutoPanning by remember { mutableStateOf(LauncherPreferences.PREF_KEYBOARD_AUTOPANNING) }
    var volumeKeysControlEnabled by remember { mutableStateOf(LauncherPreferences.PREF_VOLUME_KEYS_CONTROL_ENABLED) }
    var volumeUpKeybind by remember { mutableIntStateOf(LauncherPreferences.PREF_VOLUME_UP_KEYBIND) }
    var volumeDownKeybind by remember { mutableIntStateOf(LauncherPreferences.PREF_VOLUME_DOWN_KEYBIND) }

    val keyNames = remember { KeycodeUtils.generateKeyName() }
    fun getKeyName(keycode: Int): String {
        val index = KeycodeUtils.getIndexByValue(keycode)
        return if (index >= 0 && index < keyNames.size && (KeycodeUtils.getValueByIndex(index) == keycode)) {
            keyNames[index]
        } else {
            KeyEvent.keyCodeToString(keycode).replace("KEYCODE_", "")
        }
    }

    var showKeyPickerFor by remember { mutableStateOf<String?>(null) }

    SettingsScreenWrapper(
        title = translatedText(stringResource(R.string.preference_control_title)),
        onBack = onBack,
        addTopGap = true
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingsCard(position = CardPosition.SINGLE, useSurface = true) {
                SettingsActionItem(
                    title = translatedText(stringResource(R.string.preference_edit_controls_title)),
                    summary = translatedText(stringResource(R.string.preference_edit_controls_summary)),
                    icon = Icons.Default.Edit,
                    onClick = onNavigateToCustomControls
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingsCard(position = CardPosition.TOP, useSurface = true) {
                SettingsSwitchItem(
                    title = translatedText(stringResource(R.string.mcl_disable_gestures)),
                    summary = translatedText(stringResource(R.string.mcl_disable_gestures_subtitle)),
                    icon = Icons.Default.TouchApp,
                    checked = disableGestures,
                    onCheckedChange = {
                        disableGestures = it
                        LauncherPreferences.prefs.edit { putBoolean("disableGestures", it) }
                        LauncherPreferences.loadPreferences(context)
                    }
                )
            }

            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsSwitchItem(
                    title = translatedText(stringResource(R.string.mcl_disable_swap_hand)),
                    summary = translatedText(stringResource(R.string.mcl_disable_swap_hand_subtitle)),
                    icon = Icons.AutoMirrored.Filled.CompareArrows,
                    checked = disableDoubleTap,
                    onCheckedChange = {
                        disableDoubleTap = it
                        LauncherPreferences.prefs.edit { putBoolean("disableDoubleTap", it) }
                        LauncherPreferences.loadPreferences(context)
                    }
                )
            }

            SettingsCard(position = CardPosition.BOTTOM, useSurface = true) {
                SettingsSliderItem(
                    title = translatedText(stringResource(R.string.mcl_setting_title_longpresstrigger)),
                    summary = translatedText(stringResource(R.string.mcl_setting_subtitle_longpresstrigger)),
                    icon = Icons.Default.TouchApp,
                    value = longPressTrigger,
                    valueRange = integerResource(R.integer.gesture_delay_seekbar_min).toFloat()..integerResource(R.integer.gesture_delay_seekbar_max).toFloat(),
                    onValueChange = {
                        longPressTrigger = it
                        LauncherPreferences.prefs.edit { putInt("timeLongPressTrigger", it.toInt()) }
                        LauncherPreferences.loadPreferences(context)
                    },
                    valueSuffix = "ms"
                )
            }
        }

        PreferenceCategory(title = translatedText(stringResource(R.string.preference_category_buttons)))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingsCard(position = CardPosition.TOP, useSurface = true) {
                SettingsSliderItem(
                    title = translatedText(stringResource(R.string.mcl_setting_title_buttonscale)),
                    summary = translatedText(stringResource(R.string.mcl_setting_subtitle_buttonscale)),
                    icon = Icons.Default.SettingsOverscan,
                    value = buttonScale,
                    valueRange = integerResource(R.integer.button_scale_seekbar_min).toFloat()..integerResource(R.integer.button_scale_seekbar_max).toFloat(),
                    onValueChange = {
                        buttonScale = it
                        LauncherPreferences.prefs.edit { putInt("buttonscale", it.toInt()) }
                        LauncherPreferences.loadPreferences(context)
                    },
                    valueSuffix = "%"
                )
            }

            SettingsCard(position = CardPosition.TOP, useSurface = true) {
                SettingsSliderItem(
                    title = translatedText(stringResource(R.string.mcl_setting_title_buttonopacity)),
                    summary = translatedText(stringResource(R.string.mcl_setting_subtitle_buttonopacity)),
                    icon = Icons.Default.SettingsOverscan,
                    value = buttonOpacity,
                    valueRange = integerResource(R.integer.button_opacity_seekbar_min).toFloat()..integerResource(R.integer.button_opacity_seekbar_max).toFloat(),
                    onValueChange = {
                        buttonOpacity = it
                        LauncherPreferences.prefs.edit { putInt("buttonopacity", it.toInt()) }
                        LauncherPreferences.loadPreferences(context)
                    },
                    valueSuffix = "%"
                )
            }

            SettingsCard(position = CardPosition.BOTTOM, useSurface = true) {
                SettingsSwitchItem(
                    title = translatedText(stringResource(R.string.mcl_setting_title_buttonallcaps)),
                    summary = translatedText(stringResource(R.string.mcl_setting_subtitle_buttonallcaps)),
                    icon = Icons.Default.Title,
                    checked = buttonAllCaps,
                    onCheckedChange = {
                        buttonAllCaps = it
                        LauncherPreferences.prefs.edit { putBoolean("buttonAllCaps", it) }
                        LauncherPreferences.loadPreferences(context)
                    }
                )
            }
        }

        PreferenceCategory(title = translatedText(stringResource(R.string.preference_category_virtual_mouse)))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingsCard(position = CardPosition.TOP, useSurface = true) {
                SettingsSliderItem(
                    title = translatedText(stringResource(R.string.mcl_setting_title_mousescale)),
                    summary = translatedText(stringResource(R.string.mcl_setting_subtitle_mousescale)),
                    icon = Icons.Default.Mouse,
                    value = mouseScale,
                    valueRange = integerResource(R.integer.mouse_scale_seekbar_min).toFloat()..integerResource(R.integer.mouse_scale_seekbar_max).toFloat(),
                    onValueChange = {
                        mouseScale = it
                        LauncherPreferences.prefs.edit { putInt("mousescale", it.toInt()) }
                        LauncherPreferences.loadPreferences(context)
                    },
                    valueSuffix = "%"
                )
            }

            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsSliderItem(
                    title = translatedText(stringResource(R.string.mcl_setting_title_mousespeed)),
                    summary = translatedText(stringResource(R.string.mcl_setting_subtitle_mousespeed)),
                    icon = Icons.Default.Speed,
                    value = mouseSpeed,
                    valueRange = integerResource(R.integer.mouse_speed_seekbar_min).toFloat()..integerResource(R.integer.mouse_speed_seekbar_max).toFloat(),
                    onValueChange = {
                        mouseSpeed = it
                        LauncherPreferences.prefs.edit { putInt("mousespeed", it.toInt()) }
                        LauncherPreferences.loadPreferences(context)
                    },
                    valueSuffix = "%"
                )
            }

            SettingsCard(position = CardPosition.BOTTOM, useSurface = true) {
                SettingsSwitchItem(
                    title = translatedText(stringResource(R.string.preference_mouse_start_title)),
                    summary = translatedText(stringResource(R.string.preference_mouse_start_description)),
                    icon = Icons.Default.Mouse,
                    checked = mouseStart,
                    onCheckedChange = {
                        mouseStart = it
                        LauncherPreferences.prefs.edit { putBoolean("mouse_start", it) }
                        LauncherPreferences.loadPreferences(context)
                    }
                )
            }
        }

        if (isGyroAvailable) {
            PreferenceCategory(title = translatedText(stringResource(R.string.preference_category_gyro_controls)))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SettingsCard(position = CardPosition.TOP, useSurface = true) {
                    SettingsSwitchItem(
                        title = translatedText(stringResource(R.string.preference_enable_gyro_title)),
                        summary = translatedText(stringResource(R.string.preference_enable_gyro_description)),
                        icon = Icons.Default.ScreenRotation,
                        checked = enableGyro,
                        onCheckedChange = {
                            enableGyro = it
                            LauncherPreferences.prefs.edit { putBoolean("enableGyro", it) }
                            LauncherPreferences.loadPreferences(context)
                        }
                    )
                }

                SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                    SettingsSliderItem(
                        title = translatedText(stringResource(R.string.preference_gyro_sensitivity_title)),
                        summary = translatedText(stringResource(R.string.preference_gyro_sensitivity_description)),
                        enabled = enableGyro,
                        icon = Icons.Default.Speed,
                        value = gyroSensitivity,
                        valueRange = integerResource(R.integer.gyro_speed_seekbar_min).toFloat()..integerResource(R.integer.gyro_speed_seekbar_max).toFloat(),
                        onValueChange = {
                            gyroSensitivity = it
                            LauncherPreferences.prefs.edit { putInt("gyroSensitivity", it.toInt()) }
                            LauncherPreferences.loadPreferences(context)
                        },
                        valueSuffix = "%"
                    )
                }

                SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                    SettingsSliderItem(
                        title = translatedText(stringResource(R.string.preference_gyro_sample_rate_title)),
                        summary = translatedText(stringResource(R.string.preference_gyro_sample_rate_description)),
                        enabled = enableGyro,
                        icon = Icons.Default.Tune,
                        value = gyroSampleRate,
                        valueRange = integerResource(R.integer.gyro_rate_seekbar_min).toFloat()..integerResource(R.integer.gyro_rate_seekbar_max).toFloat(),
                        onValueChange = {
                            gyroSampleRate = it
                            LauncherPreferences.prefs.edit { putInt("gyroSampleRate", it.toInt()) }
                            LauncherPreferences.loadPreferences(context)
                        },
                        valueSuffix = "Hz"
                    )
                }

                SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                    SettingsSwitchItem(
                        title = translatedText(stringResource(R.string.preference_gyro_smoothing_title)),
                        summary = translatedText(stringResource(R.string.preference_gyro_smoothing_description)),
                        enabled = enableGyro,
                        icon = Icons.Default.Tune,
                        checked = gyroSmoothing,
                        onCheckedChange = {
                            gyroSmoothing = it
                            LauncherPreferences.prefs.edit { putBoolean("gyroSmoothing", it) }
                            LauncherPreferences.loadPreferences(context)
                        }
                    )
                }

                SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                    SettingsSwitchItem(
                        title = translatedText(stringResource(R.string.preference_gyro_invert_x_axis)),
                        summary = translatedText(stringResource(R.string.preference_gyro_invert_x_axis_description)),
                        enabled = enableGyro,
                        icon = Icons.AutoMirrored.Filled.CompareArrows,
                        checked = gyroInvertX,
                        onCheckedChange = {
                            gyroInvertX = it
                            LauncherPreferences.prefs.edit { putBoolean("gyroInvertX", it) }
                            LauncherPreferences.loadPreferences(context)
                        }
                    )
                }

                SettingsCard(position = CardPosition.BOTTOM, useSurface = true) {
                    SettingsSwitchItem(
                        title = translatedText(stringResource(R.string.preference_gyro_invert_y_axis)),
                        summary = translatedText(stringResource(R.string.preference_gyro_invert_y_axis_description)),
                        enabled = enableGyro,
                        icon = Icons.AutoMirrored.Filled.CompareArrows,
                        checked = gyroInvertY,
                        onCheckedChange = {
                            gyroInvertY = it
                            LauncherPreferences.prefs.edit { putBoolean("gyroInvertY", it) }
                            LauncherPreferences.loadPreferences(context)
                        }
                    )
                }
            }
        }

        PreferenceCategory(title = translatedText(stringResource(R.string.preference_category_controller_settings)))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingsCard(position = CardPosition.TOP, useSurface = true) {
                SettingsActionItem(
                    title = translatedText(stringResource(R.string.preference_remap_controller_title)),
                    summary = translatedText(stringResource(R.string.preference_remap_controller_description)),
                    icon = Icons.Default.VideogameAsset,
                    onClick = onNavigateToGamepadMapper
                )
            }

            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsActionItem(
                    title = translatedText(stringResource(R.string.preference_wipe_controller_title)),
                    summary = translatedText(stringResource(R.string.preference_wipe_controller_description)),
                    icon = Icons.Default.Build,
                    onClick = onWipeController
                )
            }

            SettingsCard(position = CardPosition.BOTTOM, useSurface = true) {
                SettingsSliderItem(
                    title = translatedText(stringResource(R.string.preference_deadzone_scale_title)),
                    summary = translatedText(stringResource(R.string.preference_deadzone_scale_description)),
                    icon = Icons.Default.ControlCamera,
                    value = deadzoneScale,
                    valueRange = integerResource(R.integer.gamepad_deadzone_seekbar_min).toFloat()..integerResource(R.integer.gamepad_deadzone_seekbar_max).toFloat(),
                    onValueChange = {
                        deadzoneScale = it
                        LauncherPreferences.prefs.edit { putInt("gamepad_deadzone_scale", it.toInt()) }
                        LauncherPreferences.loadPreferences(context)
                    },
                    valueSuffix = "%"
                )
            }
        }

        PreferenceCategory(title = translatedText(stringResource(R.string.preference_category_keyboard)))

        SettingsCard(position = CardPosition.SINGLE, useSurface = true) {
            SettingsSwitchItem(
                title = translatedText(stringResource(R.string.preference_keyboard_autopan_title)),
                summary = translatedText(stringResource(R.string.preference_keyboard_autopan_summary)),
                icon = Icons.Default.Keyboard,
                checked = keyboardAutoPanning,
                onCheckedChange = {
                    keyboardAutoPanning = it
                    LauncherPreferences.prefs.edit { putBoolean("keyboardAutoPanning", it) }
                    LauncherPreferences.loadPreferences(context)
                }
            )
        }

        PreferenceCategory(title = translatedText("Volume Keys"))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingsCard(position = CardPosition.TOP, useSurface = true) {
                SettingsSwitchItem(
                    title = translatedText("Enable Volume Key Controls"),
                    summary = translatedText("Use volume buttons to trigger game key events"),
                    icon = Icons.AutoMirrored.Rounded.VolumeUp,
                    checked = volumeKeysControlEnabled,
                    onCheckedChange = {
                        volumeKeysControlEnabled = it
                        LauncherPreferences.prefs.edit { putBoolean("volume_keys_control_enabled", it) }
                        LauncherPreferences.loadPreferences(context)
                    }
                )
            }

            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsActionItem(
                    title = translatedText("Volume Up Keybind"),
                    summary = translatedText("Current Key: ${getKeyName(volumeUpKeybind)} ($volumeUpKeybind)"),
                    icon = Icons.AutoMirrored.Rounded.VolumeUp,
                    enabled = volumeKeysControlEnabled,
                    onClick = { showKeyPickerFor = "up" }
                )
            }

            SettingsCard(position = CardPosition.BOTTOM, useSurface = true) {
                SettingsActionItem(
                    title = translatedText("Volume Down Keybind"),
                    summary = translatedText("Current Key: ${getKeyName(volumeDownKeybind)} ($volumeDownKeybind)"),
                    icon = Icons.AutoMirrored.Rounded.VolumeUp,
                    enabled = volumeKeysControlEnabled,
                    onClick = { showKeyPickerFor = "down" }
                )
            }
        }
    }

    if (showKeyPickerFor != null) {
        KeycodePickerDialog(
            title = if (showKeyPickerFor == "up") translatedText("Volume Up Keybind") else translatedText("Volume Down Keybind"),
            initialValue = if (showKeyPickerFor == "up") volumeUpKeybind else volumeDownKeybind,
            onKeycodePicked = { keyCode ->
                if (showKeyPickerFor == "up") {
                    volumeUpKeybind = keyCode
                    LauncherPreferences.prefs.edit { putInt("volume_up_keybind", keyCode) }
                } else {
                    volumeDownKeybind = keyCode
                    LauncherPreferences.prefs.edit { putInt("volume_down_keybind", keyCode) }
                }
                LauncherPreferences.loadPreferences(context)
            },
            onDismiss = { showKeyPickerFor = null }
        )
    }
}
