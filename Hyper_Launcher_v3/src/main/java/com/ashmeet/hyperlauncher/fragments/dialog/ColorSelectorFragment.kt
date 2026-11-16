package com.ashmeet.hyperlauncher.fragments.dialog

import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ashmeet.hyperlauncher.components.color.ColorSelectionListener
import com.ashmeet.hyperlauncher.components.color.ColorSelectorContent


class ColorSelectorFragment(private var colorSelectionListener: ColorSelectionListener?) :
    SideDialogView() {

    private var selectedColor by mutableStateOf(Color.RED)
    private var initialColor by mutableIntStateOf(Color.RED)

    private var mAlphaEnabled by mutableStateOf(true)
    private var mLockSatVal by mutableStateOf(false)

    init {
        setStartButtonListener(android.R.string.cancel) {
            colorSelectionListener?.onColorSelected(initialColor)
            disappear(true)
        }
        setEndButtonListener(android.R.string.ok) {
            disappear(true)
        }
    }

    @Composable
    override fun DialogContent() {
        ColorSelectorContent(
            initialColor = selectedColor,
            alphaEnabled = mAlphaEnabled,
            lockSatVal = mLockSatVal,
            onColorChanged = { color ->
                selectedColor = color
                colorSelectionListener?.onColorSelected(color)
            },
            onClose = { disappear(true) }
        )
    }

    fun show(fromRight: Boolean, previousColor: Int = Color.RED) {
        initialColor = previousColor
        selectedColor = previousColor
        appear(fromRight)
    }

    fun setAlphaEnabled(enabled: Boolean) {
        mAlphaEnabled = enabled
    }

    fun setLockSatVal(locked: Boolean) {
        mLockSatVal = locked
    }

}
