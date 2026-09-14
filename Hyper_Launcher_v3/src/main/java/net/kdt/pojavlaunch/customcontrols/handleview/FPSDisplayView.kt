package net.kdt.pojavlaunch.customcontrols.handleview

import android.content.Context
import android.util.AttributeSet
import android.view.Choreographer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences

class FPSDisplayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : DrawerPullButton(context, attrs) {

    private var fpsValue by mutableIntStateOf(0)
    private var frameCount = 0
    private var lastTime = 0L

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            val currentTime = frameTimeNanos / 1_000_000
            if (lastTime == 0L) {
                lastTime = currentTime
            }
            frameCount++
            if (currentTime - lastTime >= 1000) {
                fpsValue = frameCount
                frameCount = 0
                lastTime = currentTime
            }
            if (isAttachedToWindow) {
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
    }

    init {
        composeView.setContent {
            FPSContent(fpsValue)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        
        // Overwrite position with FPS specific one
        if (LauncherPreferences.PREF_FPS_POS_X != -1f && LauncherPreferences.PREF_FPS_POS_Y != -1f) {
            x = LauncherPreferences.PREF_FPS_POS_X
            y = LauncherPreferences.PREF_FPS_POS_Y
        } else {
            // Default position for FPS if not set, maybe top right
            // x = ...
        }
        
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    override fun onDetachedFromWindow() {
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        super.onDetachedFromWindow()
    }

    @Composable
    private fun FPSContent(fps: Int) {
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

            Text(
                text = fps.toString(),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    override fun savePosition() {
        LauncherPreferences.PREF_FPS_POS_X = x
        LauncherPreferences.PREF_FPS_POS_Y = y
        
        LauncherPreferences.prefs.edit {
            putFloat("fps_pos_x", x)
            putFloat("fps_pos_y", y)
        }
    }
}
