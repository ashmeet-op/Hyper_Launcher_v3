package com.ashmeet.hyperlauncher.fragments.dialog

import com.ashmeet.hyperlauncher.utils.translation.translatedText

import android.graphics.Bitmap
import android.graphics.Color
import android.view.View
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ashmeet.hyperlauncher.components.color.ColorSelectorContent
import com.ashmeet.hyperlauncher.components.dialog.CardPosition
import com.ashmeet.hyperlauncher.components.dialog.DialogActionItem
import com.ashmeet.hyperlauncher.components.dialog.DialogCard
import com.ashmeet.hyperlauncher.components.dialog.DialogSliderItem
import com.ashmeet.hyperlauncher.components.dialog.DialogSwitchItem
import com.ashmeet.hyperlauncher.components.dialog.DialogTextInput
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperlauncher.activity.CustomControlsActivity
import com.ashmeet.hyperlauncher.utils.Tools
import net.kdt.pojavlaunch.customcontrols.ControlData
import net.kdt.pojavlaunch.customcontrols.ControlJoystickData
import net.kdt.pojavlaunch.customcontrols.buttons.ControlDrawer
import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface
import net.kdt.pojavlaunch.utils.CropperUtils
import net.kdt.pojavlaunch.utils.KeycodeUtils
import kotlin.math.abs

class EditControlSideDialog : SideDialogView() {

    private var mCurrentlyEditedButton by mutableStateOf<ControlInterface?>(null)
    private var colorSelectorColor by mutableIntStateOf(Color.WHITE)
    private var isColorSelectorVisible by mutableStateOf(false)
    private var isAlphaEnabled by mutableStateOf(true)
    private var onColorSelected: ((Int) -> Unit)? = null

    private var selectedTab by mutableIntStateOf(0)

    init {
        setTitle(R.string.mcl_option_customcontrol)
        dialogWidth = 280.dp
    }

