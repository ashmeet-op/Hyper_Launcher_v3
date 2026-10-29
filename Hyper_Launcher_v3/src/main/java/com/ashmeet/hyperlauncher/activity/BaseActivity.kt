package com.ashmeet.hyperlauncher.activity

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences.PREF_FULLSCREEN_LAUNCHER
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences.PREF_IGNORE_NOTCH
import com.ashmeet.hyperlauncher.utils.HyperUtils
import com.ashmeet.hyperbackend.utils.LocaleUtils

abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleUtils.setLocale(newBase))
    }

  
    override fun onCreate(savedInstanceState: Bundle?) {

        if ((shouldEnableEdgeToEdge() && shouldIgnoreNotch()) || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && shouldIgnoreNotch())) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            )
        }

        super.onCreate(savedInstanceState)
        LocaleUtils.setLocale(this)

        applySystemBarConfiguration()
        updateOrientation()
        HyperUtils.getDisplayMetrics(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applySystemBarConfiguration()
        updateOrientation()
        HyperUtils.getDisplayMetrics(this)
    }


    open fun updateOrientation() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }



    private fun applySystemBarConfiguration() {
        val isFullscreen = setFullscreen()
        val isEdgeToEdge = shouldEnableEdgeToEdge()
        val hideBars = isFullscreen || isEdgeToEdge

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        controller.hide(WindowInsetsCompat.Type.statusBars())

        if (hideBars) {
            controller.hide(WindowInsetsCompat.Type.navigationBars())
        } else {
            controller.show(WindowInsetsCompat.Type.navigationBars())
        }

        HyperUtils.setInsetsMode(this, hideBars, shouldIgnoreNotch())
    }


    open fun setFullscreen(): Boolean {
        return PREF_FULLSCREEN_LAUNCHER
    }

    override fun onResume() {
        super.onResume()
        updateOrientation()
        HyperUtils.checkStorageInteractive(this)
    }

    override fun onPostResume() {
        super.onPostResume()

        applySystemBarConfiguration()
        updateOrientation()
        HyperUtils.getDisplayMetrics(this)
    }


    protected open fun shouldIgnoreNotch(): Boolean {
        return PREF_IGNORE_NOTCH
    }


    protected open fun shouldEnableEdgeToEdge(): Boolean {
        return PREF_FULLSCREEN_LAUNCHER
    }
}
