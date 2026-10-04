package com.ashmeet.hyperlauncher.plugin

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle

abstract class ApkPluginManager {
    abstract fun parseApkPlugin(
        context: Context,
        info: ApplicationInfo,
        loaded: (ApkPlugin) -> Unit = {}
    )

    protected fun Bundle.getVersionString(key: String): String? {
        return if (containsKey(key)) {
            runCatching {
                when (val o = get(key)) {
                    is String -> o
                    is Number -> o.toString()
                    else -> null
                }
            }.getOrNull()?.takeIf { it.isNotBlank() }
        } else null
    }
}