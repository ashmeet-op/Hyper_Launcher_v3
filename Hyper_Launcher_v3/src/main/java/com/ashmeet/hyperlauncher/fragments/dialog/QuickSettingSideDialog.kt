package com.ashmeet.hyperlauncher.fragments.dialog

import com.ashmeet.hyperlauncher.utils.translation.translatedText

import android.content.SharedPreferences
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ashmeet.hyperlauncher.components.dialog.CardPosition
import com.ashmeet.hyperlauncher.components.dialog.DialogActionItem
import com.ashmeet.hyperlauncher.components.dialog.DialogCard
import com.ashmeet.hyperlauncher.components.dialog.DialogSliderItem
import com.ashmeet.hyperlauncher.components.dialog.DialogSwitchItem
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperlauncher.utils.Tools
import net.kdt.pojavlaunch.utils.KeycodeUtils


abstract class QuickSettingSideDialog : SideDialogView() {

    private var mEditor: SharedPreferences.Editor? = null

    private var mOriginalGyroEnabled = false
    private var mOriginalGyroXEnabled = false
    private var mOriginalGyroYEnabled = false
    private var mOriginalGestureDisabled = false

    private var mOriginalGyroSensitivity = 0f
    private var mOriginalMouseSpeed = 0f
    private var mOriginalResolution = 0f
    private var mOriginalGestureDelay = 0
    private var mOriginalButtonTransparency = 0f

    private var mOriginalShowFps = false

    private var mOriginalVolumeEnabled = false
    private var mOriginalVolumeUp = 24
    private var mOriginalVolumeDown = 25

    private var selectedTab by mutableIntStateOf(0)

    init {
        setTitle(R.string.quick_setting_title)
        dialogWidth = 280.dp
    }

    override fun onDisappear() {
        mEditor?.apply()
    }

    override fun onAppear() {
        mEditor = LauncherPreferences.prefs.edit()

        mOriginalGyroEnabled = LauncherPreferences.PREF_ENABLE_GYRO
        mOriginalGyroXEnabled = LauncherPreferences.PREF_GYRO_INVERT_X
        mOriginalGyroYEnabled = LauncherPreferences.PREF_GYRO_INVERT_Y
        mOriginalGestureDisabled = LauncherPreferences.PREF_DISABLE_GESTURES

        mOriginalGyroSensitivity = LauncherPreferences.PREF_GYRO_SENSITIVITY
        mOriginalMouseSpeed = LauncherPreferences.PREF_MOUSESPEED
        mOriginalGestureDelay = LauncherPreferences.PREF_LONGPRESS_TRIGGER
        mOriginalResolution = LauncherPreferences.PREF_SCALE_FACTOR
        mOriginalButtonTransparency = LauncherPreferences.PREF_BUTTON_TRANSPARENCY
        mOriginalShowFps = LauncherPreferences.PREF_SHOW_FPS

        mOriginalVolumeEnabled = LauncherPreferences.PREF_VOLUME_KEYS_CONTROL_ENABLED
        mOriginalVolumeUp = LauncherPreferences.PREF_VOLUME_UP_KEYBIND
        mOriginalVolumeDown = LauncherPreferences.PREF_VOLUME_DOWN_KEYBIND
    }

