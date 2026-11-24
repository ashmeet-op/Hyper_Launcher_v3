package com.ashmeet.hyperlauncher.utils.horizon

import android.content.Context
import android.content.Intent
import android.util.Log
import com.ashmeet.hyperbackend.instances.Instance
import dev.horizon.android.HorizonActivity
import dev.horizon.android.HorizonInstaller

object HorizonUtils {
    private const val TAG = "HorizonUtils"
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
    fun launchHorizon(context: Context) {
        try {
            val homeDir = context.filesDir.absolutePath
            val cacheDir = context.cacheDir.absolutePath

            try {
                if (!HorizonInstaller.isReady(homeDir)) {
                    Log.i(TAG, "Horizon engine not ready; executing installation...")
                    HorizonInstaller.install(homeDir, cacheDir, object : HorizonInstaller.Listener {
                        override fun onProgress(stageIndex: Int, label: String?, done: Long, total: Long) {
                            Log.d(TAG, "Horizon install progress [$stageIndex/${HorizonInstaller.STAGE_COUNT}]: $label ($done/$total)")
                        }
                    })
                }
            } catch (e: Throwable) {
                Log.w(TAG, "HorizonInstaller check/install threw; proceeding to launch", e)
            }

            startHorizonActivity(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching Horizon instance", e)
            startHorizonActivity(context)
        }
    }

    private fun startHorizonActivity(context: Context) {
        try {
            val intent = Intent(context, HorizonActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start HorizonActivity", e)
        }
    }
}
