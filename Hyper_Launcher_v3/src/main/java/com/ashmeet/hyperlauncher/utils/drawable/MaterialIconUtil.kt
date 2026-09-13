package com.ashmeet.hyperlauncher.utils.drawable

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector

object MaterialIconUtil {
    const val ICON_SETTINGS = 1
    const val ICON_HOME = 2
    const val ICON_SLIDERS = 3

    @JvmStatic
    fun getHandleDrawable(context: Context): Drawable {
        return ResizeHandleDrawable(context)
    }

    @JvmStatic
    fun getFileDrawable(context: Context): Drawable {
        return FileIconDrawable(context)
    }

    @JvmStatic
    fun getFolderDrawable(context: Context): Drawable {
        return FolderIconDrawable(context)
    }

    private class FileIconDrawable(context: Context) : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            val typedArray = context.obtainStyledAttributes(intArrayOf(android.R.attr.colorControlActivated))
            color = typedArray.getColor(0, 0xFF888888.toInt())
            typedArray.recycle()
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        override fun draw(canvas: Canvas) {
            val b = bounds
            val w = b.width().toFloat()
            val h = b.height().toFloat()
            canvas.drawRect(b.left + w * 0.2f, b.top + h * 0.1f, b.right - w * 0.2f, b.bottom - h * 0.1f, paint)
        }
        override fun setAlpha(alpha: Int) { paint.alpha = alpha }
        override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) { paint.colorFilter = colorFilter }
        @Deprecated("Deprecated in Java") override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        override fun getIntrinsicWidth(): Int = 48
        override fun getIntrinsicHeight(): Int = 48
    }

    private class FolderIconDrawable(context: Context) : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            val typedArray = context.obtainStyledAttributes(intArrayOf(android.R.attr.colorControlActivated))
            color = typedArray.getColor(0, 0xFF42A5F5.toInt())
            typedArray.recycle()
            style = Paint.Style.FILL
        }
        override fun draw(canvas: Canvas) {
            val b = bounds
            val w = b.width().toFloat()
            val h = b.height().toFloat()
            val path = Path().apply {
                moveTo(b.left + w * 0.1f, b.top + h * 0.2f)
                lineTo(b.left + w * 0.4f, b.top + h * 0.2f)
                lineTo(b.left + w * 0.5f, b.top + h * 0.35f)
                lineTo(b.right - w * 0.1f, b.top + h * 0.35f)
                lineTo(b.right - w * 0.1f, b.bottom - h * 0.2f)
                lineTo(b.left + w * 0.1f, b.bottom - h * 0.2f)
                close()
            }
            canvas.drawPath(path, paint)
        }
        override fun setAlpha(alpha: Int) { paint.alpha = alpha }
        override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) { paint.colorFilter = colorFilter }
        @Deprecated("Deprecated in Java") override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        override fun getIntrinsicWidth(): Int = 48
        override fun getIntrinsicHeight(): Int = 48
    }

    fun getIconVector(iconId: Int): ImageVector {
        return when (iconId) {
            ICON_SETTINGS -> Icons.Default.Settings
            ICON_HOME -> Icons.Default.Home
            ICON_SLIDERS -> Icons.Default.Tune
            else -> Icons.Default.Settings
        }
    }

    private class ResizeHandleDrawable(context: Context) : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            val typedArray = context.obtainStyledAttributes(intArrayOf(android.R.attr.colorControlActivated))
            color = typedArray.getColor(0, 0xFF00FF00.toInt())
            typedArray.recycle()
            style = Paint.Style.FILL
        }
        private val path = Path()

        override fun draw(canvas: Canvas) {
            val b = bounds
            path.reset()
            path.moveTo(b.right.toFloat(), b.top.toFloat())
            path.lineTo(b.right.toFloat(), b.bottom.toFloat())
            path.lineTo(b.left.toFloat(), b.bottom.toFloat())
            path.close()
            canvas.drawPath(path, paint)
        }

        override fun setAlpha(alpha: Int) {
            paint.alpha = alpha
        }

        override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
            paint.colorFilter = colorFilter
        }

        @Deprecated("Deprecated in Java", ReplaceWith("PixelFormat.TRANSLUCENT"))
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }
}
