package com.ashmeet.hyperlauncher.components.layout

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Gravity
import android.view.Surface
import android.view.TextureView
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import kotlin.math.max

@Composable
fun LauncherBackground(
    modifier: Modifier = Modifier
) {
    var launcherBgPath by remember { mutableStateOf(LauncherPreferences.PREF_LAUNCHER_BACKGROUND_PATH) }
    var launcherBgType by remember { mutableStateOf(LauncherPreferences.PREF_LAUNCHER_BACKGROUND_TYPE) }
    var launcherBgOverlayEnabled by remember { mutableStateOf(LauncherPreferences.PREF_LAUNCHER_BACKGROUND_OVERLAY_ENABLED) }
    var launcherBgOverlayOpacity by remember { mutableFloatStateOf(LauncherPreferences.PREF_LAUNCHER_BACKGROUND_OVERLAY_OPACITY.toFloat()) }
    var launcherBgBlurEnabled by remember { mutableStateOf(LauncherPreferences.PREF_LAUNCHER_BACKGROUND_BLUR_ENABLED) }
    var launcherBgBlur by remember { mutableFloatStateOf(LauncherPreferences.PREF_LAUNCHER_BACKGROUND_BLUR.toFloat()) }
    var launcherVideoMuted by remember { mutableStateOf(LauncherPreferences.PREF_LAUNCHER_VIDEO_MUTED) }
    var launcherVideoVolume by remember { mutableFloatStateOf(LauncherPreferences.PREF_LAUNCHER_VIDEO_VOLUME.toFloat()) }
    var launcherVideoLoop by remember { mutableStateOf(LauncherPreferences.PREF_LAUNCHER_VIDEO_LOOP) }
    var mediaPlayerInstance by remember { mutableStateOf<MediaPlayer?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                try {
                    mediaPlayerInstance?.stop()
                    mediaPlayerInstance?.release()
                } catch (_: Exception) {}
                mediaPlayerInstance = null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                mediaPlayerInstance?.stop()
                mediaPlayerInstance?.release()
            } catch (_: Exception) {}
            mediaPlayerInstance = null
        }
    }

    DisposableEffect(Unit) {
        val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "launcher_background_path" -> launcherBgPath = LauncherPreferences.prefs.getString("launcher_background_path", null)
                "launcher_background_type" -> launcherBgType = LauncherPreferences.prefs.getString("launcher_background_type", "image") ?: "image"
                "launcher_background_overlay_enabled" -> launcherBgOverlayEnabled = LauncherPreferences.prefs.getBoolean("launcher_background_overlay_enabled", true)
                "launcher_background_overlay_opacity" -> launcherBgOverlayOpacity = LauncherPreferences.prefs.getInt("launcher_background_overlay_opacity", 50).toFloat()
                "launcher_background_blur_enabled" -> launcherBgBlurEnabled = LauncherPreferences.prefs.getBoolean("launcher_background_blur_enabled", false)
                "launcher_background_blur" -> launcherBgBlur = LauncherPreferences.prefs.getInt("launcher_background_blur", 0).toFloat()
                "launcher_video_muted" -> launcherVideoMuted = LauncherPreferences.prefs.getBoolean("launcher_video_muted", true)
                "launcher_video_volume" -> launcherVideoVolume = LauncherPreferences.prefs.getInt("launcher_video_volume", 50).toFloat()
                "launcher_video_loop" -> launcherVideoLoop = LauncherPreferences.prefs.getBoolean("launcher_video_loop", true)
            }
        }
        LauncherPreferences.prefs.registerOnSharedPreferenceChangeListener(prefListener)
        onDispose {
            LauncherPreferences.prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        }
    }

    val backgroundBitmap = remember(launcherBgPath, launcherBgType) {
        if (launcherBgPath != null && launcherBgType == "image") {
            try {
                val opts = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(launcherBgPath, opts)
                if (opts.outWidth > 0 && opts.outHeight > 0) {
                    var sampleSize = 1
                    val maxTargetWidth = 1920
                    val maxTargetHeight = 1080
                    while (opts.outWidth / (sampleSize * 2) >= maxTargetWidth &&
                        opts.outHeight / (sampleSize * 2) >= maxTargetHeight
                    ) {
                        sampleSize *= 2
                    }
                    opts.inSampleSize = sampleSize
                    opts.inJustDecodeBounds = false
                    opts.inPreferredConfig = Bitmap.Config.RGB_565
                    BitmapFactory.decodeFile(launcherBgPath, opts)
                } else null
            } catch (_: Exception) {
                null
            } catch (_: OutOfMemoryError) {
                null
            }
        } else null
    }

    Box(modifier = modifier.fillMaxSize().background(if (launcherBgPath != null) Color.Black else Color.Transparent)) {
        if (launcherBgType == "image" && backgroundBitmap != null) {
            Image(
                bitmap = backgroundBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (launcherBgBlurEnabled && launcherBgBlur > 0)
                            Modifier.blur(launcherBgBlur.dp)
                        else Modifier
                    ),
                contentScale = ContentScale.Crop
            )
        } else if (launcherBgType == "video" && launcherBgPath != null) {
            AndroidView(
                factory = { context ->
                    val root = FrameLayout(context)
                    val textureView = TextureView(context)
                    val mediaPlayer = MediaPlayer()
                    mediaPlayerInstance = mediaPlayer

                    textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                            val surface = Surface(st)
                            mediaPlayer.setSurface(surface)
                            try {
                                mediaPlayer.setDataSource(launcherBgPath)
                                mediaPlayer.prepareAsync()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {}
                        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                            try {
                                if (mediaPlayer.isPlaying) {
                                    mediaPlayer.stop()
                                }
                            } catch (_: Exception) {}
                            mediaPlayer.release()
                            if (mediaPlayerInstance == mediaPlayer) {
                                mediaPlayerInstance = null
                            }
                            return true
                        }
                        override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                    }

                    mediaPlayer.setOnPreparedListener { mp ->
                        mp.isLooping = launcherVideoLoop
                        if (launcherVideoMuted) {
                            mp.setVolume(0f, 0f)
                        } else {
                            val vol = launcherVideoVolume / 100f
                            mp.setVolume(vol, vol)
                        }

                        val videoWidth = mp.videoWidth.toFloat()
                        val videoHeight = mp.videoHeight.toFloat()
                        val viewWidth = root.width.toFloat()
                        val viewHeight = root.height.toFloat()

                        if (videoWidth > 0 && videoHeight > 0 && viewWidth > 0 && viewHeight > 0) {
                            val scale = max(viewWidth / videoWidth, viewHeight / videoHeight)
                            textureView.layoutParams = FrameLayout.LayoutParams(
                                (videoWidth * scale).toInt(),
                                (videoHeight * scale).toInt(),
                                Gravity.CENTER
                            )
                        }
                        mp.start()
                    }

                    root.tag = mediaPlayer
                    root.addView(textureView)
                    root
                },
                update = { root ->
                    val mp = root.tag as? MediaPlayer
                    if (mp != null) {
                        mp.isLooping = launcherVideoLoop
                        if (launcherVideoMuted) {
                            mp.setVolume(0f, 0f)
                        } else {
                            val vol = launcherVideoVolume / 100f
                            mp.setVolume(vol, vol)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (launcherBgBlurEnabled && launcherBgBlur > 0)
                            Modifier.blur(launcherBgBlur.dp)
                        else Modifier
                    )
            )
        }

        if (launcherBgPath != null && launcherBgOverlayEnabled) {
            val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
            val overlayColor = if (isDark) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f).compositeOver(Color.Black)
            } else {
                MaterialTheme.colorScheme.primary
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayColor.copy(alpha = launcherBgOverlayOpacity / 100f))
            )
        }
    }
}
