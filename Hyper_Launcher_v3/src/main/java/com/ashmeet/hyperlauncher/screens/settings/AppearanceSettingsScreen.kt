package com.ashmeet.hyperlauncher.screens.settings

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewSidebar
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.ashmeet.hyperlauncher.fragments.dialog.ColorSelectorFragment
import com.ashmeet.hyperlauncher.utils.SideDialogUtils
import com.ashmeet.hyperlauncher.screens.settings.layouts.CardPosition
import com.ashmeet.hyperlauncher.screens.settings.layouts.SettingsCard
import com.ashmeet.hyperlauncher.screens.settings.layouts.SettingsScreenWrapper
import com.ashmeet.hyperlauncher.screens.settings.preferences.CursorPreferenceItem
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.screens.settings.preferences.PointerHotspotPickerDialog
import com.ashmeet.hyperlauncher.screens.settings.preferences.PreferenceCategory
import com.ashmeet.hyperlauncher.screens.settings.preferences.SettingsActionItem
import com.ashmeet.hyperlauncher.screens.settings.preferences.SettingsSliderItem
import com.ashmeet.hyperlauncher.screens.settings.preferences.SettingsSwitchItem
import com.ashmeet.hyperlauncher.screens.settings.preferences.SingleChoiceDialog
import com.ashmeet.hyperlauncher.screens.settings.preferences.cursorInfos
import com.ashmeet.hyperlauncher.utils.translation.Translator
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperlauncher.utils.Tools
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    onBack: () -> Unit
) {
    var screenTransition by remember { mutableStateOf(LauncherPreferences.PREF_SCREEN_TRANSITION) }
    var appTheme by remember { mutableStateOf(LauncherPreferences.PREF_THEME) }
    var appThemeType by remember { mutableStateOf(LauncherPreferences.PREF_THEME_TYPE) }
    var appLanguage by remember { mutableStateOf(LauncherPreferences.PREF_LANGUAGE) }
    var isCustomTheme by remember { mutableStateOf(LauncherPreferences.PREF_CUSTOM_THEME) }
    var themeColor by remember { mutableIntStateOf(LauncherPreferences.PREF_THEME_COLOR) }
    var hideSidebar by remember { mutableStateOf(LauncherPreferences.PREF_HIDE_SIDEBAR) }

    var drawerSizePerc by remember { mutableFloatStateOf(LauncherPreferences.PREF_DRAWER_PULL_SIZE_PERC) }

    var drawerBgOpacity by remember { mutableFloatStateOf(LauncherPreferences.PREF_DRAWER_PULL_BG_OPACITY.toFloat()) }
    var drawerIconOpacity by remember { mutableFloatStateOf(LauncherPreferences.PREF_DRAWER_PULL_ICON_OPACITY.toFloat()) }
    var drawerHoldToMove by remember { mutableStateOf(LauncherPreferences.PREF_DRAWER_PULL_HOLD_TO_MOVE) }
    var drawerBackground by remember { mutableStateOf(LauncherPreferences.PREF_DRAWER_PULL_BACKGROUND) }
    var drawerIconPath by remember { mutableStateOf(LauncherPreferences.PREF_DRAWER_PULL_ICON_PATH) }

    var pointerIconPath by remember { mutableStateOf(LauncherPreferences.PREF_POINTER_ICON_PATH) }
    var pointerHotspotX by remember { mutableFloatStateOf(LauncherPreferences.PREF_POINTER_HOTSPOT_X.toFloat()) }
    var pointerHotspotY by remember { mutableFloatStateOf(LauncherPreferences.PREF_POINTER_HOTSPOT_Y.toFloat()) }
    var mouseScale by remember { mutableFloatStateOf(LauncherPreferences.PREF_MOUSESCALE * 100f) }

    var pendingCursorShape by remember { mutableIntStateOf(-1) }
    var editingCursorShape by remember { mutableIntStateOf(-1) }

    val pointerPaths = remember {
        mutableStateMapOf<Int, String?>().apply {
            put(0, LauncherPreferences.PREF_POINTER_ICON_PATH_ARROW)
            put(1, LauncherPreferences.PREF_POINTER_ICON_PATH_IBEAM)
            put(2, LauncherPreferences.PREF_POINTER_ICON_PATH_CROSSHAIR)
            put(3, LauncherPreferences.PREF_POINTER_ICON_PATH_HAND)
            put(4, LauncherPreferences.PREF_POINTER_ICON_PATH_HRESIZE)
            put(5, LauncherPreferences.PREF_POINTER_ICON_PATH_VRESIZE)
            put(6, LauncherPreferences.PREF_POINTER_ICON_PATH_ALL_RESIZE)
            put(7, LauncherPreferences.PREF_POINTER_ICON_PATH_NOT_ALLOWED)
        }
    }

    val pointerHotspots = remember {
        mutableStateMapOf<Int, Pair<Float, Float>>().apply {
            put(0, LauncherPreferences.PREF_POINTER_HOTSPOT_X_ARROW.toFloat() to LauncherPreferences.PREF_POINTER_HOTSPOT_Y_ARROW.toFloat())
            put(1, LauncherPreferences.PREF_POINTER_HOTSPOT_X_IBEAM.toFloat() to LauncherPreferences.PREF_POINTER_HOTSPOT_Y_IBEAM.toFloat())
            put(2, LauncherPreferences.PREF_POINTER_HOTSPOT_X_CROSSHAIR.toFloat() to LauncherPreferences.PREF_POINTER_HOTSPOT_Y_CROSSHAIR.toFloat())
            put(3, LauncherPreferences.PREF_POINTER_HOTSPOT_X_HAND.toFloat() to LauncherPreferences.PREF_POINTER_HOTSPOT_Y_HAND.toFloat())
            put(4, LauncherPreferences.PREF_POINTER_HOTSPOT_X_HRESIZE.toFloat() to LauncherPreferences.PREF_POINTER_HOTSPOT_Y_HRESIZE.toFloat())
            put(5, LauncherPreferences.PREF_POINTER_HOTSPOT_X_VRESIZE.toFloat() to LauncherPreferences.PREF_POINTER_HOTSPOT_Y_VRESIZE.toFloat())
            put(6, LauncherPreferences.PREF_POINTER_HOTSPOT_X_ALL_RESIZE.toFloat() to LauncherPreferences.PREF_POINTER_HOTSPOT_Y_ALL_RESIZE.toFloat())
            put(7, LauncherPreferences.PREF_POINTER_HOTSPOT_X_NOT_ALLOWED.toFloat() to LauncherPreferences.PREF_POINTER_HOTSPOT_Y_NOT_ALLOWED.toFloat())
        }
    }

    var launcherBgPath by remember { mutableStateOf(LauncherPreferences.PREF_LAUNCHER_BACKGROUND_PATH) }
    var launcherBgType by remember { mutableStateOf(LauncherPreferences.PREF_LAUNCHER_BACKGROUND_TYPE) }
    var launcherBgOverlayEnabled by remember { mutableStateOf(LauncherPreferences.PREF_LAUNCHER_BACKGROUND_OVERLAY_ENABLED) }
    var launcherBgOverlayOpacity by remember { mutableFloatStateOf(LauncherPreferences.PREF_LAUNCHER_BACKGROUND_OVERLAY_OPACITY.toFloat()) }
    var launcherBgBlurEnabled by remember { mutableStateOf(LauncherPreferences.PREF_LAUNCHER_BACKGROUND_BLUR_ENABLED) }
    var launcherBgBlur by remember { mutableFloatStateOf(LauncherPreferences.PREF_LAUNCHER_BACKGROUND_BLUR.toFloat()) }
    var launcherVideoMuted by remember { mutableStateOf(LauncherPreferences.PREF_LAUNCHER_VIDEO_MUTED) }
    var launcherVideoVolume by remember { mutableFloatStateOf(LauncherPreferences.PREF_LAUNCHER_VIDEO_VOLUME.toFloat()) }
    var launcherVideoLoop by remember { mutableStateOf(LauncherPreferences.PREF_LAUNCHER_VIDEO_LOOP) }
    var launcherBlurredElementsEnabled by remember { mutableStateOf(LauncherPreferences.PREF_BLURRED_ELEMENTS_ENABLED) }

    val context = LocalContext.current

    val individualPointerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && pendingCursorShape != -1) {
            val shape = pendingCursorShape
            val suffix = cursorInfos.find { it.shapeId == shape }?.suffix ?: ""
            val destination = File(Tools.DIR_DATA, "custom_pointer_$suffix.png")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destination).use { output ->
                        input.copyTo(output)
                    }
                }
                val path = destination.absolutePath
                LauncherPreferences.prefs.edit { putString("pointer_icon_path_$suffix", path) }
                when (shape) {
                    0 -> LauncherPreferences.PREF_POINTER_ICON_PATH_ARROW = path
                    1 -> LauncherPreferences.PREF_POINTER_ICON_PATH_IBEAM = path
                    2 -> LauncherPreferences.PREF_POINTER_ICON_PATH_CROSSHAIR = path
                    3 -> LauncherPreferences.PREF_POINTER_ICON_PATH_HAND = path
                    4 -> LauncherPreferences.PREF_POINTER_ICON_PATH_HRESIZE = path
                    5 -> LauncherPreferences.PREF_POINTER_ICON_PATH_VRESIZE = path
                    6 -> LauncherPreferences.PREF_POINTER_ICON_PATH_ALL_RESIZE = path
                    7 -> LauncherPreferences.PREF_POINTER_ICON_PATH_NOT_ALLOWED = path
                }
                pointerPaths[shape] = path
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resetIndividualPointer(shape: Int) {
        val suffix = cursorInfos.find { it.shapeId == shape }?.suffix ?: ""
        LauncherPreferences.prefs.edit {
            remove("pointer_icon_path_$suffix")
            remove("pointer_hotspot_x_$suffix")
            remove("pointer_hotspot_y_$suffix")
        }
        when (shape) {
            0 -> {
                LauncherPreferences.PREF_POINTER_ICON_PATH_ARROW = null
                LauncherPreferences.PREF_POINTER_HOTSPOT_X_ARROW = -1
                LauncherPreferences.PREF_POINTER_HOTSPOT_Y_ARROW = -1
            }
            1 -> {
                LauncherPreferences.PREF_POINTER_ICON_PATH_IBEAM = null
                LauncherPreferences.PREF_POINTER_HOTSPOT_X_IBEAM = -1
                LauncherPreferences.PREF_POINTER_HOTSPOT_Y_IBEAM = -1
            }
            2 -> {
                LauncherPreferences.PREF_POINTER_ICON_PATH_CROSSHAIR = null
                LauncherPreferences.PREF_POINTER_HOTSPOT_X_CROSSHAIR = -1
                LauncherPreferences.PREF_POINTER_HOTSPOT_Y_CROSSHAIR = -1
            }
            3 -> {
                LauncherPreferences.PREF_POINTER_ICON_PATH_HAND = null
                LauncherPreferences.PREF_POINTER_HOTSPOT_X_HAND = -1
                LauncherPreferences.PREF_POINTER_HOTSPOT_Y_HAND = -1
            }
            4 -> {
                LauncherPreferences.PREF_POINTER_ICON_PATH_HRESIZE = null
                LauncherPreferences.PREF_POINTER_HOTSPOT_X_HRESIZE = -1
                LauncherPreferences.PREF_POINTER_HOTSPOT_Y_HRESIZE = -1
            }
            5 -> {
                LauncherPreferences.PREF_POINTER_ICON_PATH_VRESIZE = null
                LauncherPreferences.PREF_POINTER_HOTSPOT_X_VRESIZE = -1
                LauncherPreferences.PREF_POINTER_HOTSPOT_Y_VRESIZE = -1
            }
            6 -> {
                LauncherPreferences.PREF_POINTER_ICON_PATH_ALL_RESIZE = null
                LauncherPreferences.PREF_POINTER_HOTSPOT_X_ALL_RESIZE = -1
                LauncherPreferences.PREF_POINTER_HOTSPOT_Y_ALL_RESIZE = -1
            }
            7 -> {
                LauncherPreferences.PREF_POINTER_ICON_PATH_NOT_ALLOWED = null
                LauncherPreferences.PREF_POINTER_HOTSPOT_X_NOT_ALLOWED = -1
                LauncherPreferences.PREF_POINTER_HOTSPOT_Y_NOT_ALLOWED = -1
            }
        }
        pointerPaths[shape] = null
        pointerHotspots[shape] = -1f to -1f
    }

    var showTransitionDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showThemeTypeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showHotspotDialog by remember { mutableStateOf(false) }

    val transitionOptions = listOf("none", "fade", "bounce")
    val transitionOptionNames = transitionOptions.map { id ->
        when (id) {
            "none" -> translatedText(stringResource(R.string.preference_screen_transition_none))
            "fade" -> translatedText(stringResource(R.string.preference_screen_transition_fade))
            "bounce" -> translatedText(stringResource(R.string.preference_screen_transition_bounce))
            else -> id
        }
    }

    val themeOptions = listOf("system", "light", "dark", "dynamic")
    val themeOptionNames = themeOptions.map { id ->
        when (id) {
            "system" -> translatedText(stringResource(R.string.preference_app_theme_system))
            "light" -> translatedText(stringResource(R.string.preference_app_theme_light))
            "dark" -> translatedText(stringResource(R.string.preference_app_theme_dark))
            "dynamic" -> translatedText(stringResource(R.string.preference_app_theme_dynamic))
            else -> id
        }
    }

    val themeTypeOptions = listOf("tonal", "vibrant", "tertiary", "monochrome", "neutral", "content")
    val themeTypeOptionNames = themeTypeOptions.map { id ->
        when (id) {
            "tonal" -> translatedText("Tonal")
            "vibrant" -> translatedText("Vibrant")
            "tertiary" -> translatedText("Tertiary")
            "monochrome" -> translatedText("Monochrome")
            "neutral" -> translatedText("Neutral")
            "content" -> translatedText("Content")
            else -> id.replaceFirstChar { it.uppercase() }
        }
    }

    val languageOptions = listOf("system", "en", "es", "fr", "de", "it", "ja", "zh", "ru", "ko", "pt", "tr", "vi", "hi", "ar", "uk")
    val languageOptionNames = languageOptions.map { id ->
        when (id) {
            "system" -> translatedText(stringResource(R.string.preference_language_system))
            "en" -> translatedText(stringResource(R.string.preference_language_english))
            "es" -> translatedText(stringResource(R.string.preference_language_spanish))
            "fr" -> translatedText(stringResource(R.string.preference_language_french))
            "de" -> translatedText(stringResource(R.string.preference_language_german))
            "it" -> translatedText(stringResource(R.string.preference_language_italian))
            "ja" -> translatedText(stringResource(R.string.preference_language_japanese))
            "zh" -> translatedText(stringResource(R.string.preference_language_chinese))
            "ru" -> translatedText(stringResource(R.string.preference_language_russian))
            "ko" -> translatedText(stringResource(R.string.preference_language_korean))
            "pt" -> translatedText(stringResource(R.string.preference_language_portuguese))
            "tr" -> translatedText(stringResource(R.string.preference_language_turkish))
            "vi" -> translatedText(stringResource(R.string.preference_language_vietnamese))
            "hi" -> translatedText(stringResource(R.string.preference_language_hindi))
            "ar" -> translatedText(stringResource(R.string.preference_language_arabic))
            "uk" -> translatedText(stringResource(R.string.preference_language_ukrainian))
            else -> id
        }
    }

    SettingsScreenWrapper(
        title = translatedText(stringResource(R.string.preference_appearance_title)),
        onBack = onBack,
        addTopGap = true
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingsCard(position = CardPosition.TOP, useSurface = true) {
                SettingsActionItem(
                    title = translatedText(stringResource(R.string.preference_app_theme_title)),
                    summary = themeOptionNames[themeOptions.indexOf(appTheme).coerceAtLeast(0)],
                    icon = Icons.Default.Palette,
                    onClick = { showThemeDialog = true }
                )
            }

            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsActionItem(
                    title = translatedText("Theme Type"),
                    summary = themeTypeOptionNames[themeTypeOptions.indexOf(appThemeType).coerceAtLeast(0)],
                    icon = Icons.Rounded.ColorLens,
                    onClick = { showThemeTypeDialog = true }
                )
            }

            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsActionItem(
                    title = translatedText(stringResource(R.string.preference_language_title)),
                    summary = languageOptionNames[languageOptions.indexOf(appLanguage).coerceAtLeast(0)],
                    icon = Icons.Default.Language,
                    onClick = { showLanguageDialog = true }
                )
            }

            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsSwitchItem(
                    title = translatedText("Custom Theme"),
                    summary = translatedText("Enable custom colors for the launcher"),
                    icon = Icons.Rounded.ColorLens,
                    checked = isCustomTheme,
                    onCheckedChange = {
                        isCustomTheme = it
                        LauncherPreferences.prefs.edit { putBoolean("app_custom_theme", it) }
                        LauncherPreferences.PREF_CUSTOM_THEME = it
                        LauncherPreferences.loadPreferences(context)
                    }
                )
            }

            if (isCustomTheme) {
                SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                    SettingsActionItem(
                        title = translatedText("Theme Color"),
                        summary = translatedText("Choose a custom color for the launcher"),
                        icon = Icons.Rounded.ColorLens,
                        onClick = {
                            val colorSelector = ColorSelectorFragment { color ->
                                themeColor = color
                                LauncherPreferences.prefs.edit { putInt("app_theme_color", color) }
                                LauncherPreferences.PREF_THEME_COLOR = color
                                LauncherPreferences.loadPreferences(context)
                            }
                            colorSelector.setAlphaEnabled(false)
                            colorSelector.setLockSatVal(true)
                            colorSelector.show(true, themeColor)
                            SideDialogUtils.activeDialog = colorSelector
                        }
                    )
                }
            }

            SettingsCard(position = CardPosition.BOTTOM, useSurface = true) {
                SettingsActionItem(
                    title = translatedText(stringResource(R.string.preference_screen_transition_title)),
                    summary = transitionOptionNames[transitionOptions.indexOf(screenTransition).coerceAtLeast(0)],
                    icon = Icons.Default.Animation,
                    onClick = { showTransitionDialog = true }
                )
            }

            PreferenceCategory(title = translatedText("Main Menu"))
            SettingsCard(position = CardPosition.SINGLE, useSurface = true) {
                SettingsSwitchItem(
                    title = translatedText("Hide sidebar"),
                    summary = translatedText("Hide action buttons on the left side of the main menu"),
                    icon = Icons.AutoMirrored.Rounded.ViewSidebar,
                    checked = hideSidebar,
                    onCheckedChange = {
                        hideSidebar = it
                        LauncherPreferences.prefs.edit { putBoolean("hide_sidebar", it) }
                        LauncherPreferences.PREF_HIDE_SIDEBAR = it
                    }
                )
            }

            PreferenceCategory(title = translatedText("Drawer Button"))
            SettingsCard(position = CardPosition.TOP, useSurface = true) {
                SettingsSliderItem(
                    title = translatedText("Size"),
                    icon = Icons.Rounded.AspectRatio,
                    value = drawerSizePerc,
                    valueRange = 10f..100f,
                    onValueChange = {
                        drawerSizePerc = it
                        LauncherPreferences.prefs.edit { putFloat("drawer_pull_size_perc", it) }
                        LauncherPreferences.PREF_DRAWER_PULL_SIZE_PERC = it
                    },
                    valueSuffix = "%"
                )
            }
            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsSliderItem(
                    title = translatedText("Background Opacity"),
                    icon = Icons.Rounded.Opacity,
                    value = drawerBgOpacity,
                    valueRange = 0f..100f,
                    onValueChange = {
                        drawerBgOpacity = it
                        LauncherPreferences.prefs.edit { putInt("drawer_pull_opacity", it.toInt()) }
                        LauncherPreferences.PREF_DRAWER_PULL_BG_OPACITY = it.toInt()
                    },
                    valueSuffix = "%"
                )
            }
            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsSliderItem(
                    title = translatedText("Icon Opacity"),
                    icon = Icons.Rounded.Opacity,
                    value = drawerIconOpacity,
                    valueRange = 0f..100f,
                    onValueChange = {
                        drawerIconOpacity = it
                        LauncherPreferences.prefs.edit { putInt("drawer_pull_icon_opacity", it.toInt()) }
                        LauncherPreferences.PREF_DRAWER_PULL_ICON_OPACITY = it.toInt()
                    },
                    valueSuffix = "%"
                )
            }
            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsSwitchItem(
                    title = translatedText("Hold to move"),
                    icon = Icons.Rounded.DragIndicator,
                    checked = drawerHoldToMove,
                    onCheckedChange = {
                        drawerHoldToMove = it
                        LauncherPreferences.prefs.edit { putBoolean("drawer_pull_hold_to_move", it) }
                        LauncherPreferences.PREF_DRAWER_PULL_HOLD_TO_MOVE = it
                    }
                )
            }
            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsSwitchItem(
                    title = translatedText("Show background"),
                    icon = Icons.Rounded.RadioButtonUnchecked,
                    checked = drawerBackground,
                    onCheckedChange = {
                        drawerBackground = it
                        LauncherPreferences.prefs.edit { putBoolean("drawer_pull_background", it) }
                        LauncherPreferences.PREF_DRAWER_PULL_BACKGROUND = it
                    }
                )
            }

            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                    if (uri != null) {
                        val destination = File(Tools.DIR_DATA, "custom_drawer_icon.png")
                        try {
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                FileOutputStream(destination).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            LauncherPreferences.prefs.edit { putString("drawer_pull_icon_path", destination.absolutePath) }
                            LauncherPreferences.PREF_DRAWER_PULL_ICON_PATH = destination.absolutePath
                            drawerIconPath = destination.absolutePath
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                SettingsActionItem(
                    title = translatedText("Change icon image"),
                    summary = if (drawerIconPath != null) "Custom icon active" else "Default icon active",
                    icon = Icons.Rounded.AddPhotoAlternate,
                    onClick = { launcher.launch("image/*") }
                )
            }
            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsActionItem(
                    title = translatedText("Reset icon"),
                    icon = Icons.Rounded.Restore,
                    onClick = {
                        LauncherPreferences.prefs.edit { remove("drawer_pull_icon_path") }
                        LauncherPreferences.PREF_DRAWER_PULL_ICON_PATH = null
                        drawerIconPath = null
                    }
                )
            }
            SettingsCard(position = CardPosition.BOTTOM, useSurface = true) {
                SettingsActionItem(
                    title = translatedText("Reset position"),
                    icon = Icons.Rounded.Restore,
                    onClick = {
                        LauncherPreferences.prefs.edit {
                            remove("drawer_pull_pos_x")
                            remove("drawer_pull_pos_y")
                        }
                        LauncherPreferences.PREF_DRAWER_PULL_POS_X = -1f
                        LauncherPreferences.PREF_DRAWER_PULL_POS_Y = -1f
                    }
                )
            }

            PreferenceCategory(title = translatedText("Launcher Background"))
            SettingsCard(position = CardPosition.TOP, useSurface = true) {
                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                    if (uri != null) {
                        val type = context.contentResolver.getType(uri)
                        val isVideo = type?.startsWith("video") == true
                        val extension = if (isVideo) "mp4" else "png"
                        val destination = File(Tools.DIR_DATA, "launcher_background_${System.currentTimeMillis()}.$extension")
                        try {
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                FileOutputStream(destination).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            val path = destination.absolutePath
                            val bgType = if (isVideo) "video" else "image"
                            LauncherPreferences.prefs.edit {
                                putString("launcher_background_path", path)
                                putString("launcher_background_type", bgType)
                            }
                            LauncherPreferences.PREF_LAUNCHER_BACKGROUND_PATH = path
                            LauncherPreferences.PREF_LAUNCHER_BACKGROUND_TYPE = bgType
                            launcherBgPath = path
                            launcherBgType = bgType
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                SettingsActionItem(
                    title = translatedText("Change Background"),
                    summary = if (launcherBgPath != null) translatedText("Custom background active") else translatedText("Default background active"),
                    icon = Icons.Rounded.AddPhotoAlternate,
                    onClick = { launcher.launch("*/*") }
                )
            }
            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsActionItem(
                    title = translatedText("Reset Background"),
                    icon = Icons.Rounded.Restore,
                    onClick = {
                        LauncherPreferences.prefs.edit {
                            remove("launcher_background_path")
                            remove("launcher_background_type")
                        }
                        LauncherPreferences.PREF_LAUNCHER_BACKGROUND_PATH = null
                        LauncherPreferences.PREF_LAUNCHER_BACKGROUND_TYPE = "image"
                        launcherBgPath = null
                        launcherBgType = "image"
                    }
                )
            }
            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsSwitchItem(
                    title = translatedText("Enable Overlay"),
                    summary = translatedText("Show a color overlay on top of the background"),
                    icon = Icons.Rounded.Layers,
                    checked = launcherBgOverlayEnabled,
                    onCheckedChange = {
                        launcherBgOverlayEnabled = it
                        LauncherPreferences.prefs.edit { putBoolean("launcher_background_overlay_enabled", it) }
                        LauncherPreferences.PREF_LAUNCHER_BACKGROUND_OVERLAY_ENABLED = it
                    },
                    enabled = launcherBgPath != null
                )
            }
            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsSliderItem(
                    title = translatedText("Overlay Opacity"),
                    icon = Icons.Rounded.Opacity,
                    value = launcherBgOverlayOpacity,
                    valueRange = 0f..100f,
                    onValueChange = {
                        launcherBgOverlayOpacity = it
                        LauncherPreferences.prefs.edit { putInt("launcher_background_overlay_opacity", it.toInt()) }
                        LauncherPreferences.PREF_LAUNCHER_BACKGROUND_OVERLAY_OPACITY = it.toInt()
                    },
                    valueSuffix = "%",
                    enabled = launcherBgPath != null && launcherBgOverlayEnabled
                )
            }

            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsSwitchItem(
                    title = translatedText("Background Blur"),
                    summary = translatedText("Apply a blur effect to the background"),
                    icon = Icons.Rounded.BlurOn,
                    checked = launcherBgBlurEnabled,
                    onCheckedChange = {
                        launcherBgBlurEnabled = it
                        LauncherPreferences.prefs.edit { putBoolean("launcher_background_blur_enabled", it) }
                        LauncherPreferences.PREF_LAUNCHER_BACKGROUND_BLUR_ENABLED = it
                    },
                    enabled = launcherBgPath != null
                )
            }

            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsSliderItem(
                    title = translatedText("Blur Intensity"),
                    icon = Icons.Rounded.Opacity,
                    value = launcherBgBlur,
                    valueRange = 0f..25f,
                    onValueChange = {
                        launcherBgBlur = it
                        LauncherPreferences.prefs.edit { putInt("launcher_background_blur", it.toInt()) }
                        LauncherPreferences.PREF_LAUNCHER_BACKGROUND_BLUR = it.toInt()
                    },
                    enabled = launcherBgPath != null && launcherBgBlurEnabled
                )
            }

            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsSwitchItem(
                    title = translatedText("Blurred Elements"),
                    summary = translatedText("Apply a blurred matte finish to UI components"),
                    icon = Icons.Rounded.BlurOn,
                    checked = launcherBlurredElementsEnabled,
                    onCheckedChange = {
                        launcherBlurredElementsEnabled = it
                        LauncherPreferences.prefs.edit { putBoolean("blurred_elements_enabled", it) }
                        LauncherPreferences.PREF_BLURRED_ELEMENTS_ENABLED = it
                    }
                )
            }

            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsSwitchItem(
                    title = translatedText("Mute Video"),
                    summary = translatedText("Disable sound for the background video"),
                    icon = if (launcherVideoMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                    checked = launcherVideoMuted,
                    onCheckedChange = {
                        launcherVideoMuted = it
                        LauncherPreferences.prefs.edit { putBoolean("launcher_video_muted", it) }
                        LauncherPreferences.PREF_LAUNCHER_VIDEO_MUTED = it
                    },
                    enabled = launcherBgPath != null && launcherBgType == "video"
                )
            }

            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsSwitchItem(
                    title = translatedText("Loop Video"),
                    summary = translatedText("Automatically restart the video when it ends"),
                    icon = Icons.Rounded.Repeat,
                    checked = launcherVideoLoop,
                    onCheckedChange = {
                        launcherVideoLoop = it
                        LauncherPreferences.prefs.edit { putBoolean("launcher_video_loop", it) }
                        LauncherPreferences.PREF_LAUNCHER_VIDEO_LOOP = it
                    },
                    enabled = launcherBgPath != null && launcherBgType == "video"
                )
            }

            SettingsCard(position = CardPosition.BOTTOM, useSurface = true) {
                SettingsSliderItem(
                    title = translatedText("Video Volume"),
                    icon = Icons.AutoMirrored.Rounded.VolumeUp,
                    value = launcherVideoVolume,
                    valueRange = 0f..100f,
                    onValueChange = {
                        launcherVideoVolume = it
                        LauncherPreferences.prefs.edit { putInt("launcher_video_volume", it.toInt()) }
                        LauncherPreferences.PREF_LAUNCHER_VIDEO_VOLUME = it.toInt()
                    },
                    valueSuffix = "%",
                    enabled = launcherBgPath != null && !launcherVideoMuted && launcherBgType == "video"
                )
            }

            PreferenceCategory(title = translatedText("Pointer Settings"))
            SettingsCard(position = CardPosition.TOP, useSurface = true) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val painter = if (pointerIconPath != null) {
                        val bitmap = remember(pointerIconPath) {
                            try {
                                BitmapFactory.decodeFile(pointerIconPath)
                            } catch (_: Exception) {
                                null
                            }
                        }
                        if (bitmap != null) {
                            BitmapPainter(bitmap.asImageBitmap())
                        } else {
                            androidx.compose.ui.res.painterResource(id = R.drawable.img_mouse_pointer_arrow)
                        }
                    } else {
                        androidx.compose.ui.res.painterResource(id = R.drawable.img_mouse_pointer_arrow)
                    }

                    Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        SettingsSliderItem(
                            title = translatedText("Pointer Size"),
                            icon = null,
                            value = mouseScale,
                            valueRange = 25f..300f,
                            onValueChange = {
                                mouseScale = it
                                LauncherPreferences.prefs.edit { putInt("mousescale", it.toInt()) }
                                LauncherPreferences.loadPreferences(context)
                            },
                            valueSuffix = "%"
                        )
                    }
                }
            }
            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                    if (uri != null) {
                        val destination = File(Tools.DIR_DATA, "custom_pointer_icon.png")
                        try {
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                FileOutputStream(destination).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            LauncherPreferences.prefs.edit { putString("pointer_icon_path", destination.absolutePath) }
                            LauncherPreferences.PREF_POINTER_ICON_PATH = destination.absolutePath
                            pointerIconPath = destination.absolutePath
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                SettingsActionItem(
                    title = translatedText("Change pointer image"),
                    summary = if (pointerIconPath != null) "Custom pointer active" else "Default pointer active",
                    icon = Icons.Rounded.AddPhotoAlternate,
                    onClick = { launcher.launch("image/*") }
                )
            }
            SettingsCard(position = CardPosition.MIDDLE, useSurface = true) {
                SettingsActionItem(
                    title = translatedText("Adjust Hotspot"),
                    summary = translatedText("Set the point where clicks occur") + " (${pointerHotspotX.toInt()}, ${pointerHotspotY.toInt()})",
                    icon = Icons.Rounded.DragIndicator,
                    onClick = { showHotspotDialog = true }
                )
            }
            SettingsCard(position = CardPosition.BOTTOM, useSurface = true) {
                SettingsActionItem(
                    title = translatedText("Reset pointer"),
                    icon = Icons.Rounded.Restore,
                    onClick = {
                        LauncherPreferences.prefs.edit {
                            remove("pointer_icon_path")
                            remove("pointer_hotspot_x")
                            remove("pointer_hotspot_y")
                        }
                        LauncherPreferences.PREF_POINTER_ICON_PATH = null
                        LauncherPreferences.PREF_POINTER_HOTSPOT_X = 0
                        LauncherPreferences.PREF_POINTER_HOTSPOT_Y = 0
                        pointerIconPath = null
                        pointerHotspotX = 0f
                        pointerHotspotY = 0f
                    }
                )
            }

            PreferenceCategory(title = translatedText("Individual Pointers"))
            cursorInfos.forEachIndexed { index, cursorInfo ->
                SettingsCard(
                    position = when {
                        cursorInfos.size == 1 -> CardPosition.SINGLE
                        index == 0 -> CardPosition.TOP
                        index == cursorInfos.size - 1 -> CardPosition.BOTTOM
                        else -> CardPosition.MIDDLE
                    },
                    useSurface = true
                ) {
                    CursorPreferenceItem(
                        title = translatedText(cursorInfo.name),
                        shapeId = cursorInfo.shapeId,
                        imagePath = pointerPaths[cursorInfo.shapeId],
                        hotspotX = pointerHotspots[cursorInfo.shapeId]?.first ?: -1f,
                        hotspotY = pointerHotspots[cursorInfo.shapeId]?.second ?: -1f,
                        onPickImage = {
                            pendingCursorShape = cursorInfo.shapeId
                            individualPointerLauncher.launch("image/*")
                        },
                        onAdjustHotspot = {
                            editingCursorShape = cursorInfo.shapeId
                        },
                        onReset = {
                            resetIndividualPointer(cursorInfo.shapeId)
                        }
                    )
                }
            }
        }

        if (showThemeDialog) {
            SingleChoiceDialog(
                title = translatedText(stringResource(R.string.preference_app_theme_title)),
                options = themeOptionNames,
                optionValues = themeOptions,
                selectedValue = appTheme,
                onValueChange = { newValue ->
                    appTheme = newValue
                    LauncherPreferences.prefs.edit { putString("app_theme", newValue) }
                    LauncherPreferences.loadPreferences(context)
                },
                onDismiss = { showThemeDialog = false }
            )
        }

        if (showThemeTypeDialog) {
            SingleChoiceDialog(
                title = translatedText("Theme Type"),
                options = themeTypeOptionNames,
                optionValues = themeTypeOptions,
                selectedValue = appThemeType,
                onValueChange = { newValue ->
                    appThemeType = newValue
                    LauncherPreferences.prefs.edit { putString("app_theme_type", newValue) }
                    LauncherPreferences.loadPreferences(context)
                },
                onDismiss = { showThemeTypeDialog = false }
            )
        }

        if (showLanguageDialog) {
            SingleChoiceDialog(
                title = translatedText(stringResource(R.string.preference_language_title)),
                options = languageOptionNames,
                optionValues = languageOptions,
                selectedValue = appLanguage,
                onValueChange = { newValue ->
                    appLanguage = newValue
                    LauncherPreferences.prefs.edit { putString("app_language", newValue) }
                    LauncherPreferences.loadPreferences(context)
                    Translator.prefetchTranslations(context)
                },
                onDismiss = { showLanguageDialog = false }
            )
        }

        if (showTransitionDialog) {
            SingleChoiceDialog(
                title = translatedText(stringResource(R.string.preference_screen_transition_title)),
                options = transitionOptionNames,
                optionValues = transitionOptions,
                selectedValue = screenTransition,
                onValueChange = { newValue ->
                    screenTransition = newValue
                    LauncherPreferences.prefs.edit { putString("screen_transition", newValue) }
                    LauncherPreferences.loadPreferences(context)
                },
                onDismiss = { showTransitionDialog = false }
            )
        }

        if (showHotspotDialog || editingCursorShape != -1) {
            val shape = editingCursorShape
            val isGeneral = shape == -1

            PointerHotspotPickerDialog(
                title = translatedText("Adjust Hotspot"),
                imagePath = if (isGeneral) pointerIconPath else pointerPaths[shape],
                shapeId = if (isGeneral) 0 else shape,
                initialX = if (isGeneral) pointerHotspotX else pointerHotspots[shape]?.first ?: -1f,
                initialY = if (isGeneral) pointerHotspotY else pointerHotspots[shape]?.second ?: -1f,
                onConfirm = { x, y ->
                    if (isGeneral) {
                        pointerHotspotX = x
                        pointerHotspotY = y
                        LauncherPreferences.prefs.edit {
                            putInt("pointer_hotspot_x", x.toInt())
                            putInt("pointer_hotspot_y", y.toInt())
                        }
                        LauncherPreferences.PREF_POINTER_HOTSPOT_X = x.toInt()
                        LauncherPreferences.PREF_POINTER_HOTSPOT_Y = y.toInt()
                        showHotspotDialog = false
                    } else {
                        val suffix = cursorInfos.find { it.shapeId == shape }?.suffix ?: ""
                        LauncherPreferences.prefs.edit {
                            putInt("pointer_hotspot_x_$suffix", x.toInt())
                            putInt("pointer_hotspot_y_$suffix", y.toInt())
                        }
                        when (shape) {
                            0 -> { LauncherPreferences.PREF_POINTER_HOTSPOT_X_ARROW = x.toInt(); LauncherPreferences.PREF_POINTER_HOTSPOT_Y_ARROW = y.toInt() }
                            1 -> { LauncherPreferences.PREF_POINTER_HOTSPOT_X_IBEAM = x.toInt(); LauncherPreferences.PREF_POINTER_HOTSPOT_Y_IBEAM = y.toInt() }
                            2 -> { LauncherPreferences.PREF_POINTER_HOTSPOT_X_CROSSHAIR = x.toInt(); LauncherPreferences.PREF_POINTER_HOTSPOT_Y_CROSSHAIR = y.toInt() }
                            3 -> { LauncherPreferences.PREF_POINTER_HOTSPOT_X_HAND = x.toInt(); LauncherPreferences.PREF_POINTER_HOTSPOT_Y_HAND = y.toInt() }
                            4 -> { LauncherPreferences.PREF_POINTER_HOTSPOT_X_HRESIZE = x.toInt(); LauncherPreferences.PREF_POINTER_HOTSPOT_Y_HRESIZE = y.toInt() }
                            5 -> { LauncherPreferences.PREF_POINTER_HOTSPOT_X_VRESIZE = x.toInt(); LauncherPreferences.PREF_POINTER_HOTSPOT_Y_VRESIZE = y.toInt() }
                            6 -> { LauncherPreferences.PREF_POINTER_HOTSPOT_X_ALL_RESIZE = x.toInt(); LauncherPreferences.PREF_POINTER_HOTSPOT_Y_ALL_RESIZE = y.toInt() }
                            7 -> { LauncherPreferences.PREF_POINTER_HOTSPOT_X_NOT_ALLOWED = x.toInt(); LauncherPreferences.PREF_POINTER_HOTSPOT_Y_NOT_ALLOWED = y.toInt() }
                        }
                        pointerHotspots[shape] = x to y
                        editingCursorShape = -1
                    }
                },
                onDismiss = {
                    showHotspotDialog = false
                    editingCursorShape = -1
                }
            )
        }
    }

    SideDialogUtils.activeDialog?.Content()
}