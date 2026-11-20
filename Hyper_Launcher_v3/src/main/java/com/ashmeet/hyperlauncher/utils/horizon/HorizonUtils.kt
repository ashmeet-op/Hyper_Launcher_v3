package com.ashmeet.hyperlauncher.utils.horizon

import android.content.Context
import android.content.Intent
import com.ashmeet.hyperbackend.instances.Instance

object HorizonUtils {
    const val HORIZON_VERSION_ID = "horizon"
    const val HORIZON_RUNTIME = "horizon"

    @JvmStatic
    fun isHorizonInstance(instance: Instance?): Boolean {
        return instance != null && isHorizonInstance(instance.versionId, instance.selectedRuntime)
    }

    @JvmStatic
    fun isHorizonInstance(versionId: String?, runtimeName: String?): Boolean {
        if (versionId != null && versionId.equals(HORIZON_VERSION_ID, ignoreCase = true)) {
            return true
        }
        if (runtimeName != null && runtimeName.lowercase().contains(HORIZON_RUNTIME)) {
            return true
        }
        return false
    }

    @JvmStatic
    fun launchHorizon(context: Context, instance: Instance) {
        try {
            val intent = Intent(context, com.ashmeet.hyperbackend.game.GameActivity::class.java).apply {
                putExtra(com.ashmeet.hyperbackend.game.GameActivity.INTENT_LAUNCH_VERSION, instance.versionId ?: HORIZON_VERSION_ID)
                putExtra(com.ashmeet.hyperbackend.game.GameActivity.INTENT_LAUNCH_CLASSPATH, arrayOf<java.io.File?>())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
