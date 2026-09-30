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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences.PREF_DYNAMIC_ORIENTATION
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences.PREF_FULLSCREEN_LAUNCHER
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences.PREF_IGNORE_NOTCH
import com.ashmeet.hyperlauncher.utils.Tools
import net.kdt.pojavlaunch.utils.LocaleUtils

abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleUtils.setLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        if (shouldEnableEdgeToEdge() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM)) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            )
        }

        super.onCreate(savedInstanceState)
        LocaleUtils.setLocale(this)

        applySystemBarConfiguration()
        updateOrientation()
        Tools.getDisplayMetrics(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applySystemBarConfiguration()
        updateOrientation()
        Tools.getDisplayMetrics(this)
    }


    open fun updateOrientation() {
        requestedOrientation = if (PREF_DYNAMIC_ORIENTATION) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
        }
    }


    private fun applySystemBarConfiguration() {
        val isFullscreen = setFullscreen()
        val isEdgeToEdge = shouldEnableEdgeToEdge()
        val hideBars = isFullscreen || isEdgeToEdge

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (hideBars) {

            var types = WindowInsetsCompat.Type.navigationBars()
            if (isFullscreen) {
                types = types or WindowInsetsCompat.Type.statusBars()
            }
            controller.hide(types)
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }



        Tools.setInsetsMode(this, hideBars, shouldIgnoreNotch())
    }


    open fun setFullscreen(): Boolean {
        return PREF_FULLSCREEN_LAUNCHER
    }

    override fun onResume() {
        super.onResume()
        updateOrientation()
        Tools.checkStorageInteractive(this)
    }

    override fun onPostResume() {
        super.onPostResume()

        applySystemBarConfiguration()
        updateOrientation()
        Tools.getDisplayMetrics(this)
    }


    protected open fun shouldIgnoreNotch(): Boolean {
        return PREF_IGNORE_NOTCH
    }


    protected open fun shouldEnableEdgeToEdge(): Boolean {
        return PREF_FULLSCREEN_LAUNCHER
    }
}
