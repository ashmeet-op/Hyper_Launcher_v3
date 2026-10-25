package com.ashmeet.hyperlauncher.utils

import android.util.Log
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.my.kizzyrpc.KizzyRPC
import com.my.kizzyrpc.model.Activity
import com.my.kizzyrpc.model.Assets
import com.my.kizzyrpc.model.Metadata
import com.my.kizzyrpc.model.Timestamps
import kotlinx.coroutines.*
import net.ashmeet.hyperlauncher.BuildConfig
import kotlin.time.Duration.Companion.milliseconds

object DiscordRpcManager {
    private const val TAG = "DiscordRpcManager"
    private var rpcJob: Job? = null
    private var kizzyRPC: KizzyRPC? = null

    fun startRpc(details: String = "Playing Minecraft", state: String = "HyperLauncher") {
        if (!LauncherPreferences.PREF_ENABLE_RPC) return
        var token = LauncherPreferences.PREF_DISCORD_TOKEN
        if (token.isNullOrBlank()) {
            token = BuildConfig.DC_KEY
        }
        if (token.isBlank()) {
            Log.w(TAG, "Discord token is blank, cannot start RPC")
            return
        }

        stopRpc()

        rpcJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                kizzyRPC = KizzyRPC(token)
                val startTime = System.currentTimeMillis()
                while (isActive) {
                    kizzyRPC?.setActivity(
                        activity = Activity(
                            applicationId = "1557366086293520466",
                            name = "Hyper Launcher",
                            details = details,
                            state = state,
                            type = 0,
                            timestamps = Timestamps(
                                start = startTime,
                                end = null
                            ),
                            assets = Assets(
                                largeImage = null,
                                smallImage = null,
                                largeText = "HyperLauncher",
                                smallText = null
                            ),
                            buttons = listOf("Download HyperLauncher"),
                            metadata = Metadata(listOf("https://github.com"))
                        ),
                        status = "online",
                        since = startTime
                    )
                    delay(15000.milliseconds)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update Discord RPC", e)
            }
        }
    }

    fun stopRpc() {
        rpcJob?.cancel()
        rpcJob = null
        kizzyRPC = null
    }
}
