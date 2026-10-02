package com.ashmeet.hyperlauncher.plugins.natives

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ashmeet.hyperlauncher.plugins.interfaces.HyperPlugin
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences

class FCLRendererPlugin : HyperPlugin {
    override fun prepare(activity: AppCompatActivity, javaArgList: MutableList<String>, mcVersion: String) {
        val selectedRenderer = LauncherPreferences.PREF_RENDERER
        Log.i("FCLRenderer", "HyperLauncher FCL Renderer selected: $selectedRenderer for MC version $mcVersion")
        val plugins = LibraryPlugin.discoverAllPlugins(activity)
        for (plugin in plugins) {
            if (plugin.getRendererId() == selectedRenderer || plugin.rendererName == selectedRenderer) {
                Log.i("FCLRenderer", "Found matching renderer plugin: ${plugin.appId}, lib: ${plugin.getRendererLibrary()}, egl: ${plugin.getRendererEGL()}")
                val dlopenList = plugin.getDlopenLibrary().value
                for (lib in dlopenList) {
                    Log.i("FCLRenderer", "Dlopen target: $lib")
                }
            }
        }
    }
}
