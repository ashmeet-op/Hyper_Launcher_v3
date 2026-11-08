package com.ashmeet.hyperlauncher.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri

object Discord {
    private const val TAG = "Discord"
    private var isInitialized = false

    init {
        try {
            System.loadLibrary("discord_bridge")
            isInitialized = true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to load discord_bridge native library", e)
        }
    }

    external fun init(appId: Long)
    external fun setPresence(details: String, state: String)
    external fun runCallbacks()
    external fun authorize()

    fun safeInit(appId: Long = 1557366086293520466L) {
        if (!isInitialized) return
        try {
            init(appId)
        } catch (e: Throwable) {
            Log.e(TAG, "Error initializing Discord RPC", e)
        }
    }

    fun safeSetPresence(details: String, state: String) {
        if (!isInitialized) return
        try {
            setPresence(details, state)
        } catch (e: Throwable) {
            Log.e(TAG, "Error setting Discord presence", e)
        }
    }

    fun safeRunCallbacks() {
        if (!isInitialized) return

        try {
            runCallbacks()
        }

        catch (_: Throwable) {

        }
    }

    fun safeAuthorize(context: Context) {
        try {
            if (isInitialized) {
                authorize()
            }
            val authUrl = "https://discord.com/api/oauth2/authorize?client_id=1557366086293520466&response_type=code&scope=rpc.activities.write%20identify"
            val intent = Intent(Intent.ACTION_VIEW, authUrl.toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Throwable) {
            Log.e(TAG, "Error during Discord authorization", e)
        }
    }
}
