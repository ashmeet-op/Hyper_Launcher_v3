package com.ashmeet.hyperlauncher.utils.horizon

import android.content.Context
import android.util.Log
import com.ashmeet.hyperbackend.authenticator.AuthType
import com.ashmeet.hyperbackend.authenticator.accounts.Account
import org.json.JSONObject
import java.io.File
import java.io.FileWriter

object HorizonAuth {

    private const val TAG = "HorizonAuth"

    /**
     * Prepares command-line arguments and temporary session files for Horizon.
     * Returns a list of argument pairs/flags to pass to Horizon engine.
     */
    fun prepareAuthArgs(context: Context, account: Account?): List<String> {
        val args = mutableListOf<String>()

        if (account == null) {
            Log.w(TAG, "No account provided, defaulting to Steve (offline)")
            args.add("--name")
            args.add("Steve")
            return args
        }

        val username = if (account.username.isNullOrEmpty()) "Steve" else account.username
        args.add("--name")
        args.add(username)

        if (account.authType == AuthType.MICROSOFT || account.isMicrosoft) {
            try {
                val sessionsDir = File(context.filesDir, "sessions")
                if (!sessionsDir.exists()) {
                    sessionsDir.mkdirs()
                }

                val sessionFile = File(sessionsDir, "session-${account.profileId}.json")
                val json = JSONObject().apply {
                    put("name", username)
                    put("uuid", account.profileId.replace("-", ""))
                    put("access_token", if (account.accessToken.isNullOrEmpty()) "0" else account.accessToken)
                    put("user_type", "msa")
                    put("xuid", if (account.xuid.isNullOrEmpty()) "" else account.xuid)
                }

                FileWriter(sessionFile).use { writer ->
                    writer.write(json.toString(2))
                }

                args.add("--session-file")
                args.add(sessionFile.absolutePath)
                Log.i(TAG, "Created Microsoft auth session file: ${sessionFile.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create session file for Microsoft account", e)
            }
        }

        return args
    }
}
