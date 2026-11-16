package com.ashmeet.hyperlauncher.colorpicker.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


@Composable
fun TransparentChecker(
    modifier: Modifier = Modifier,
    gridSize: Float = 20f,
) {
    Canvas(modifier) {
        transparentCheckerBackground(
            width = size.width,
            height = size.height,
            gridSize = gridSize
        )
    }
}