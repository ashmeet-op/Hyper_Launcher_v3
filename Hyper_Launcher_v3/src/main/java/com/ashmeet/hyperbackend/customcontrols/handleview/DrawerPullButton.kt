package com.ashmeet.hyperbackend.customcontrols.handleview

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.ashmeet.hyperlauncher.recorder.RecordingManager
import com.ashmeet.hyperlauncher.recorder.RecordingState
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.theme.PojavTheme
import com.ashmeet.hyperlauncher.utils.HyperUtils
import dev.horizon.android.HorizonInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

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
    protected var showFps by mutableStateOf(LauncherPreferences.PREF_SHOW_FPS)
    protected var showMemory by mutableStateOf(LauncherPreferences.PREF_SHOW_MEMORY)

    private var widthMultiplier by mutableFloatStateOf(if (LauncherPreferences.PREF_SHOW_FPS) 1.5f else 1.0f)
    private var heightMultiplier by mutableFloatStateOf(1.0f)
    private var widthAnimator: ValueAnimator? = null
    private var heightAnimator: ValueAnimator? = null

    private var fpsValue by mutableIntStateOf(0)
    private var fpsJob: Job? = null

    private var usedMemoryMb by mutableIntStateOf(0)
    private var allocatedMemoryMb by mutableIntStateOf(0)
    private var freeMemoryMb by mutableIntStateOf(0)
    private var memoryJob: Job? = null

    private fun startFpsTracker() {
        stopFpsTracker()
        fpsJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive && showFps) {
                val nativeFps = withContext(Dispatchers.IO) {
                    try {
                        if (HorizonInput.sLoaded) {
                            val hFps = HorizonInput.fps()
                            if (hFps > 0) return@withContext hFps
                        }
                        val gFps = git.artdeell.dnbootstrap.glfw.GLFW.getFps()
                        if (gFps > 0) return@withContext gFps

                        val sFps = git.mojo.sdl.SDLActivity.getFps()
                        if (sFps > 0) return@withContext sFps
                        0
                    } catch ( _ : Throwable) {
                        0
                    }
                }
                fpsValue = nativeFps
                delay(1000.milliseconds)
            }
        }
    }

    private fun stopFpsTracker() {
        fpsJob?.cancel()
        fpsJob = null
    }

    private fun startMemoryTracker() {
        stopMemoryTracker()
        memoryJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive && showMemory) {
                val (alloc, free, used) = withContext(Dispatchers.IO) {
                    val prefAlloc = LauncherPreferences.PREF_RAM_ALLOCATION
                    val totalDevice = HyperUtils.getTotalDeviceMemory(context)
                    val allocated = if (prefAlloc > 0) prefAlloc else totalDevice
                    val freeMem = HyperUtils.getFreeDeviceMemory(context)
                    val usedMem = (allocated - freeMem).coerceAtLeast(0)
                    Triple(allocated, freeMem, usedMem)
                }
                allocatedMemoryMb = alloc
                freeMemoryMb = free
                usedMemoryMb = used
                delay(1000.milliseconds)
            }
        }
    }

    private fun stopMemoryTracker() {
        memoryJob?.cancel()
        memoryJob = null
    }

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "drawer_pull_size_perc", "drawer_pull_opacity", "drawer_pull_icon_opacity",
            "drawer_pull_background", "drawer_pull_icon_path", "show_fps", "show_memory" -> {
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
            PojavTheme {
                DrawerPullButtonContent(
                    showBackground = showBackground,
                    bgOpacity = bgOpacity,
                    iconOpacity = iconOpacity,
                    iconPath = iconPath,
                    showFps = showFps,
                    fpsValue = fpsValue,
                    showMemory = showMemory,
                    usedMemoryMb = usedMemoryMb,
                    allocatedMemoryMb = allocatedMemoryMb,
                    freeMemoryMb = freeMemoryMb
                )
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        LauncherPreferences.prefs.registerOnSharedPreferenceChangeListener(prefListener)
        updateAppearance()

        if (LauncherPreferences.PREF_DRAWER_PULL_POS_X != -1f && LauncherPreferences.PREF_DRAWER_PULL_POS_Y != -1f) {
            x = LauncherPreferences.PREF_DRAWER_PULL_POS_X
            y = LauncherPreferences.PREF_DRAWER_PULL_POS_Y
        }

        if (showFps) {
            startFpsTracker()
        }
        if (showMemory) {
            startMemoryTracker()
        }
    }

    override fun onDetachedFromWindow() {
        LauncherPreferences.prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        stopFpsTracker()
        stopMemoryTracker()
        widthAnimator?.cancel()
        heightAnimator?.cancel()
        super.onDetachedFromWindow()
    }

    open fun updateAppearance() {
        pullSizePerc = LauncherPreferences.PREF_DRAWER_PULL_SIZE_PERC
        bgOpacity = LauncherPreferences.PREF_DRAWER_PULL_BG_OPACITY
        iconOpacity = LauncherPreferences.PREF_DRAWER_PULL_ICON_OPACITY
        showBackground = LauncherPreferences.PREF_DRAWER_PULL_BACKGROUND
        iconPath = LauncherPreferences.PREF_DRAWER_PULL_ICON_PATH

        val oldShowFps = showFps
        showFps = LauncherPreferences.PREF_SHOW_FPS
        if (showFps && !oldShowFps) {
            startFpsTracker()
        } else if (!showFps && oldShowFps) {
            stopFpsTracker()
        }

        val oldShowMemory = showMemory
        showMemory = LauncherPreferences.PREF_SHOW_MEMORY
        if (showMemory && !oldShowMemory) {
            startMemoryTracker()
        } else if (!showMemory && oldShowMemory) {
            stopMemoryTracker()
        }

        requestLayout()
    }

    private fun animateWidth(target: Float) {
        if (abs(widthMultiplier - target) < 0.01f) return
        widthAnimator?.cancel()
        widthAnimator = ValueAnimator.ofFloat(widthMultiplier, target).apply {
            duration = 300
            addUpdateListener { animator ->
                widthMultiplier = animator.animatedValue as Float
                requestLayout()
            }
            start()
        }
    }

    private fun animateHeight(target: Float) {
        if (abs(heightMultiplier - target) < 0.01f) return
        heightAnimator?.cancel()
        heightAnimator = ValueAnimator.ofFloat(heightMultiplier, target).apply {
            duration = 300
            addUpdateListener { animator ->
                heightMultiplier = animator.animatedValue as Float
                requestLayout()
            }
            start()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val dm = resources.displayMetrics
        val dpSize = (25 + (pullSizePerc - 10) * (35f / 90f))
        val size = (dpSize * dm.density).toInt()
        val width = (size * widthMultiplier).toInt()
        val height = (size * heightMultiplier).toInt()

        val newWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
        val newHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        super.onMeasure(newWidthSpec, newHeightSpec)
        setMeasuredDimension(width, height)
    }

    @Composable
    private fun DrawerPullButtonContent(
        showBackground: Boolean,
        bgOpacity: Int,
        iconOpacity: Int,
        iconPath: String?,
        showFps: Boolean,
        fpsValue: Int,
        showMemory: Boolean,
        usedMemoryMb: Int,
        allocatedMemoryMb: Int,
        freeMemoryMb: Int
    ) {
        val recordingState by RecordingManager.recordingState.collectAsState()
        val isRecording = recordingState is RecordingState.Recording
        val durationSec = (recordingState as? RecordingState.Recording)?.durationSeconds ?: 0L

        val hasFpsOrRecording = isRecording || showFps

        LaunchedEffect(isRecording, showFps, fpsValue, showMemory) {
            val fpsExtra = when {
                !showFps -> 0.0f
                fpsValue >= 1000 -> 0.45f
                fpsValue >= 100 -> 0.25f
                else -> 0.0f
            }
            val targetW = when {
                hasFpsOrRecording && showMemory -> {
                    when {
                        isRecording && showFps -> 4.5f + fpsExtra
                        else -> 4.2f + fpsExtra
                    }
                }
                hasFpsOrRecording && !showMemory -> {
                    when {
                        isRecording && showFps -> 2.4f + fpsExtra
                        isRecording && !showFps -> 1.8f
                        else -> 1.5f + fpsExtra
                    }
                }
                !hasFpsOrRecording && showMemory -> 4.2f
                else -> 1.0f
            }
            val targetH = if (hasFpsOrRecording && showMemory) 1.8f else 1.0f

            animateWidth(targetW)
            animateHeight(targetH)
        }

        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )

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

            val isExpanded = hasFpsOrRecording || showMemory
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize(0.85f)
                    .padding(horizontal = 4.dp)
            ) {
                if (customBitmap != null) {
                    Image(
                        bitmap = customBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(if (isExpanded) 20.dp else 28.dp)
                            .alpha(iconOpacity / 100f)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = null,
                        modifier = Modifier
                            .size(if (isExpanded) 20.dp else 28.dp)
                            .alpha(iconOpacity / 100f),
                        tint = Color.White
                    )
                }

                if (hasFpsOrRecording && showMemory) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isRecording) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color.Red.copy(alpha = pulseAlpha))
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = RecordingManager.formatDuration(durationSec),
                                        color = Color.Red,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            if (showFps) {
                                Text(
                                    text = if (isRecording) "  $fpsValue Fps" else " $fpsValue Fps",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.alpha(iconOpacity / 100f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        MemoryIndicator(
                            usedMb = usedMemoryMb,
                            allocatedMb = allocatedMemoryMb,
                            freeMb = freeMemoryMb,
                            iconOpacity = iconOpacity
                        )
                    }
                } else if (hasFpsOrRecording) {
                    AnimatedVisibility(
                        visible = isRecording,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red.copy(alpha = pulseAlpha))
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = RecordingManager.formatDuration(durationSec),
                                color = Color.Red,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showFps,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Text(
                                text = " $fpsValue Fps",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.alpha(iconOpacity / 100f)
                            )
                        }
                    }
                } else if (showMemory) {
                    Spacer(modifier = Modifier.width(4.dp))
                    MemoryIndicator(
                        usedMb = usedMemoryMb,
                        allocatedMb = allocatedMemoryMb,
                        freeMb = freeMemoryMb,
                        iconOpacity = iconOpacity
                    )
                }
            }
        }
    }

    @Composable
    private fun MemoryIndicator(
        usedMb: Int,
        allocatedMb: Int,
        freeMb: Int,
        iconOpacity: Int
    ) {
        val ratio = if (allocatedMb > 0) (usedMb.toFloat() / allocatedMb.toFloat()).coerceIn(0f, 1f) else 0f
        val animatedRatio by animateFloatAsState(
            targetValue = ratio,
            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
            label = "Memory Progress"
        )
        val barColor = when {
            ratio >= 0.85f -> MaterialTheme.colorScheme.error
            ratio >= 0.70f -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.primary
        }

        val textString = if (allocatedMb <= 0) "Loading..." else "$usedMb/${allocatedMb}MB (${freeMb}MB free)"
        val invertedTextColor = if (barColor.luminance() > 0.45f) Color.Black else Color.White

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .height(16.dp)
                .alpha(iconOpacity / 100f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
        ) {
            // Progress Bar Fill
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedRatio.coerceIn(0f, 1f))
                    .background(barColor)
                    .align(Alignment.CenterStart)
            )

            // Base Text (White for dark track background)
            Text(
                text = textString,
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.6f),
                        blurRadius = 2f
                    )
                ),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .matchParentSize()
                    .drawWithContent {
                        clipRect(right = size.width * animatedRatio.coerceIn(0f, 1f)) {
                            this@drawWithContent.drawContent()
                        }
                    }
            ) {
                Text(
                    text = textString,
                    color = invertedTextColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    style = TextStyle(
                        shadow = if (barColor.luminance() > 0.45f) {
                            Shadow(
                                color = Color.White.copy(alpha = 0.6f),
                                blurRadius = 2f
                            )
                        } else {
                            Shadow(
                                color = Color.Black.copy(alpha = 0.6f),
                                blurRadius = 2f
                            )
                        }
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
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

        if (!mHasMoved && LauncherPreferences.PREF_DRAWER_PULL_POS_X != -1f && LauncherPreferences.PREF_DRAWER_PULL_POS_Y != -1f) {
            x = LauncherPreferences.PREF_DRAWER_PULL_POS_X
            y = LauncherPreferences.PREF_DRAWER_PULL_POS_Y
        } else if (!mHasMoved) {
            val parent = parent as? View ?: return
            x = parent.width.toFloat() - measuredWidth.toFloat()
            y = (parent.height.toFloat() - measuredHeight.toFloat()) / 2f
        }
    }

    @Suppress("SameParameterValue")
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
