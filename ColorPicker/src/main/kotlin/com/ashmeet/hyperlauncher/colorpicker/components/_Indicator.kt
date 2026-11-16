package com.ashmeet.hyperlauncher.colorpicker.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


internal fun DrawScope.drawVerticalIndicator(
    currentColor: Color,
    xPos: Float,
    height: Float,
    width: Dp = 2.dp,
    overshoot: Dp = 2.dp
) {

    val invertedColor = invertColor(currentColor)
    val overshootPx = overshoot.toPx()

    drawLine(
        color = invertedColor,
        start = Offset(xPos, -overshootPx),
        end = Offset(xPos, height + overshootPx),
        strokeWidth = width.toPx()
    )
}


internal fun DrawScope.drawHorizontalIndicator(
    currentColor: Color,
    yPos: Float,
    width: Float,
    height: Dp = 2.dp,
    overshoot: Dp = 2.dp
) {
    val invertedColor = invertColor(currentColor)
    val overshootPx = overshoot.toPx()

    drawLine(
        color = invertedColor,
        start = Offset(-overshootPx, yPos),
        end = Offset(width + overshootPx, yPos),
        strokeWidth = height.toPx()
    )
}


internal fun DrawScope.drawCenterIndicator(
    currentColor: Color,
    pressOffset: Offset,
    radius: Dp = 6.dp,
    width: Dp = 2.dp
) {

    val invertedColor = invertColor(currentColor)

    drawCircle(
        color = invertedColor,
        radius = radius.toPx(),
        center = pressOffset,
        style = Stroke(width = width.toPx())
    )
}


private fun invertColor(color: Color): Color {
    val r = 255 - (color.red * 255).toInt()
    val g = 255 - (color.green * 255).toInt()
    val b = 255 - (color.blue * 255).toInt()
    return Color(r, g, b, 255)
}