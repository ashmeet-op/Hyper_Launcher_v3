package com.ashmeet.hyperlauncher.plugins.natives

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ashmeet.hyperlauncher.plugins.interfaces.HyperPlugin
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences

class FCLRendererPlugin : HyperPlugin {
    override fun prepare(activity: AppCompatActivity, javaArgList: MutableList<String>, mcVersion: String) {
        val selectedRenderer = LauncherPreferences.PREF_RENDERER
        Log.i("FCLRenderer", "HyperLauncher FCL Renderer selected: $selectedRenderer")
    }
}