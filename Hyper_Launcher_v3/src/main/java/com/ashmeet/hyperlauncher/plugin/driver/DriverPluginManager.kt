package com.ashmeet.hyperlauncher.plugin.driver

import android.content.Context
import android.content.pm.ApplicationInfo
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperlauncher.plugin.ApkPlugin
import com.ashmeet.hyperlauncher.plugin.ApkPluginManager
import com.ashmeet.hyperlauncher.plugin.cacheAppIcon

/**
 * FCL Driver Plugin Manager
 * [FCL DriverPlugin.kt](https://github.com/FCL-Team/FoldCraftLauncher/blob/main/FCLauncher/src/main/java/com/tungsten/fclauncher/plugins/DriverPlugin.kt)
 */
object DriverPluginManager: ApkPluginManager() {
    private val driverList: MutableList<Driver> = mutableListOf()

    fun getDriverList(): List<Driver> = driverList.toList()

    fun getDriver(driverId: String): Driver =
        driverList.find { it.id == driverId } ?: driverList[0]

    /**
     * Initialize driver
     */
    fun initDriver(context: Context) {
        driverList.clear()
        val applicationInfo = context.applicationInfo
        driverList.add(
            Driver(
                id = "turnip",
                appName = "",
                appVersion = "",
                name = "Turnip",
                path = applicationInfo.nativeLibraryDir,
                isLauncher = true
            )
        )
    }

    /**
     * Generic FCL plugin parser
     */
    override fun parseApkPlugin(
        context: Context,
        info: ApplicationInfo,
        loaded: (ApkPlugin) -> Unit
    ) {
        if (info.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
            val metaData = info.metaData ?: return
            if (metaData.getBoolean("fclPlugin", false)) {
                val driver = metaData.getString("driver") ?: return
                val nativeLibraryDir = info.nativeLibraryDir

                val packageManager = context.packageManager
                val packageName = info.packageName
                val appName = info.loadLabel(packageManager).toString()
                val appVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: ""

                val plugin = Driver(
                    id = packageName,
                    appName = appName,
                    appVersion = appVersion,
                    name = driver,
                    summary = context.getString(R.string.settings_renderer_from_plugins, appName),
                    path = nativeLibraryDir,
                    isLauncher = false
                )

                driverList.add(plugin)

                runCatching {
                    cacheAppIcon(context, info)
                    loaded(plugin)
                }
            }
        }
    }
}