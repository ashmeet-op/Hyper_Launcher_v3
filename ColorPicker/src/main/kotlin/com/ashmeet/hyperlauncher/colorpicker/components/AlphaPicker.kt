package com.ashmeet.hyperlauncher.colorpicker.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.ashmeet.hyperlauncher.colorpicker.ColorPickerController
import com.ashmeet.hyperlauncher.colorpicker.getGradientColorAtPosition


@Composable
fun AlphaBarPicker(
    controller: ColorPickerController,
    modifier: Modifier = Modifier,
    onChangeFinished: () -> Unit = {}
) {
    var pressOffset by remember { mutableStateOf(Offset.Zero) }
    var widthPx by remember { mutableFloatStateOf(0f) }
    var heightPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(controller.alpha, widthPx) {
        if (widthPx > 0f) {
            pressOffset = Offset(alphaToX(controller.alpha, widthPx), heightPx / 2f)
        }
    }


    val currentColor by controller.color


    val colors by remember(currentColor) {
        derivedStateOf {
            listOf(
                currentColor.copy(alpha = 0f),
                currentColor.copy(alpha = 1f)
            )
        }
    }

    Canvas(
        modifier = modifier
            .progressBar(
                widthPx = widthPx,
                heightPx = heightPx,
                onOffsetChanged = { offset ->
                    pressOffset = offset
                    controller.setAlpha(xToAlpha(offset.x, widthPx))
                },
                onSizeChanged = { size ->
                    widthPx = size.width.toFloat()
                    heightPx = size.height.toFloat()
                },
                onChangeFinished = onChangeFinished
            )
    ) {
        transparentCheckerBackground(
            width = size.width,
            height = size.height
        )

        drawRect(
            brush = Brush.horizontalGradient(colors = colors),
            size = size
        )

        val indicatorColor = colors.getGradientColorAtPosition(
            x = pressOffset.x,
            widthPx = widthPx
        )

        drawVerticalIndicator(
            currentColor = indicatorColor,
            xPos = pressOffset.x,
            height = size.height
        )
    }
}

private fun alphaToX(alpha: Float, widthPx: Float) =
    (alpha * widthPx).coerceIn(0f, widthPx)

private fun xToAlpha(x: Float, widthPx: Float) =
    (x / widthPx).coerceIn(0f, 1f)


@Composable
fun VerticalAlphaBarPicker(
    controller: ColorPickerController,
    modifier: Modifier = Modifier,
    onChangeFinished: () -> Unit = {}
) {
    var pressOffset by remember { mutableStateOf(Offset.Zero) }
    var widthPx by remember { mutableFloatStateOf(0f) }
    var heightPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(controller.alpha, heightPx) {
        if (heightPx > 0f) {
            pressOffset = Offset(widthPx / 2f, alphaToY(controller.alpha, heightPx))
        }
    }

    val currentColor by controller.color

    val colors by remember(currentColor) {
        derivedStateOf {
            listOf(
                currentColor.copy(alpha = 0f),
                currentColor.copy(alpha = 1f)
            )
        }
    }

    Canvas(
        modifier = modifier
            .verticalProgressBar(
                widthPx = widthPx,
                heightPx = heightPx,
                onOffsetChanged = { offset ->
                    pressOffset = offset
                    controller.setAlpha(yToAlpha(offset.y, heightPx))
                },
                onSizeChanged = { size ->
                    widthPx = size.width.toFloat()
                    heightPx = size.height.toFloat()
                },
                onChangeFinished = onChangeFinished
            )
    ) {
        transparentCheckerBackground(
            width = size.width,
            height = size.height
        )

        drawRect(
            brush = Brush.verticalGradient(colors = colors),
            size = size
        )

        val indicatorColor = colors.getGradientColorAtPosition(
            x = pressOffset.y,
            widthPx = heightPx
        )

        drawHorizontalIndicator(
            currentColor = indicatorColor,
            yPos = pressOffset.y,
            width = size.width
        )
    }
}

private fun alphaToY(alpha: Float, heightPx: Float) =
    (alpha * heightPx).coerceIn(0f, heightPx)

private fun yToAlpha(y: Float, heightPx: Float) =
    (y / heightPx).coerceIn(0f, 1f)