    @Composable
    override fun DialogHeader() {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = ComposeColor.Transparent,
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
                icon = { Icon(Icons.Rounded.Settings, null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                icon = { Icon(Icons.Rounded.Gamepad, null) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                icon = { Icon(Icons.Rounded.Palette, null) }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                icon = { Icon(Icons.Rounded.Visibility, null) }
            )
        }
    }

    @Composable
    override fun DialogContent() {
        Surface(
            color = ComposeColor.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            if (isColorSelectorVisible) {
                ColorSelectorContent(
                    initialColor = colorSelectorColor,
                    alphaEnabled = isAlphaEnabled,
                    onColorChanged = {
                        colorSelectorColor = it
                        onColorSelected?.invoke(it)
                    },
                    onClose = { isColorSelectorVisible = false }
                )
            } else {
                mCurrentlyEditedButton?.let { button ->
                    EditControlContent(
                        button = button,
                        selectedTab = selectedTab,
                        onShowColorPicker = { color, alpha, onSelected ->
                            colorSelectorColor = color
                            isAlphaEnabled = alpha
                            onColorSelected = onSelected
                            isColorSelectorVisible = true
                        },
                        onClose = { disappear(true) }
                    )
                }
            }
        }
    }

    fun setCurrentlyEditedButton(button: ControlInterface) {
        mCurrentlyEditedButton = button
        isColorSelectorVisible = false
    }

    fun adaptPanelPosition() {
        val button = mCurrentlyEditedButton ?: return
        val controlView = button.controlView
        val parentView = button.controlLayoutParent ?: return

        val isAtRight = (controlView.x + controlView.width / 2f) < (parentView.width / 2f)
        appear(isAtRight)
    }

    fun disappearLayer(): Boolean {
        return if (isColorSelectorVisible) {
            isColorSelectorVisible = false
            false
        } else {
            disappear(false)
            true
        }
    }
}

@Composable
private fun EditControlContent(
    button: ControlInterface,
    selectedTab: Int,
    onShowColorPicker: (Int, Boolean, (Int) -> Unit) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val properties = button.properties


    var name by remember(properties) { mutableStateOf(properties.name) }
    var widthText by remember(properties) { mutableStateOf(if (properties.width % 1f == 0f) properties.width.toInt().toString() else properties.width.toString()) }
    var heightText by remember(properties) { mutableStateOf(if (properties.height % 1f == 0f) properties.height.toInt().toString() else properties.height.toString()) }
    var opacity by remember(properties) { mutableFloatStateOf(properties.opacity * 100f) }
    var strokeWidth by remember(properties) { mutableFloatStateOf(properties.strokeWidth * 10f) }
    var cornerRadius by remember(properties) { mutableFloatStateOf(properties.cornerRadius) }

    var isToggle by remember(properties) { mutableStateOf(properties.isToggle) }
    var passThruEnabled by remember(properties) { mutableStateOf(properties.passThruEnabled) }
    var isSwipeable by remember(properties) { mutableStateOf(properties.isSwipeable) }

    var displayInGame by remember(properties) { mutableStateOf(properties.displayInGame) }
    var displayInMenu by remember(properties) { mutableStateOf(properties.displayInMenu) }

    var showNameDialog by remember { mutableStateOf(false) }
    var showWidthDialog by remember { mutableStateOf(false) }
    var showHeightDialog by remember { mutableStateOf(false) }

    val isJoystick = properties is ControlJoystickData
    val isDrawer = button is ControlDrawer
    val isSubButton = !isDrawer && button.controlView.parent is ControlDrawer

    val view = button.controlView
    DisposableEffect(view, properties) {
        val listener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val w = properties.width
            val h = properties.height
            val currentW = widthText.toFloatOrNull() ?: -1f
            if (abs(w - currentW) > 0.5f) {
                widthText = if (w % 1f == 0f) w.toInt().toString() else w.toString()
            }
            val currentH = heightText.toFloatOrNull() ?: -1f
            if (abs(h - currentH) > 0.5f) {
                heightText = if (h % 1f == 0f) h.toInt().toString() else h.toString()
            }
        }
        view.addOnLayoutChangeListener(listener)
        onDispose {
            view.removeOnLayoutChangeListener(listener)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        var cardIndex = 0

        DialogCard(useSurface = true, delayIndex = cardIndex++) {
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
            }
        ) { targetTab ->
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (targetTab) {
                    0 -> {

                        if (!isJoystick) {
                            DialogCard(useSurface = true, delayIndex = cardIndex++) {
                                DialogActionItem(
                                    title = translatedText(stringResource(R.string.global_name)),
                                    summary = name ?: "",
                                    onClick = { showNameDialog = true }
                                )
                            }

                            if (showNameDialog) {
                                DialogTextInput(
                                    title = translatedText(stringResource(R.string.global_name)),
                                    initialValue = name ?: "",
                                    onConfirm = {
                                        name = it
                                        properties.name = it
                                        button.updateProperties()
                                        showNameDialog = false
                                    },
                                    onDismiss = { showNameDialog = false }
                                )
                            }
                        }


                        if (!isSubButton) {
                            DialogCard(useSurface = true, delayIndex = cardIndex++) {
                                DialogActionItem(
                                    title = translatedText(stringResource(R.string.customctrl_size)) + " X",
                                    summary = widthText,
                                    onClick = { showWidthDialog = true }
                                )
                            }

                            if (showWidthDialog) {
                                DialogTextInput(
                                    title = translatedText(stringResource(R.string.customctrl_size)) + " X",
                                    initialValue = widthText,
                                    onConfirm = {
                                        widthText = it
                                        it.toFloatOrNull()?.let { v ->
                                            properties.width = v
                                            if (isJoystick) properties.height = v
                                            button.updateProperties()
                                        }
                                        showWidthDialog = false
                                    },
                                    onDismiss = { showWidthDialog = false }
                                )
                            }

                            if (!isJoystick) {
                                DialogCard(useSurface = true, delayIndex = cardIndex++) {
                                    DialogActionItem(
                                        title = translatedText(stringResource(R.string.customctrl_size)) + " Y",
                                        summary = heightText,
                                        onClick = { showHeightDialog = true }
                                    )
                                }

                                if (showHeightDialog) {
                                    DialogTextInput(
                                        title = translatedText(stringResource(R.string.customctrl_size)) + " Y",
                                        initialValue = heightText,
                                        onConfirm = {
                                            heightText = it
                                            it.toFloatOrNull()?.let { v ->
                                                properties.height = v
                                                button.updateProperties()
                                            }
                                            showHeightDialog = false
                                        },
                                        onDismiss = { showHeightDialog = false }
                                    )
                                }
                            }
                        }
                    }
                    1 -> {

                        if (!isJoystick && !isDrawer) {
                            val specialArray = remember { ControlData.buildSpecialButtonArray() }
                            val keyNames = remember { KeycodeUtils.generateKeyName() }
                            val allKeyNames = remember { specialArray + keyNames }

                            properties.keycodes.forEachIndexed { index, keycode ->
                                var expanded by remember { mutableStateOf(false) }
                                val selectedIndex = if (keycode < 0) {
                                    keycode + specialArray.size
                                } else {
                                    KeycodeUtils.getIndexByValue(keycode) + specialArray.size
                                }

                                DialogCard(useSurface = true, delayIndex = cardIndex++) {
                                    Box {
                                        DialogActionItem(
                                            title = "Key ${index + 1}",
                                            summary = allKeyNames.getOrElse(selectedIndex) { "" },
                                            onClick = { expanded = true }
                                        )
                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false }) {
                                            allKeyNames.forEachIndexed { i, keyName ->
                                                DropdownMenuItem(
                                                    text = { Text(keyName) },
                                                    onClick = {
                                                        val newKeycode =
                                                            if (i < specialArray.size) {
                                                                i - specialArray.size
                                                            } else {
                                                                KeycodeUtils.getValueByIndex(i - specialArray.size)
                                                            }
                                                        properties.keycodes[index] = newKeycode
                                                        button.updateProperties()
                                                        expanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }


                        if (!isJoystick && !isDrawer) {
                            DialogCard(
                                position = CardPosition.SINGLE,
                                useSurface = true,
                                delayIndex = cardIndex++
                            ) {
                                DialogSwitchItem(
                                    title = translatedText(stringResource(R.string.customctrl_toggle)),
                                    checked = isToggle,
                                    onCheckedChange = {
                                        isToggle = it
                                        properties.isToggle = it
                                    }
                                )
                            }
                            DialogCard(
                                position = CardPosition.SINGLE,
                                useSurface = true,
                                delayIndex = cardIndex++
                            ) {
                                DialogSwitchItem(
                                    title = translatedText(stringResource(R.string.customctrl_passthru)),
                                    checked = passThruEnabled,
                                    onCheckedChange = {
                                        passThruEnabled = it
                                        properties.passThruEnabled = it
                                    }
                                )
                            }
                            DialogCard(
                                position = CardPosition.SINGLE,
                                useSurface = true,
                                delayIndex = cardIndex++
                            ) {
                                DialogSwitchItem(
                                    title = translatedText(stringResource(R.string.customctrl_swipeable)),
                                    checked = isSwipeable,
                                    onCheckedChange = {
                                        isSwipeable = it
                                        properties.isSwipeable = it
                                    }
                                )
                            }
                        }

                        if (isJoystick) {
                            var forwardLock by remember(properties) { mutableStateOf(properties.forwardLock) }
                            var absolute by remember(properties) { mutableStateOf(properties.absolute) }

                            DialogCard(
                                position = CardPosition.SINGLE,
                                useSurface = true,
                                delayIndex = cardIndex++
                            ) {
                                DialogSwitchItem(
                                    title = translatedText(stringResource(R.string.customctrl_forward_lock)),
                                    checked = forwardLock,
                                    onCheckedChange = {
                                        forwardLock = it
                                        properties.forwardLock = it
                                    }
                                )
                            }
                            DialogCard(
                                position = CardPosition.SINGLE,
                                useSurface = true,
                                delayIndex = cardIndex++
                            ) {
                                DialogSwitchItem(
                                    title = translatedText(stringResource(R.string.customctrl_absolute_tracking)),
                                    checked = absolute,
                                    onCheckedChange = {
                                        absolute = it
                                        properties.absolute = it
                                    }
                                )
                            }
                        }
                    }
                    2 -> {

                        if (context is CustomControlsActivity && !isJoystick) {
                            DialogCard(useSurface = true, delayIndex = cardIndex++) {
                                DialogActionItem(
                                    title = translatedText(stringResource(R.string.customctrl_background_bitmap)),
                                    onClick = {
                                        val receiver = object : CropperUtils.CropperReceiver {
                                            override fun getAspectRatio() =
                                                button.controlView.width.toFloat() / button.controlView.height

                                            override fun getTargetMaxSide() = maxOf(
                                                button.controlView.width,
                                                button.controlView.height
                                            )

                                            override fun onCropped(contentBitmap: Bitmap) {
                                                val storage = button.controlLayoutParent.bitmaps
                                                properties.bitmapTag = storage.putBitmap(
                                                    contentBitmap,
                                                    properties.bitmapTag
                                                )
                                                button.setBackground()
                                            }

                                            override fun onFailed(e: Exception) =
                                                Tools.showError(context, e)
                                        }
                                        context.startCropping(receiver)
                                    }
                                )
                            }
                        }

                        DialogCard(useSurface = true, delayIndex = cardIndex++) {
                            DialogActionItem(
                                title = translatedText(stringResource(R.string.customctrl_background_color)),
                                summary = if (properties.bitmapTag != null) translatedText(
                                    stringResource(R.string.customctrl_background_color_warning)
                                ) else null,
                                onClick = {
                                    onShowColorPicker(properties.bgColor, true) {
                                        properties.bitmapTag = null
                                        properties.bgColor = it
                                        button.setBackground()
                                    }
                                }
                            )
                        }

                        if (properties.bitmapTag == null) {
                            DialogCard(useSurface = true, delayIndex = cardIndex++) {
                                DialogActionItem(
                                    title = translatedText(stringResource(R.string.customctrl_stroke_color)),
                                    onClick = {
                                        onShowColorPicker(properties.strokeColor, false) {
                                            properties.strokeColor = it
                                            button.setBackground()
                                        }
                                    }
                                )
                            }
                            DialogCard(useSurface = true, delayIndex = cardIndex++) {
                                DialogSliderItem(
                                    title = translatedText(stringResource(R.string.customctrl_stroke_width)),
                                    value = strokeWidth,
                                    valueRange = 0f..100f,
                                    onValueChange = {
                                        strokeWidth = it
                                        properties.strokeWidth = it / 10f
                                        button.setBackground()
                                    }
                                )
                            }
                            DialogCard(useSurface = true, delayIndex = cardIndex++) {
                                DialogSliderItem(
                                    title = translatedText(stringResource(R.string.customctrl_corner_radius)),
                                    value = cornerRadius,
                                    valueRange = 0f..100f,
                                    onValueChange = {
                                        cornerRadius = it
                                        properties.cornerRadius = it
                                        button.setBackground()
                                    }
                                )
                            }
                        }

                        DialogCard(useSurface = true, delayIndex = cardIndex++) {
                            DialogSliderItem(
                                title = translatedText(stringResource(R.string.customctrl_button_opacity)),
                                value = opacity,
                                valueRange = 0f..100f,
                                onValueChange = {
                                    opacity = it
                                    properties.opacity = it / 100f
                                    button.controlView.alpha = it / 100f
                                }
                            )
                        }
                    }
                    3 -> {

                        if (!isSubButton) {
                            DialogCard(
                                position = CardPosition.SINGLE,
                                useSurface = true,
                                delayIndex = cardIndex++
                            ) {
                                DialogSwitchItem(
                                    title = translatedText(stringResource(R.string.customctrl_visibility_ingame)),
                                    checked = displayInGame,
                                    onCheckedChange = {
                                        displayInGame = it
                                        properties.displayInGame = it
                                    }
                                )
                            }
                            DialogCard(
                                position = CardPosition.SINGLE,
                                useSurface = true,
                                delayIndex = cardIndex++
                            ) {
                                DialogSwitchItem(
                                    title = translatedText(stringResource(R.string.customctrl_visibility_in_menus)),
                                    checked = displayInMenu,
                                    onCheckedChange = {
                                        displayInMenu = it
                                        properties.displayInMenu = it
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
