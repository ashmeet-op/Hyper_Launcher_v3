package com.ashmeet.hyperlauncher.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.ashmeet.hyperlauncher.plugins.manager.NativePluginManager
import git.artdeell.mojoexec.MojoExec
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.utils.GLInfoUtils
import net.kdt.pojavlaunch.utils.JREUtils
import net.kdt.pojavlaunch.utils.MesaUtils
import java.io.File
import java.util.ArrayList

object RendererCompatUtil {
    private var sCompatibleRenderers: RenderersList? = null

    @JvmStatic
    fun checkVulkanSupport(packageManager: PackageManager): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL) &&
                packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)
    }

    private fun findNativeLibraryPath(name: String): String? {
        val file = File(Tools.NATIVE_LIB_DIR, name)
        if (file.exists()) return file.absolutePath

        val pluginPaths = NativePluginManager.getRuntimeLibraryPath()

        if (pluginPaths.isNotEmpty()) {
            for (path in pluginPaths.split(":").toTypedArray()) {
                val pFile = File(path, name)
                if (pFile.exists()) return pFile.absolutePath
            }
        }
        return null
    }

    private fun hasNativeLibrary(name: String): Boolean {
        return findNativeLibraryPath(name) != null
    }

    @JvmStatic
    fun getCompatibleRenderers(context: Context): RenderersList {
        sCompatibleRenderers?.let { return it }

        val resources = context.resources
        val defaultRenderers = resources.getStringArray(R.array.renderer_values)
        val defaultRendererNames = resources.getStringArray(R.array.renderer)

        val deviceHasVulkan = checkVulkanSupport(context.packageManager)
        val deviceCompatibleMesa = Build.VERSION.SDK_INT >= 29 && hasNativeLibrary("libEGL_mesa.so")
        val deviceHasOpenGLES3 = JREUtils.getDetectedVersion() >= 3
        val appHasLtw = hasNativeLibrary("libltw.so")
        val appHasMobileGlues = hasNativeLibrary("libmobileglues.so")

        val rendererIds = ArrayList<String>(defaultRenderers.size)
        val rendererNames = ArrayList<String>(defaultRendererNames.size)

        for (i in defaultRenderers.indices) {
            val rendererId = defaultRenderers[i]
            if (rendererId.contains("vulkan") && !deviceHasVulkan) continue
            if (rendererId.contains("zink") && !deviceCompatibleMesa) continue
            if (rendererId.contains("freedreno") && (!GLInfoUtils.getGlInfo().isAdreno || !deviceCompatibleMesa)) continue
            if (rendererId.contains("ltw") && (!deviceHasOpenGLES3 || !appHasLtw)) continue
            if (rendererId.contains("mobileglues") && (!deviceHasOpenGLES3 || !appHasMobileGlues)) continue
            rendererIds.add(rendererId)
            rendererNames.add(defaultRendererNames[i])
        }

        for (plugin in NativePluginManager.getPlugins()) {
            val rendererId = plugin.rendererName
            if (rendererId != null && !rendererIds.contains(rendererId)) {
                rendererIds.add(rendererId)
                val displayName = plugin.displayName ?: ("FCL: $rendererId")
                val pluginName = plugin.name
                if (pluginName != null) {
                    rendererNames.add("$displayName (from $pluginName plugin)")
                } else {
                    rendererNames.add(displayName)
                }
            }
        }

        val list = RenderersList(
            rendererIds,
            rendererNames.toTypedArray()
        )
        sCompatibleRenderers = list

        return list
    }

    @JvmStatic
    fun getCompatibleDrivers(context: Context): RenderersList {
        val driverIds = ArrayList<String>()
        val driverNames = ArrayList<String>()

        driverIds.add("default")
        driverNames.add("Default")

        for (plugin in NativePluginManager.getPlugins()) {
            val driverId = plugin.driverName
            if (driverId != null && !driverIds.contains(driverId)) {
                driverIds.add(driverId)
                val displayName = plugin.displayName ?: ("FCL: $driverId")
                val pluginName = plugin.name
                if (pluginName != null) {
                    driverNames.add("$displayName (from $pluginName plugin)")
                } else {
                    driverNames.add(displayName)
                }
            }
        }

        return RenderersList(driverIds, driverNames.toTypedArray())
    }

    @JvmStatic
    fun checkRendererCompatible(context: Context, rendererName: String): Boolean {
        return getCompatibleRenderers(context).rendererIds.contains(rendererName)
    }

    @JvmStatic
    fun releaseRenderersCache() {
        sCompatibleRenderers = null
        System.gc()
    }

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    @JvmStatic
    fun loadGraphicsLibrary(renderer: String): String? {
        val renderLibrary: String
        val useGles: Boolean
        var bypassNamespace = false
        var preloadVk = true
        val glesVersion: Int

        if (renderer.contains(":")) {
            val parts = renderer.split(":")
            if (parts.size >= 3) {
                val providerPath = findNativeLibraryPath(parts[2])
                if (providerPath != null) {
                    try {
                        System.load(providerPath)
                    } catch (e: Throwable) {
                        Log.e("RENDER_LIBRARY", "Failed to System.load provider: $providerPath", e)
                    }
                }
                renderLibrary = parts[1]
            } else if (parts.size == 2) {
                renderLibrary = parts[1]
            } else {
                renderLibrary = "libgl4es_114.so"
            }
            useGles = true
            glesVersion = if (JREUtils.getDetectedVersion() >= 3) 3 else 2
        } else {
            when (renderer) {
                "freedreno_kgsl", "vulkan_zink" -> {
                    if (renderer == "freedreno_kgsl") preloadVk = false
                    renderLibrary = MesaUtils.getPreferredEGL()
                    useGles = false
                    bypassNamespace = true
                    glesVersion = 3
                    if (preloadVk) MojoExec.preloadVulkan()
                }
                "opengles3_ltw" -> {
                    renderLibrary = "libltw.so"
                    useGles = true
                    glesVersion = 3
                }
                "mobileglues" -> {
                    renderLibrary = "libmobileglues.so"
                    useGles = true
                    glesVersion = 3
                }
                "opengles2", "opengles2_5", "opengles3" -> {
                    renderLibrary = "libgl4es_114.so"
                    useGles = true
                    glesVersion = (ExtraCore.getValue(ExtraConstants.OPEN_GL_VERSION) as String).toInt()
                }
                else -> {
                    renderLibrary = "libgl4es_114.so"
                    useGles = true
                    glesVersion = (ExtraCore.getValue(ExtraConstants.OPEN_GL_VERSION) as String).toInt()
                }
            }
        }

        if (!MojoExec.prepareEgl(renderLibrary, bypassNamespace, useGles, glesVersion)) {
            Log.e("RENDER_LIBRARY", "Failed to load renderer $renderLibrary")
            return null
        }
        MesaUtils.destroyZink()
        return renderLibrary
    }

    class RenderersList(@JvmField val rendererIds: List<String>, @JvmField val rendererDisplayNames: Array<String>)
}