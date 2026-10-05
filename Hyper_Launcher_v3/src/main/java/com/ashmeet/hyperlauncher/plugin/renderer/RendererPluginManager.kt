package com.ashmeet.hyperlauncher.plugin.renderer

import android.content.Context
import android.content.pm.ApplicationInfo
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperlauncher.plugin.ApkPlugin
import com.ashmeet.hyperlauncher.plugin.ApkPluginManager
import com.ashmeet.hyperlauncher.plugin.cacheAppIcon
import com.ashmeet.hyperlauncher.plugin.renderer_v2.RendererV2PluginManager
import com.ashmeet.hyperlauncher.renderer.Renderers

/**
 * FCL and ZalithLauncher renderer plugin manager, supporting native renderer plugins
 * [FCL Renderer Plugin](https://github.com/FCL-Team/FCLRendererPlugin)
 */
object RendererPluginManager: ApkPluginManager() {
    private val rendererPluginList: MutableList<RendererPlugin> = mutableListOf()

    /**
     * Get all renderers loaded by current renderer plugins
     */
    fun getRendererList(): List<RendererPlugin> = rendererPluginList

    /**
     * Remove specified loaded renderers
     */
    fun removeRenderer(rendererPlugins: Collection<RendererPlugin>) {
        rendererPluginList.removeAll(rendererPlugins)
    }

    /**
     * Currently selected renderer plugin
     */
    val selectedRendererPlugin: RendererPlugin?
        get() {
            val currentRenderer = runCatching {
                Renderers.getCurrentRenderer().getUniqueIdentifier()
            }.getOrNull()
            return rendererPluginList.find { it.packageName == currentRenderer || it.id == currentRenderer }
        }

    /**
     * Get preferred EGL if plugin renderer is selected
     */
    @JvmStatic
    fun getPreferredEgl(): String? {
        return selectedRendererPlugin?.getRendererEGL()
    }

    /**
     * Clear renderer plugins
     */
    fun clearPlugin() {
        rendererPluginList.clear()
    }

    /**
     * Whether the current renderer plugin is configurable
     */
    @JvmStatic
    fun isConfigurablePlugin(rendererUniqueIdentifier: String): Boolean {
        val renderer = rendererPluginList.find { it.packageName == rendererUniqueIdentifier || it.id == rendererUniqueIdentifier }
        return renderer?.isConfigurable == true
    }

    /**
     * Parse ZalithLauncher and FCL renderer plugins
     */
    override fun parseApkPlugin(
        context: Context,
        info: ApplicationInfo,
        loaded: (ApkPlugin) -> Unit
    ) {
        if (info.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
            val metaData = info.metaData ?: return
            if (
                metaData.getBoolean("fclPlugin", false) ||
                metaData.getBoolean("zalithRendererPlugin", false)
            ) {
                val packageManager = context.packageManager
                val packageName = info.packageName
                val appName = info.loadLabel(packageManager).toString()

                // Skip loading legacy architecture if V2 architecture plugin is already loaded
                if (
                    RendererV2PluginManager.getRendererList().any { v2Plugin ->
                        v2Plugin.packageName == packageName
                    }
                ) return

                val rendererString = metaData.getString("renderer") ?: return
                val des = metaData.getString("des") ?: return
                val pojavEnvString = metaData.getString("pojavEnv") ?: return
                val nativeLibraryDir = info.nativeLibraryDir
                val renderer = rendererString.split(":")

                var rendererId: String = renderer[0]
                val envList = mutableMapOf<String, String>()
                val dlopenList = mutableListOf<String>()
                pojavEnvString.split(":").forEach { envString ->
                    if (envString.contains("=")) {
                        val stringList = envString.split("=")
                        val key = stringList[0]
                        val value = stringList[1]
                        when (key) {
                            "POJAV_RENDERER" -> rendererId = value
                            "DLOPEN" -> {
                                value.split(",").forEach { lib ->
                                    dlopenList.add(lib)
                                }
                            }
                            "LIB_MESA_NAME", "MESA_LIBRARY" -> envList[key] = "$nativeLibraryDir/$value"
                            else -> envList[key] = value
                        }
                    }
                }

                val plugin = RendererPlugin(
                    packageName = packageName,
                    id = rendererId,
                    displayName = des,
                    summary = context.getString(R.string.settings_renderer_from_plugins, appName),
                    minMCVer = metaData.getVersionString("minMCVer"),
                    maxMCVer = metaData.getVersionString("maxMCVer"),
                    glName = renderer[1],
                    eglName = renderer[2].progressEglName(nativeLibraryDir),
                    path = nativeLibraryDir,
                    env = envList,
                    dlopen = dlopenList,
                    isConfigurable = packageName in setOf(
                        "com.bzlzhh.plugin.ngg",
                        "com.bzlzhh.plugin.ngg.angleless",
                        "com.fcl.plugin.mobileglues"
                    )
                )

                rendererPluginList.add(plugin)

                runCatching {
                    cacheAppIcon(context, info)
                    ApkPlugin(
                        packageName = packageName,
                        appName = appName,
                        appVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: ""
                    )
                }.getOrNull()?.let { loaded(it) }
            }
        }
    }

    private fun String.progressEglName(libPath: String): String =
        if (startsWith("/")) "$libPath$this"
        else this
}