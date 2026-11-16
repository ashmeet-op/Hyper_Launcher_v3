package com.ashmeet.hyperlauncher.components.slider

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndicatorSlider(
    modifier: Modifier = Modifier,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    steps: Int = 0,
    colors: SliderColors = SliderDefaults.colors(),
) {
    val state = rememberSliderState(
        value = value,
        trackRange = valueRange,
        steps = steps,
    )

    LaunchedEffect(value) {
        if (state.value != value) {
            state.value = value
        }
    }

    val density = LocalDensity.current
    val sliderTopCut = with(density) { 8.dp.toPx().toInt() }
    val sliderBottomCut = with(density) { 6.dp.toPx().toInt() }
    Layout(
        modifier = modifier,
        content = {
            Slider(
                state = state,
                onValueChange = onValueChange,
                enabled = enabled,
                onValueChangeFinished = onValueChangeFinished,
                interactionSource = interactionSource,
                colors = colors,
                thumb = {
                    SliderDefaults.Thumb(
                        interactionSource = interactionSource,
                        colors = colors,
                        enabled = enabled,
                        thumbSize = DpSize(6.0.dp, 20.0.dp),
                    )
                },
            )
        },
    ) { measurables, constraints ->
        val placeable = measurables.first().measure(constraints)
        val newHeight = (placeable.height - sliderTopCut - sliderBottomCut).coerceAtLeast(0)
        layout(placeable.width, newHeight) {
            placeable.place(0, -sliderTopCut)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleTextSlider(
    modifier: Modifier = Modifier,
    shorter: Boolean = false,
    value: Float,
    decimalFormat: String = "#0.00",
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit,
    toInt: Boolean = false,
    suffix: String? = null,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    onTextClick: (() -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    appendContent: @Composable () -> Unit = {},
) {
    val formatter = DecimalFormat(decimalFormat)
    fun getTextString(v: Float) = if (toInt) v.toInt().toString() else formatter.format(v)

    fun changeValue(newValue: Float, finished: Boolean) {
        onValueChange(newValue)
        if (finished) onValueChangeFinished?.invoke()
    }

    LaunchedEffect(Unit) {
        if (value !in valueRange) {
            val newValue = value.coerceIn(valueRange)
            changeValue(newValue, finished = true)
        }
    }

    val state = rememberSliderState(
        value = value,
        trackRange = valueRange,
        steps = steps,
    )

    LaunchedEffect(value) {
        if (state.value != value) {
            state.value = value
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val sliderColors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )

        if (shorter) {
            IndicatorSlider(
                value = value,
                enabled = enabled,
                onValueChange = { changeValue(it, finished = false) },
                onValueChangeFinished = onValueChangeFinished,
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier.weight(1f),
                colors = sliderColors,
            )
        } else {
            Slider(
                state = state,
                onValueChange = { changeValue(it, finished = false) },
                enabled = enabled,
                onValueChangeFinished = onValueChangeFinished,
                modifier = Modifier.weight(1f),
                colors = sliderColors,
            )
        }
        Surface(
            modifier = Modifier
                .alpha(alpha = if (enabled) 1f else 0.5f)
                .padding(start = 12.dp)
                .align(Alignment.CenterVertically),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Row(
                modifier = Modifier.padding(PaddingValues(horizontal = 10.dp, vertical = 6.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .then(
                            if (onTextClick != null) {
                                Modifier.clickable(enabled = enabled, onClick = onTextClick)
                            } else Modifier
                        )
                ) {
                    Text(
                        text = getTextString(value),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    suffix?.let { text ->
                        Text(text = text, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        appendContent()
    }
}
