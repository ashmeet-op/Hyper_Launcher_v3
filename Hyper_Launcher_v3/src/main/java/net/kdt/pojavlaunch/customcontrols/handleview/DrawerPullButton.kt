package net.kdt.pojavlaunch.customcontrols.handleview

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.edit
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import java.io.File
import kotlin.math.abs

open class DrawerPullButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    protected val composeView = ComposeView(context)
    private var mInitialX = 0f
    private var mInitialY = 0f
    private var mInitialTouchX = 0f
    private var mInitialTouchY = 0f
    private var mHasMoved = false

    protected var pullSizePerc by mutableFloatStateOf(LauncherPreferences.PREF_DRAWER_PULL_SIZE_PERC)
    protected var bgOpacity by mutableIntStateOf(LauncherPreferences.PREF_DRAWER_PULL_BG_OPACITY)
    protected var iconOpacity by mutableIntStateOf(LauncherPreferences.PREF_DRAWER_PULL_ICON_OPACITY)
    protected var showBackground by mutableStateOf(LauncherPreferences.PREF_DRAWER_PULL_BACKGROUND)
    protected var iconPath by mutableStateOf(LauncherPreferences.PREF_DRAWER_PULL_ICON_PATH)

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "drawer_pull_size_perc", "drawer_pull_opacity", "drawer_pull_icon_opacity",
            "drawer_pull_background", "drawer_pull_icon_path" -> {
                updateAppearance()
            }
            "drawer_pull_pos_x", "drawer_pull_pos_y" -> {
                if (LauncherPreferences.PREF_DRAWER_PULL_POS_X == -1f || LauncherPreferences.PREF_DRAWER_PULL_POS_Y == -1f) {
                    mHasMoved = false
                    requestLayout()
                } else {
                    x = LauncherPreferences.PREF_DRAWER_PULL_POS_X
                    y = LauncherPreferences.PREF_DRAWER_PULL_POS_Y
                }
            }
        }
    }

    init {
        isClickable = true
        addView(composeView)
        composeView.setContent {
            DrawerPullButtonContent(
                showBackground = showBackground,
                bgOpacity = bgOpacity,
                iconOpacity = iconOpacity,
                iconPath = iconPath
            )
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        LauncherPreferences.prefs.registerOnSharedPreferenceChangeListener(prefListener)
        updateAppearance()
        
        // Load saved position
        if (LauncherPreferences.PREF_DRAWER_PULL_POS_X != -1f && LauncherPreferences.PREF_DRAWER_PULL_POS_Y != -1f) {
            x = LauncherPreferences.PREF_DRAWER_PULL_POS_X
            y = LauncherPreferences.PREF_DRAWER_PULL_POS_Y
        }
    }

    override fun onDetachedFromWindow() {
        LauncherPreferences.prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        super.onDetachedFromWindow()
    }

    open fun updateAppearance() {
        pullSizePerc = LauncherPreferences.PREF_DRAWER_PULL_SIZE_PERC
        bgOpacity = LauncherPreferences.PREF_DRAWER_PULL_BG_OPACITY
        iconOpacity = LauncherPreferences.PREF_DRAWER_PULL_ICON_OPACITY
        showBackground = LauncherPreferences.PREF_DRAWER_PULL_BACKGROUND
        iconPath = LauncherPreferences.PREF_DRAWER_PULL_ICON_PATH

        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val dm = resources.displayMetrics
        val dpSize = (25 + (pullSizePerc - 10) * (35f / 90f))
        val size = (dpSize * dm.density).toInt()
        
        val newWidthSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
        val newHeightSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
        super.onMeasure(newWidthSpec, newHeightSpec)
        setMeasuredDimension(size, size)
    }

    @Composable
    private fun DrawerPullButtonContent(
        showBackground: Boolean,
        bgOpacity: Int,
        iconOpacity: Int,
        iconPath: String?
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (showBackground) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.85f)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = bgOpacity / 100f))
                )
            }

            val customBitmap = remember(iconPath) {
                iconPath?.let { path ->
                    if (File(path).exists()) {
                        val options = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                            BitmapFactory.decodeFile(path, this)
                            inSampleSize = calculateInSampleSize(this, 256, 256)
                            inJustDecodeBounds = false
                        }
                        BitmapFactory.decodeFile(path, options)
                    } else null
                }
            }

            if (customBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = customBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize(0.55f)
                        .alpha(iconOpacity / 100f)
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize(0.55f)
                        .alpha(iconOpacity / 100f),
                    tint = Color.White
                )
            }
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // We must intercept the touch event to ensure onTouchEvent is called, 
        // as children (like ComposeView) might consume it otherwise.
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!LauncherPreferences.PREF_DRAWER_PULL_HOLD_TO_MOVE) {
            return super.onTouchEvent(event)
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mInitialX = x
                mInitialY = y
                mInitialTouchX = event.rawX
                mInitialTouchY = event.rawY
                mHasMoved = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - mInitialTouchX
                val dy = event.rawY - mInitialTouchY
                
                if (abs(dx) > 10 || abs(dy) > 10) {
                    x = mInitialX + dx
                    y = mInitialY + dy
                    mHasMoved = true
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (mHasMoved) {
                    savePosition()
                } else {
                    performClick()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                if (mHasMoved) savePosition()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    open fun savePosition() {
        LauncherPreferences.PREF_DRAWER_PULL_POS_X = x
        LauncherPreferences.PREF_DRAWER_PULL_POS_Y = y
        
        LauncherPreferences.prefs.edit {
            putFloat("drawer_pull_pos_x", x)
            putFloat("drawer_pull_pos_y", y)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        // Only set initial position if not currently being moved
        if (!mHasMoved && LauncherPreferences.PREF_DRAWER_PULL_POS_X != -1f && LauncherPreferences.PREF_DRAWER_PULL_POS_Y != -1f) {
            x = LauncherPreferences.PREF_DRAWER_PULL_POS_X
            y = LauncherPreferences.PREF_DRAWER_PULL_POS_Y
        } else if (!mHasMoved) {
            translationX = 0f
            translationY = 0f
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
