package com.ashmeet.hyperlauncher.filemanager.components

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.widget.SeekBar
import androidx.annotation.Nullable
import net.ashmeet.hyperlauncher.R

@SuppressLint("AppCompatCustomView")
class CustomSeekbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.seekBarStyle
) : SeekBar(context, attrs, defStyleAttr) {

    private var mMin = 0
    private var mIncrement = 1
    private var mListener: OnSeekBarChangeListener? = null

    private val mInternalListener = object : OnSeekBarChangeListener {
        private var internalChanges = false
        private var previousProgress = 0

        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (internalChanges) return
            internalChanges = true

            var adjustedProgress = progress + mMin
            adjustedProgress = applyIncrement(adjustedProgress)

            if (adjustedProgress != previousProgress) {
                if (mListener != null) {
                    previousProgress = adjustedProgress
                    mListener?.onProgressChanged(seekBar, adjustedProgress, fromUser)
                }
            }

            super@CustomSeekbar.setProgress(adjustedProgress)
            internalChanges = false
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            if (internalChanges) return
            mListener?.onStartTrackingTouch(seekBar)
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            if (internalChanges) return
            internalChanges = true

            super@CustomSeekbar.setProgress(seekBar.progress)

            mListener?.onStopTrackingTouch(seekBar)
            internalChanges = false
        }
    }

    init {
        setup(attrs)
    }

    fun setIncrement(increment: Int) {
        mIncrement = increment
    }

    fun setRange(min: Int, max: Int) {
        mMin = min
        setMax(max - min)
    }

    override fun setProgress(progress: Int) {
        super.setProgress(applyIncrement(progress - mMin))
    }

    override fun setProgress(progress: Int, animate: Boolean) {
        super.setProgress(applyIncrement(progress - mMin), animate)
    }

    override fun getProgress(): Int {
        return applyIncrement(super.getProgress() + mMin)
    }

    override fun setMin(min: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            super.setMin(0)
        }
        mMin = min
    }

    override fun setOnSeekBarChangeListener(l: OnSeekBarChangeListener?) {
        mListener = l
    }

    fun setup(@Nullable attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.CustomSeekbar).use { attributes ->
            setIncrement(attributes.getInt(R.styleable.CustomSeekbar_seekBarIncrement, 1))
            val min = attributes.getInt(R.styleable.CustomSeekbar_android_min, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                super.setMin(0)
            }
            setRange(min, super.max)
        }

        if (super.progress == 0) {
            super.setProgress(super.progress + 1)
            post {
                super.setProgress(super.progress - 1)
                post { super.setOnSeekBarChangeListener(mInternalListener) }
            }
        } else {
            super.setOnSeekBarChangeListener(mInternalListener)
        }
    }

    private fun applyIncrement(progress: Int): Int {
        if (mIncrement < 1) return progress
        var p = progress / mIncrement
        p = p * mIncrement
        return p
    }
}
