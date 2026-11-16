package com.ashmeet.hyperlauncher.utils.drawable

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Insets
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.annotation.RequiresApi

@RequiresApi(29)
class InsetBackground(private val insets: Insets, bgColor: Int) : Drawable() {
    private val leftRect = Rect()
    private val topRect = Rect()
    private val rightRect = Rect()
    private val bottomRect = Rect()
    private val rectPaint = Paint().apply {
        color = bgColor
    }

    init {
        Log.i("InsetBackground", insets.toString())
    }

    private fun computeRects(width: Int, height: Int) {
        leftRect.set(0, 0, insets.left, height)

        topRect.set(insets.left, 0, width - insets.right, insets.top)

        rightRect.set(width - insets.right, 0, width, height)

        bottomRect.set(0, height - insets.bottom, width, height)
    }

    override fun onBoundsChange(bounds: Rect) {
        computeRects(bounds.width(), bounds.height())
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(leftRect, rectPaint)
        canvas.drawRect(rightRect, rectPaint)
        canvas.drawRect(topRect, rectPaint)
        canvas.drawRect(bottomRect, rectPaint)
    }

    override fun setAlpha(alpha: Int) {}

    override fun setColorFilter(colorFilter: ColorFilter?) {}

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSPARENT
}
