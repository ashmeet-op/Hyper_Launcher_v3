package com.ashmeet.hyperlauncher.utils

import android.util.Log
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.milliseconds

object DiscordRpcManager {
    private const val TAG = "DiscordRpcManager"
    private var rpcJob: Job? = null

    fun startRpc(details: String = "Playing Minecraft", state: String = "HyperLauncher", largeImage: String? = null) {
        if (!LauncherPreferences.PREF_ENABLE_RPC) return

        stopRpc()

        // Initialize official Discord RPC via JNI (native C++ implementation)
        val appId = 1557366086293520466L
        Discord.safeInit(appId)
        Discord.safeSetPresence(details, state)

        rpcJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                while (isActive) {
                    // Call native JNI runCallbacks() regularly every ~100 ms as required by Discord SDK
                    Discord.safeRunCallbacks()
                    delay(100.milliseconds)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed in native Discord RPC loop", e)
            }
        }
    }

    fun stopRpc() {
        rpcJob?.cancel()
        rpcJob = null
    }
}
