package com.ashmeet.hyperlauncher.utils

import android.util.Log

object GameOptionsUtils {

    @JvmStatic
    fun parseIntDefault(value: String?, defaultValue: Int): Int {
        if (value == null) return defaultValue
        return value.toIntOrNull() ?: defaultValue
    }

    private fun fixDeathCloud() {
        val info = GLInfoUtils.getGlInfo()
        if (!info.isArm()) return
        val cloudRange = parseIntDefault(MCOptionUtils.get("cloudRange"), 128)
        if (cloudRange <= 64) return
        MCOptionUtils.set("cloudRange", "64")
    }

    private fun disableNarrator() {
        if (parseIntDefault(MCOptionUtils.get("narrator"), 0) == 0) return
        MCOptionUtils.set("narrator", "0")
    }

    private fun disableFullscreen() {
        val fullscreen = MCOptionUtils.get("fullscreen") ?: return
        if (fullscreen == "true") MCOptionUtils.set("fullscreen", "false")
        else if (fullscreen == "1") MCOptionUtils.set("fullscreen", "0")
    }

    private fun enableCape() {
        MCOptionUtils.set("showCape", "true")
        MCOptionUtils.set("modelPart_cape", "true")
    }

    @JvmStatic
    fun fixOptions(isLtw: Boolean) {
        try {
            MCOptionUtils.load()
        } catch (e: Exception) {
            Log.e("HyperUtils", "Failed to load config", e)
        }

        if (isLtw) fixDeathCloud()
        disableFullscreen()
        disableNarrator()
        enableCape()

        try {
            MCOptionUtils.save()
        } catch (e: Exception) {
            Log.e("HyperUtils", "Failed to save config", e)
        }
    }
}
