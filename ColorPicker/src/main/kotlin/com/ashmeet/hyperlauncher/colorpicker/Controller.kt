package com.ashmeet.hyperlauncher.colorpicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import android.graphics.Color as NativeColor


@Composable
fun rememberColorPickerController(
    initialColor: Color
): ColorPickerController {
    val hsv = FloatArray(3)
    NativeColor.colorToHSV(initialColor.toArgb(), hsv)

    return rememberColorPickerController(
        initialHue = hsv[0],
        initialSaturation = hsv[1],
        initialValue = hsv[2],
        initialAlpha = initialColor.alpha
    )
}


@Composable
fun rememberColorPickerController(
    initialHue: Float = 0f,
    initialSaturation: Float = 1f,
    initialValue: Float = 1f,
    initialAlpha: Float = 1f
): ColorPickerController = remember {
    ColorPickerController(
        initialHue = initialHue,
        initialSaturation = initialSaturation,
        initialValue = initialValue,
        initialAlpha = initialAlpha,
    )
}


class ColorPickerController internal constructor(
    val initialHue: Float,
    val initialSaturation: Float,
    val initialValue: Float,
    val initialAlpha: Float
) {
    private val _hue = mutableFloatStateOf(initialHue)
    private val _saturation = mutableFloatStateOf(initialSaturation)
    private val _value = mutableFloatStateOf(initialValue)
    private val _alpha = mutableFloatStateOf(initialAlpha)

    val hue: Float get() = _hue.floatValue
    val saturation: Float get() = _saturation.floatValue
    val value: Float get() = _value.floatValue
    val alpha: Float get() = _alpha.floatValue


    fun getOriginalColor(): Color =
        Color.hsv(initialHue, initialSaturation, initialValue, initialAlpha)


    val color: State<Color> = derivedStateOf {
        Color.hsv(_hue.floatValue, _saturation.floatValue, _value.floatValue, _alpha.floatValue)
    }

    fun setHue(hue: Float) {
        _hue.floatValue = hue.coerceIn(0f, 360f)
    }

    fun setSaturation(sat: Float) {
        _saturation.floatValue = sat.coerceIn(0f, 1f)
    }

    fun setValue(value: Float) {
        _value.floatValue = value.coerceIn(0f, 1f)
    }

    fun setAlpha(alpha: Float) {
        _alpha.floatValue = alpha.coerceIn(0f, 1f)
    }

    fun setColor(color: Color) {
        val hsv = FloatArray(3)
        NativeColor.colorToHSV(color.toArgb(), hsv)

        _hue.floatValue = hsv[0]
        _saturation.floatValue = hsv[1]
        _value.floatValue = hsv[2]
        _alpha.floatValue = color.alpha
    }
}