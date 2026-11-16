package com.ashmeet.hyperlauncher.filemanager.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.widget.ProgressBar
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import net.ashmeet.hyperlauncher.R

class TextProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.progressBarStyleHorizontal
) : ProgressBar(context, attrs, defStyleAttr) {

    private var mTextPadding = 0
    private lateinit var mTextPaint: Paint
    private var mText = ""

    init {
        initView()
    }

    private fun initView() {
        progressDrawable = ResourcesCompat.getDrawable(resources, R.drawable.view_text_progressbar, null)
        progress = 35
        mTextPaint = Paint().apply {
            color = Color.WHITE
            flags = Paint.FAKE_BOLD_TEXT_FLAG
            isAntiAlias = true
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mTextPaint.textSize = ((height - paddingBottom - paddingTop) * 0.55).toFloat()
        val textWidth = mTextPaint.measureText(mText)
        val maxProgress = if (max > 0) max.toFloat() else 100f
        val xPos = Math.max(
            Math.min(
                ((progress * width) / maxProgress) + mTextPadding,
                width - textWidth - mTextPadding
            ),
            mTextPadding.toFloat()
        ).toInt()
        val yPos = ((height / 2) - ((mTextPaint.descent() + mTextPaint.ascent()) / 2)).toInt()

        canvas.drawText(mText, xPos.toFloat(), yPos.toFloat(), mTextPaint)
    }

    fun setText(@StringRes resid: Int) {
        setText(context.resources.getText(resid).toString())
    }

    fun setText(text: String) {
        mText = text
        invalidate()
    }
}
