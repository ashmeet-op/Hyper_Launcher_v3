package com.ashmeet.hyperlauncher.utils.horizon

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.ashmeet.hyperbackend.authenticator.accounts.Account
import com.ashmeet.hyperbackend.authenticator.accounts.Accounts
import com.ashmeet.hyperbackend.instances.DisplayInstance
import com.ashmeet.hyperbackend.instances.Instance
import com.ashmeet.hyperlauncher.utils.Architecture
import com.ashmeet.hyperlauncher.utils.HyperUtils
import dev.horizon.android.HorizonActivity
import dev.horizon.android.HorizonInstaller
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter

object HorizonUtils {

    private const val TAG = "HorizonUtils"
    const val HORIZON_VERSION_ID = "26.3-horizon"
    const val HORIZON_RUNTIME = "HORIZON_NATIVE"

    @JvmStatic
    fun isHorizonInstance(instance: DisplayInstance?): Boolean {
        if (instance == null) return false
        if (instance is Instance) {
            val rt = instance.selectedRuntime?.lowercase() ?: ""
            if (rt == HORIZON_RUNTIME.lowercase() || rt.contains("horizon")) return true
        }
        val vId = instance.versionId?.lowercase() ?: ""
        return vId.contains("horizon")
    }

    @JvmStatic
    fun isHorizonInstance(versionId: String?, selectedRuntime: String?): Boolean {
        val vId = versionId?.lowercase() ?: ""
        val rt = selectedRuntime?.lowercase() ?: ""
        return vId.contains("horizon") || rt == HORIZON_RUNTIME.lowercase() || rt.contains("horizon")
    }

    @JvmStatic
    fun launchHorizon(context: Context, instance: Instance?): Boolean {
        val arch = Architecture.getDeviceArchitecture()

        if (arch == Architecture.UNSUPPORTED_ARCH) {
            Toast.makeText(context, "Unsupported device architecture for Horizon Rust engine.", Toast.LENGTH_LONG).show()
            return false
        }

        val currentAccount = Accounts.getCurrent()

        if (currentAccount == null) {
            Toast.makeText(context, "No account selected. Please sign in or create an offline profile.", Toast.LENGTH_LONG).show()
            return false
        }

        if (!HorizonInstaller.isReady(context)) {
            HorizonInstaller.showInstallDialogAndRun(context) {
                executeLaunch(context, instance, currentAccount)
            }
            return true
        }

        return executeLaunch(context, instance, currentAccount)
    }

    private fun executeLaunch(context: Context, instance: Instance?, account: Account): Boolean {
        return try {
            val gameDir = instance?.gameDirectory ?: File(HyperUtils.DIR_GAME_HOME, "default-horizon")

            if (!gameDir.exists()) {
                gameDir.mkdirs()
            }

            File(gameDir, "saves").mkdirs()
            File(gameDir, "resourcepacks").mkdirs()

            val resourcesDir = HorizonInstaller.getHorizonHomeDir(context)
            val authArgs = HorizonAuth.prepareAuthArgs(context, account)
            val cmdArgs = mutableListOf<String>()

            cmdArgs.add("--resources")
            cmdArgs.add(resourcesDir.absolutePath)
            cmdArgs.add("--game-dir")
            cmdArgs.add(gameDir.absolutePath)
            cmdArgs.addAll(authArgs)

            val renderer = instance?.getLaunchRenderer() ?: "vulkan"
            val launchConfig = JSONObject().apply {
                put("args", JSONArray(cmdArgs))
                put("renderer", if (renderer.lowercase().contains("opengl")) "opengl" else "vulkan")
                put("render_scale", 1.0)
                put("input_host", "keyboard_mouse")
                put("orientation", "sensor_landscape")
                put("immersive", true)
                put("avoid_cutout", true)
            }

            val configFile = File(context.filesDir, "launch_config.json")
            FileWriter(configFile).use { writer ->
                writer.write(launchConfig.toString(2))
            }
            Log.i(TAG, "Wrote launch_config.json to ${configFile.absolutePath}")

            val instanceConfigFile = File(gameDir, "launch_config.json")
            FileWriter(instanceConfigFile).use { writer ->
                writer.write(launchConfig.toString(2))
            }

            val intent = Intent(context, HorizonActivity::class.java)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start HorizonActivity", e)
            Toast.makeText(context, "Failed to start Horizon engine: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }
}
