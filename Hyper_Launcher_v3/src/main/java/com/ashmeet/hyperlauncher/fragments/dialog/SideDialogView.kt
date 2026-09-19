package com.ashmeet.hyperlauncher.fragments.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ashmeet.hyperlauncher.components.dialog.SideDialog

import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import kotlin.time.Duration.Companion.milliseconds


abstract class SideDialogView {

    var isDisplaying by mutableStateOf(false)
    var isAtRight by mutableStateOf(false)
    var dialogWidth by mutableStateOf(300.dp)
    var verticalPadding by mutableStateOf(if (LauncherPreferences.PREF_FULLSCREEN_LAUNCHER) 4.dp else 32.dp)

    protected var mTitleRes: Int = 0
    protected var mStartButtonRes: Int = 0
    protected var mEndButtonRes: Int = 0

    protected var mStartButtonListener: (() -> Unit)? = null
    protected var mEndButtonListener: (() -> Unit)? = null

    fun setTitle(textId: Int) {
        mTitleRes = textId
    }

    fun setStartButtonListener(textId: Int, listener: (() -> Unit)?) {
        mStartButtonRes = textId
        mStartButtonListener = listener
    }

    fun setEndButtonListener(textId: Int, listener: (() -> Unit)?) {
        mEndButtonRes = textId
        mEndButtonListener = listener
    }


    open fun appear(fromRight: Boolean) {
        isAtRight = fromRight
        isDisplaying = true
        onAppear()
    }


    open fun disappear(destroy: Boolean) {
        isDisplaying = false
        onDisappear()
        if (destroy) onDestroy()
    }


    @Composable
    fun Content() {
        var visible by remember { mutableStateOf(false) }

        androidx.compose.runtime.LaunchedEffect(isDisplaying) {
            if (isDisplaying) {
                kotlinx.coroutines.delay(50.milliseconds)
                visible = true
            } else {
                visible = false
            }
        }

        SideDialog(
            visible = visible,
            onDismissRequest = { disappear(false) },
            fromRight = isAtRight,
            width = dialogWidth,
            verticalPadding = verticalPadding,
            startText = if (mStartButtonRes != 0) stringResource(mStartButtonRes) else null,
            onStartClick = mStartButtonListener,
            endText = if (mEndButtonRes != 0) stringResource(mEndButtonRes) else null,
            onEndClick = mEndButtonListener,
            header = { DialogHeader() }
        ) {
            DialogContent()
        }
    }

    @Composable
    abstract fun DialogContent()

    @Composable
    open fun DialogHeader() {}

    protected open fun onAppear() {}
    protected open fun onDisappear() {}
    protected open fun onDestroy() {}
}
