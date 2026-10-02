package com.ashmeet.hyperlauncher.plugins.natives

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.ashmeet.hyperlauncher.plugins.interfaces.NativePlugin
import net.kdt.pojavlaunch.modloaders.ComparableVersionString
import java.io.File
import java.util.HashMap

@Suppress("unused")
class LibraryPlugin private constructor(
    val appId: String,
    val libraryPath: String,
    private val metaData: Bundle?
) : NativePlugin {
    override fun getPaths(): Array<String> = arrayOf(libraryPath)
    override fun getJVMEnv(): Map<String, String> = getRendererEnv().value
    override val name: String? get() = rendererName
    override val rendererName: String?
        get() = getMetadataString("displayName") ?: getMetadataString(METADATA_FCL_RENDERER) ?: getMetadataString(METADATA_FCL_DESCRIPTION)
    override val driverName: String? get() = getMetadataString(METADATA_FCL_DRIVER)
    override val displayName: String? get() = rendererName

    override fun supportsVersion(mcVersion: String?): Boolean {
        if (mcVersion == null) return true
        val minVerStr = getMinMCVersion()
        val maxVerStr = getMaxMCVersion()
        val current = ComparableVersionString.parse(mcVersion)
        if (!current.isValid) return true

        if (!minVerStr.isNullOrEmpty()) {
            val min = ComparableVersionString.parse(minVerStr)
            if (min.isValid && current < min) return false
        }

        if (!maxVerStr.isNullOrEmpty()) {
            val max = ComparableVersionString.parse(maxVerStr)
            if (max.isValid && current > max) return false
        }

        return true
    }

    fun getId(): String = appId

    fun resolve(library: String): File {
        return File(libraryPath, library)
    }

    fun getIdentifier(): String = appId
    fun getNativeLibPath(): String = libraryPath

    fun getRendererId(): String {
        return getMetadataString("id") ?: getMetadataString(METADATA_FCL_RENDERER) ?: appId
    }

    fun getUniqueIdentifier(): String = appId

    fun getMinMCVersion(): String? {
        return getMetadataString("minMCVer") ?: getMetadataString(METADATA_FCL_MIN_MC_VER)
    }

    fun getMaxMCVersion(): String? {
        return getMetadataString("maxMCVer") ?: getMetadataString(METADATA_FCL_MAX_MC_VER)
    }

    fun getRendererEnv(): Lazy<Map<String, String>> = lazy {
        val envMap = HashMap<String, String>()
        val envString = getMetadataString("environment") ?: getMetadataString(METADATA_FCL_ENVIRONMENT)
        val boatEnv = getMetadataString("boatEnv") ?: getMetadataString(METADATA_FCL_BOAT_ENV)
        val pojavEnv = getMetadataString("pojavEnv") ?: getMetadataString(METADATA_FCL_POJAV_ENV)
        parseEnvString(envString, libraryPath, envMap)
        parseEnvString(boatEnv, libraryPath, envMap)
        parseEnvString(pojavEnv, libraryPath, envMap)
        envMap
    }

    fun getDlopenLibrary(): Lazy<List<String>> = lazy {
        val dlopenStr = getMetadataString("dlopen") ?: ""
        if (dlopenStr.isBlank()) {
            emptyList()
        } else {
            dlopenStr.split("[:; ,]+".toRegex())
                .filter { it.isNotBlank() }
                .map { lib -> "$libraryPath/${lib.trim()}" }
        }
    }

    fun getRendererLibrary(): String {
        val glName = getMetadataString("glName") ?: getMetadataString("rendererLibrary") ?: "libGL.so"
        return "$libraryPath/$glName"
    }

    fun getRendererEGL(): String {
        val eglName = getMetadataString("eglName") ?: getMetadataString("rendererEGL") ?: "libEGL.so"
        return eglName
    }

    fun isConfigurable(): Boolean {
        val conf = getMetadataString("isConfigurable")
        return conf?.lowercase() == "true"
    }

    @Suppress("DEPRECATION")
    private fun getMetadataString(key: String): String? {
        return metaData?.get(key)?.toString()
    }

    private fun parseEnvString(envString: String?, libDir: String, envMap: MutableMap<String, String>) {
        if (envString.isNullOrEmpty()) return
        val pairs = envString.split("[:; \t\r\n]+".toRegex()).toTypedArray()
        for (pair in pairs) {
            if (pair.isEmpty()) continue
            val kv = pair.split("=".toRegex(), 2).toTypedArray()
            if (kv.size == 2) {
                val key = kv[0].trim()
                var value = kv[1].trim()
                if (value.contains("{nativeLibraryDir}")) {
                    value = value.replace("{nativeLibraryDir}", libDir)
                } else if (!value.startsWith("/") && value.endsWith(".so")) {
                    val fullLib = File(libDir, value)
                    if (fullLib.exists()) {
                        value = fullLib.absolutePath
                    }
                }
                if (key.isNotEmpty()) {
                    envMap[key] = value
                }
            }
        }
    }

    companion object {
        private const val TAG = "LibraryPlugin"
        const val METADATA_FCL_PLUGIN = "FCLNativePlugin"
        const val METADATA_FCL_PLUGIN_ALT = "fclPlugin"
        const val METADATA_FCL_DESCRIPTION = "des"
        const val METADATA_FCL_ENVIRONMENT = "environment"
        const val METADATA_FCL_RENDERER = "renderer"
        const val METADATA_FCL_BOAT_ENV = "boatEnv"
        const val METADATA_FCL_POJAV_ENV = "pojavEnv"
        const val METADATA_FCL_MIN_MC_VER = "minMCVer"
        const val METADATA_FCL_MAX_MC_VER = "maxMCVer"
        const val METADATA_FCL_DRIVER = "driver"
        const val METADATA_POJAV_PLUGIN_TYPE = "net.kdt.pojavlaunch.PLUGIN_TYPE"
        const val METADATA_POJAV_PLUGIN_LIBS = "net.kdt.pojavlaunch.PLUGIN_LIBS"
        const val ID_ANGLE_PLUGIN = "git.mojo.angle"
        const val ID_FFMPEG_PLUGIN = "git.mojo.ffmpeg"
        const val ID_ZINK_PLUGIN = "git.mojo.zink"
        const val ID_MESA_PLUGIN = "git.mojo.mesa"

        @JvmStatic
        fun fromApplicationInfo(info: ApplicationInfo): LibraryPlugin {
            return LibraryPlugin(info.packageName, info.nativeLibraryDir, info.metaData)
        }

        @JvmStatic
        fun discoverPlugin(ctx: Context, appId: String): LibraryPlugin? {
            return try {
                val info = ctx.packageManager.getApplicationInfo(appId, PackageManager.GET_META_DATA)
                fromApplicationInfo(info)
            } catch (_: PackageManager.NameNotFoundException) {
                Log.i(TAG, "Plugin not installed: $appId")
                null
            } catch (e: Exception) {
                Log.e(TAG, "Plugin discover failed: ${e.message}")
                null
            }
        }

        @SuppressLint("QueryAllPackages")
        @JvmStatic
        fun discoverAllPlugins(ctx: Context): List<LibraryPlugin> {
            val plugins = mutableListOf<LibraryPlugin>()
            val pm = ctx.packageManager

            val installedApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledApplications(PackageManager.GET_META_DATA)
            }

            for (info in installedApps) {
                if (info.metaData != null && (
                    info.metaData.containsKey(METADATA_FCL_PLUGIN) ||
                    info.metaData.containsKey(METADATA_FCL_PLUGIN_ALT) ||
                    info.metaData.containsKey(METADATA_POJAV_PLUGIN_TYPE)
                )) {
                    plugins.add(fromApplicationInfo(info))
                }
            }
            return plugins
        }
    }

    fun getMetaData(): Bundle = metaData ?: Bundle()

    fun resolveAbsolutePath(library: String): String {
        return File(libraryPath, library).absolutePath
    }

    fun checkLibraries(vararg libs: String): Boolean {
        for (lib in libs) {
            if (!File(libraryPath, lib).exists()) return false
        }
        return true
    }
}