    @Composable
    override fun DialogHeader() {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            divider = {},
            modifier = Modifier.fillMaxWidth(),
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        height = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                icon = { Icon(Icons.Rounded.GraphicEq, null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                icon = { Icon(Icons.Rounded.Mouse, null) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                icon = { Icon(Icons.Rounded.ScreenRotation, null) }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                icon = { Icon(Icons.Rounded.Tune, null) }
            )
        }
    }

    @Composable
    override fun DialogContent() {
        Surface(
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            QuickSettingContent(
                selectedTab = selectedTab,
                onResolutionChanged = { onResolutionChanged() },
                onGyroStateChanged = { onGyroStateChanged() },
                onButtonTransparencyChanged = { onButtonTransparencyChanged() },
                onForceClose = { onForceClose() },
                onViewOutput = { onViewOutput() },
                onCustomKey = { onCustomKey() },
                onShowFpsChanged = { onShowFpsChanged() },
                onClose = { disappear(true) }
            ) { key, value ->
                when (value) {
                    is Boolean -> mEditor?.putBoolean(key, value)
                    is Int -> mEditor?.putInt(key, value)
                    is Float -> mEditor?.putFloat(key, value)
                }
                mEditor?.apply()
            }
        }
    }


    fun cancel() {
        if (isDisplaying) {
            LauncherPreferences.PREF_ENABLE_GYRO = mOriginalGyroEnabled
            LauncherPreferences.PREF_GYRO_INVERT_X = mOriginalGyroXEnabled
            LauncherPreferences.PREF_GYRO_INVERT_Y = mOriginalGyroYEnabled
            LauncherPreferences.PREF_DISABLE_GESTURES = mOriginalGestureDisabled
            LauncherPreferences.PREF_GYRO_SENSITIVITY = mOriginalGyroSensitivity
            LauncherPreferences.PREF_MOUSESPEED = mOriginalMouseSpeed
            LauncherPreferences.PREF_LONGPRESS_TRIGGER = mOriginalGestureDelay
            LauncherPreferences.PREF_SCALE_FACTOR = mOriginalResolution
            LauncherPreferences.PREF_BUTTON_TRANSPARENCY = mOriginalButtonTransparency
            LauncherPreferences.PREF_SHOW_FPS = mOriginalShowFps
            LauncherPreferences.PREF_VOLUME_KEYS_CONTROL_ENABLED = mOriginalVolumeEnabled
            LauncherPreferences.PREF_VOLUME_UP_KEYBIND = mOriginalVolumeUp
            LauncherPreferences.PREF_VOLUME_DOWN_KEYBIND = mOriginalVolumeDown
            onGyroStateChanged()
            onResolutionChanged()
            onButtonTransparencyChanged()
            onShowFpsChanged()
        }
        disappear(true)
    }


    abstract fun onResolutionChanged()


    abstract fun onGyroStateChanged()


    open fun onButtonTransparencyChanged() {}


    abstract fun onShowFpsChanged()


    abstract fun onForceClose()


    abstract fun onViewOutput()


    abstract fun onCustomKey()
}

@Composable
private fun QuickSettingContent(
    selectedTab: Int,
    onResolutionChanged: () -> Unit,
    onGyroStateChanged: () -> Unit,
    onButtonTransparencyChanged: () -> Unit,
    onForceClose: () -> Unit,
    onViewOutput: () -> Unit,
    onCustomKey: () -> Unit,
    onShowFpsChanged: () -> Unit,
    onClose: () -> Unit,
    onPreferenceChanged: (String, Any) -> Unit
) {
    val context = LocalContext.current
    var enableGyro by remember { mutableStateOf(LauncherPreferences.PREF_ENABLE_GYRO) }
    var gyroInvertX by remember { mutableStateOf(LauncherPreferences.PREF_GYRO_INVERT_X) }
    var gyroInvertY by remember { mutableStateOf(LauncherPreferences.PREF_GYRO_INVERT_Y) }
    var gyroSensitivity by remember { mutableFloatStateOf(LauncherPreferences.PREF_GYRO_SENSITIVITY * 100f) }

    var mouseSpeed by remember { mutableFloatStateOf(LauncherPreferences.PREF_MOUSESPEED * 100f) }

    var disableGestures by remember { mutableStateOf(LauncherPreferences.PREF_DISABLE_GESTURES) }
    var gestureDelay by remember { mutableFloatStateOf(LauncherPreferences.PREF_LONGPRESS_TRIGGER.toFloat()) }

    var resolutionScaler by remember { mutableFloatStateOf(LauncherPreferences.PREF_SCALE_FACTOR * 100f) }
    var buttonTransparency by remember { mutableFloatStateOf(LauncherPreferences.PREF_BUTTON_TRANSPARENCY) }
    var showFps by remember { mutableStateOf(LauncherPreferences.PREF_SHOW_FPS) }

    var volumeKeysControlEnabled by remember { mutableStateOf(LauncherPreferences.PREF_VOLUME_KEYS_CONTROL_ENABLED) }
    var volumeUpKeybind by remember { mutableIntStateOf(LauncherPreferences.PREF_VOLUME_UP_KEYBIND) }
    var volumeDownKeybind by remember { mutableIntStateOf(LauncherPreferences.PREF_VOLUME_DOWN_KEYBIND) }

    var showKeyPickerFor by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val isGyroAvailable = remember { Tools.deviceSupportsGyro(context) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        var cardIndex = 0

        DialogCard(position = CardPosition.SINGLE, useSurface = true, delayIndex = cardIndex++) {
            DialogActionItem(
                title = translatedText(stringResource(R.string.close)),
                onClick = onClose
            )
        }

        @OptIn(ExperimentalAnimationApi::class)
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                }
            },
            label = "TabContent"
        ) { targetTab ->
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (targetTab) {
                    0 -> {
                        DialogCard(position = CardPosition.SINGLE, useSurface = true, delayIndex = cardIndex++) {
                            DialogSliderItem(
                                title = translatedText(stringResource(R.string.mcl_setting_title_resolution_scaler)),
                                value = resolutionScaler,
                                valueRange = 25f..100f,
                                valueSuffix = "%",
                                onValueChange = {
                                    resolutionScaler = it
                                    LauncherPreferences.PREF_SCALE_FACTOR = it / 100f
                                    onPreferenceChanged("resolutionRatio", it.toInt())
                                    scope.launch { onResolutionChanged() }
                                }
                            )
                        }

                        DialogCard(position = CardPosition.SINGLE, useSurface = true, delayIndex = cardIndex++) {
                            DialogSwitchItem(
                                title = translatedText("Show FPS Display"),
                                checked = showFps,
                                onCheckedChange = {
                                    showFps = it
                                    LauncherPreferences.PREF_SHOW_FPS = it
                                    onPreferenceChanged("show_fps", it)
                                    scope.launch { onShowFpsChanged() }
                                }
                            )
                        }

                        DialogCard(position = CardPosition.SINGLE, useSurface = true, delayIndex = cardIndex++) {
                            DialogSliderItem(
                                title = translatedText(stringResource(R.string.mcl_setting_title_buttonopacity)),
                                value = buttonTransparency,
                                valueRange = 0f..100f,
                                valueSuffix = "%",
                                onValueChange = {
                                    buttonTransparency = it
                                    LauncherPreferences.PREF_BUTTON_TRANSPARENCY = it
                                    onPreferenceChanged("buttonTransparency", it.toInt())
                                    scope.launch { onButtonTransparencyChanged() }
                                }
                            )
                        }
                    }
                    1 -> {
                        DialogCard(position = CardPosition.SINGLE, useSurface = true, delayIndex = cardIndex++) {
                            DialogSliderItem(
                                title = translatedText(stringResource(R.string.mcl_setting_title_mousespeed)),
                                value = mouseSpeed,
                                valueRange = 25f..300f,
                                valueSuffix = "%",
                                onValueChange = {
                                    mouseSpeed = it
                                    LauncherPreferences.PREF_MOUSESPEED = it / 100f
                                    onPreferenceChanged("mousespeed", it.toInt())
                                }
                            )
                        }

                        DialogCard(
                            position = CardPosition.SINGLE,
                            useSurface = true,
                            delayIndex = cardIndex++
                        ) {
                            DialogSwitchItem(
                                title = translatedText(stringResource(R.string.mcl_disable_gestures)),
                                checked = disableGestures,
                                onCheckedChange = {
                                    disableGestures = it
                                    LauncherPreferences.PREF_DISABLE_GESTURES = it
                                    onPreferenceChanged("disableGestures", it)
                                }
                            )
                        }

                        if (!disableGestures) {
                            DialogCard(position = CardPosition.SINGLE, useSurface = true, delayIndex = cardIndex++) {
                                DialogSliderItem(
                                    title = translatedText(stringResource(R.string.mcl_setting_title_longpresstrigger)),
                                    value = gestureDelay,
                                    valueRange = 100f..1000f,
                                    valueSuffix = " ms",
                                    onValueChange = {
                                        gestureDelay = it
                                        LauncherPreferences.PREF_LONGPRESS_TRIGGER = it.toInt()
                                        onPreferenceChanged("timeLongPressTrigger", it.toInt())
                                    }
                                )
                            }
                        }
                    }
                    2 -> {
                        if (isGyroAvailable) {
                            DialogCard(
                                position = CardPosition.SINGLE,
                                useSurface = true,
                                delayIndex = cardIndex++
                            ) {
                                DialogSwitchItem(
                                    title = translatedText(stringResource(R.string.preference_enable_gyro_title)),
                                    checked = enableGyro,
                                    onCheckedChange = {
                                        enableGyro = it
                                        LauncherPreferences.PREF_ENABLE_GYRO = it
                                        onPreferenceChanged("enableGyro", it)
                                        scope.launch { onGyroStateChanged() }
                                    }
                                )
                            }

                            if (enableGyro) {
                                DialogCard(
                                    position = CardPosition.SINGLE,
                                    useSurface = true,
                                    delayIndex = cardIndex++
                                ) {
                                    DialogSwitchItem(
                                        title = translatedText(stringResource(R.string.preference_gyro_invert_x_axis)),
                                        checked = gyroInvertX,
                                        onCheckedChange = {
                                            gyroInvertX = it
                                            LauncherPreferences.PREF_GYRO_INVERT_X = it
                                            onPreferenceChanged("gyroInvertX", it)
                                            scope.launch { onGyroStateChanged() }
                                        }
                                    )
                                }
                                DialogCard(
                                    position = CardPosition.SINGLE,
                                    useSurface = true,
                                    delayIndex = cardIndex++
                                ) {
                                    DialogSwitchItem(
                                        title = translatedText(stringResource(R.string.preference_gyro_invert_y_axis)),
                                        checked = gyroInvertY,
                                        onCheckedChange = {
                                            gyroInvertY = it
                                            LauncherPreferences.PREF_GYRO_INVERT_Y = it
                                            onPreferenceChanged("gyroInvertY", it)
                                            scope.launch { onGyroStateChanged() }
                                        }
                                    )
                                }
                                DialogCard(position = CardPosition.SINGLE, useSurface = true, delayIndex = cardIndex++) {
                                    DialogSliderItem(
                                        title = translatedText(stringResource(R.string.preference_gyro_sensitivity_title)),
                                        value = gyroSensitivity,
                                        valueRange = 25f..300f,
                                        valueSuffix = "%",
                                        onValueChange = {
                                            gyroSensitivity = it
                                            LauncherPreferences.PREF_GYRO_SENSITIVITY = it / 100f
                                            onPreferenceChanged("gyroSensitivity", it.toInt())
                                            scope.launch { onGyroStateChanged() }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    3 -> {
                        DialogCard(position = CardPosition.SINGLE, useSurface = true, delayIndex = cardIndex++) {
                            DialogSwitchItem(
                                title = translatedText("Enable Volume Key Controls"),
                                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                                checked = volumeKeysControlEnabled,
                                onCheckedChange = {
                                    volumeKeysControlEnabled = it
                                    LauncherPreferences.PREF_VOLUME_KEYS_CONTROL_ENABLED = it
                                    onPreferenceChanged("volume_keys_control_enabled", it)
                                }
                            )
                        }

                        DialogCard(position = CardPosition.SINGLE, useSurface = true, delayIndex = cardIndex++) {
                            DialogActionItem(
                                title = translatedText("Volume Up Keybind"),
                                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                                enabled = volumeKeysControlEnabled,
                                onClick = { showKeyPickerFor = "up" }
                            )
                        }

                        DialogCard(position = CardPosition.SINGLE, useSurface = true, delayIndex = cardIndex++) {
                            DialogActionItem(
                                title = translatedText("Volume Down Keybind"),
                                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                                enabled = volumeKeysControlEnabled,
                                onClick = { showKeyPickerFor = "down" }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        DialogCard(position = CardPosition.SINGLE, useSurface = true, delayIndex = cardIndex++) {
                            DialogActionItem(
                                title = translatedText(stringResource(R.string.control_forceclose)),
                                icon = Icons.Rounded.Close,
                                onClick = onForceClose
                            )
                        }

                        DialogCard(position = CardPosition.SINGLE, useSurface = true, delayIndex = cardIndex++) {
                            DialogActionItem(
                                title = translatedText(stringResource(R.string.control_viewout)),
                                icon = Icons.Rounded.Description,
                                onClick = onViewOutput
                            )
                        }

                        DialogCard(position = CardPosition.SINGLE, useSurface = true, delayIndex = cardIndex++) {
                            DialogActionItem(
                                title = translatedText(stringResource(R.string.control_customkey)),
                                icon = Icons.Rounded.Keyboard,
                                onClick = onCustomKey
                            )
                        }
                    }
                }
            }
        }
    }

    if (showKeyPickerFor != null) {
        com.ashmeet.hyperlauncher.components.dialog.KeycodePickerDialog(
            title = if (showKeyPickerFor == "up") translatedText("Volume Up Keybind") else translatedText("Volume Down Keybind"),
            initialValue = if (showKeyPickerFor == "up") volumeUpKeybind else volumeDownKeybind,
            onKeycodePicked = { keyCode ->
                if (showKeyPickerFor == "up") {
                    volumeUpKeybind = keyCode
                    LauncherPreferences.PREF_VOLUME_UP_KEYBIND = keyCode
                    onPreferenceChanged("volume_up_keybind", keyCode)
                } else {
                    volumeDownKeybind = keyCode
                    LauncherPreferences.PREF_VOLUME_DOWN_KEYBIND = keyCode
                    onPreferenceChanged("volume_down_keybind", keyCode)
                }
            },
            onDismiss = { showKeyPickerFor = null }
        )
    }
}
