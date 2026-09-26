package com.ashmeet.hyperlauncher.plugins.manager

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ashmeet.hyperlauncher.plugins.interfaces.NativePlugin
import com.ashmeet.hyperlauncher.plugins.natives.DhCompatPlugin
import com.ashmeet.hyperlauncher.plugins.natives.ImGuiPlugin
import com.ashmeet.hyperlauncher.plugins.natives.PhysicsPlugin
import com.ashmeet.hyperlauncher.plugins.natives.RapierPlugin
import com.ashmeet.hyperlauncher.plugins.natives.FCLRendererPlugin
import com.ashmeet.hyperlauncher.plugins.natives.HpmPlugin
import com.ashmeet.hyperlauncher.utils.Tools
import java.io.File
import kotlin.collections.iterator

object HyperPluginManager {
    private const val TAG = "HyperPlugin"

    private val plugins = listOf(
        PhysicsPlugin(),
        RapierPlugin(),
        ImGuiPlugin(),
        DhCompatPlugin(),
        FCLRendererPlugin(),
        HpmPlugin()
    )

    @JvmStatic
    fun applyHooks(activity: AppCompatActivity, javaArgList: MutableList<String>, versionId: String, gameDir: File) {
        val mcVersion = resolveMinecraftVersion(versionId)

        discoverExternalPlugins(File(Tools.DIR_DATA, "plugins"), mcVersion)
        Tools.DIR_GAME_NEW?.let { discoverExternalPlugins(File(it, "plugins"), mcVersion) }

        setupLibraryPaths(javaArgList, versionId, mcVersion)
        applyPluginEnvironment(javaArgList, mcVersion)
        prepareNatives(activity, javaArgList, mcVersion)
    }


    @JvmStatic
    fun addDynamicPlugin(path: String, env: Map<String, String> = emptyMap(), mcVersion: String? = null) {
        val pluginDir = File(path)
        if (!pluginDir.exists() || !pluginDir.isDirectory) {
            Log.w(TAG, "Cannot add dynamic plugin: $path is not a valid directory")
            return
        }

        NativePluginManager.registerPlugin(object : NativePlugin {
            override fun getPaths(): Array<String> = arrayOf(path)
            override fun getJVMEnv(): Map<String, String> = env
            override fun supportsVersion(targetVersion: String?): Boolean {
                if (mcVersion == null || targetVersion == null) return true
                return mcVersion == targetVersion
            }
        })
        Log.i(TAG, "Registered dynamic plugin at: $path")
    }

    @JvmStatic
    fun discoverExternalPlugins(directory: File, mcVersion: String?) {
        if (!directory.exists() || !directory.isDirectory) return

        val files = directory.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                val soFiles = file.listFiles { _, name -> name.endsWith(".so") }
                if (soFiles != null && soFiles.isNotEmpty()) {
                    addDynamicPlugin(file.absolutePath, mcVersion = mcVersion)
                }
            }
        }
    }

    private fun resolveMinecraftVersion(versionId: String): String {
        return try {
            val vInfo = Tools.getVersionInfo(versionId)
            vInfo.inheritsFrom ?: vInfo.id
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve MC version for $versionId", e)
            versionId
        }
    }

    private fun setupLibraryPaths(javaArgList: MutableList<String>, versionId: String, mcVersion: String) {
        val versionSpecificNativesDir = File(Tools.DIR_CACHE, "natives/$versionId")
        val pluginPaths = NativePluginManager.getRuntimeLibraryPath(mcVersion)
        val nativeLibDir = Tools.NATIVE_LIB_DIR

        if (versionSpecificNativesDir.exists()) {
            val dirPath = versionSpecificNativesDir.absolutePath
            var combinedPath = "$dirPath:$nativeLibDir"
            if (pluginPaths.isNotEmpty()) {
                combinedPath = "$pluginPaths:$combinedPath"
            }

            javaArgList.add("-Djava.library.path=$combinedPath")
            javaArgList.add("-Djna.boot.library.path=$dirPath")
            javaArgList.add("-Djna.library.path=$combinedPath")
        } else if (pluginPaths.isNotEmpty()) {
            val combinedPath = "$pluginPaths:$nativeLibDir"
            javaArgList.add("-Djava.library.path=$combinedPath")
            javaArgList.add("-Djna.library.path=$combinedPath")
        }
    }

    private fun applyPluginEnvironment(javaArgList: MutableList<String>, mcVersion: String) {
        val pluginEnv = NativePluginManager.getRuntimeJVMEnv(mcVersion)
        for ((key, value) in pluginEnv) {
            if (key != "HYPERPLUGIN_PATH") {
                javaArgList.add("-D$key=$value")
            }
        }
    }

    private fun applyMioLibPatcher(javaArgList: MutableList<String>) {
        try {
            val possiblePaths = arrayOf(
                "${Tools.DIR_DATA}/MioLibPatcher/MioLibPatcher.jar",
                "${Tools.DIR_DATA}/launcher/MioLibPatcher.jar"
            )

            var patcherJar: File? = null
            for (path in possiblePaths) {
                val f = File(path)
                if (f.exists() && f.length() > 0L) {
                    patcherJar = f
                    break
                }
            }

            if (patcherJar != null) {
                javaArgList.add("-javaagent:${patcherJar.absolutePath}")
                Log.i(TAG, "SUCCESS: Applied MioLibPatcher agent: ${patcherJar.absolutePath}")
            } else {
                Log.e(TAG, "CRITICAL ERROR: MioLibPatcher.jar NOT FOUND! Axiom/Physics will crash.")
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to apply MioLibPatcher agent", t)
        }
    }

    private fun prepareNatives(activity: AppCompatActivity, javaArgList: MutableList<String>, mcVersion: String) {
        for (plugin in plugins) {
            try {
                plugin.prepare(activity, javaArgList, mcVersion)
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to prepare plugin ${plugin::class.java.simpleName}", t)
            }
        }
    }

    internal fun findNative(activity: AppCompatActivity, mcVersion: String, vararg possibleNames: String): File? {
        val nativeLibDir = activity.applicationInfo.nativeLibraryDir
        val pluginPaths = NativePluginManager.getRuntimeLibraryPath(mcVersion)

        for (name in possibleNames) {
            val f = File(nativeLibDir, name)
            if (f.exists()) return f
        }

        if (pluginPaths.isNotEmpty()) {
            val paths = pluginPaths.split(":").toTypedArray()
            for (path in paths) {
                for (name in possibleNames) {
                    val f = File(path, name)
                    if (f.exists()) return f
                }
            }
        }
        return null
    }
}