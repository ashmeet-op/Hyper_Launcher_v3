package com.ashmeet.hyperlauncher.plugin.natives

import com.ashmeet.hyperlauncher.plugin.ApkPlugin
import com.ashmeet.hyperlauncher.plugin.Plugin

class NativePlugin(
    packageName: String,
    appName: String,
    appVersion: String,
    val displayName: String,
    val minMCVer: String? = null,
    val maxMCVer: String? = null,
    val path: String,
    val envList: List<String>
): ApkPlugin(
    packageName = packageName,
    appName = appName,
    appVersion = appVersion
), Plugin {
    override fun getIdentifier(): String = packageName
    override fun getNativeLibPath(): String = path
}
