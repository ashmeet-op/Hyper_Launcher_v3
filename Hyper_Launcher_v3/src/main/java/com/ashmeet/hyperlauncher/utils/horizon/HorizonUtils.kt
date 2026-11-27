package com.ashmeet.hyperlauncher.utils.horizon

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.graphics.drawable.toDrawable
import com.ashmeet.hyperbackend.instances.Instance
import com.ashmeet.hyperlauncher.components.dialog.HorizonInstallerDialog
import com.ashmeet.hyperlauncher.theme.PojavTheme
import com.ashmeet.hyperlauncher.utils.HyperUtils
import com.ashmeet.hyperlauncher.utils.helper.LauncherComposeHelper
import dev.horizon.android.HorizonActivity
import dev.horizon.android.HorizonInstaller
import java.io.File

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
    @JvmOverloads
    fun launchHorizon(context: Context, configureIntent: ((Intent) -> Unit)? = null) {
        val homeDir = File(context.filesDir, "horizon").absolutePath
        val cacheDir = context.cacheDir.absolutePath

        if (HorizonInstaller.isReady(homeDir)) {
            Log.i(TAG, "Horizon engine is ready; launching activity instantly...")
            startHorizonActivity(context, configureIntent)
            return
        }

        var statusTextState by mutableStateOf("Initializing Horizon installation...")
        var isIndeterminateState by mutableStateOf(true)
        var progressState by mutableFloatStateOf(0f)
        var installerDialog: Dialog? = null

        HyperUtils.runOnUiThread {
            try {
                val dialog = Dialog(context).apply {
                    setCancelable(false)
                    setCanceledOnTouchOutside(false)
                    window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
                }
                val composeView = ComposeView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    LauncherComposeHelper.ensureViewTreeOwners(this)
                    setContent {
                        PojavTheme {
                            HorizonInstallerDialog(
                                statusText = statusTextState,
                                isIndeterminate = isIndeterminateState,
                                progress = progressState
                            )
                        }
                    }
                }
                dialog.setContentView(composeView)
                dialog.show()
                installerDialog = dialog
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to create installer dialog", e)
            }
        }

        Thread {
            try {
                Log.i(TAG, "Horizon engine not ready; installing engine resources...")
                val installError = HorizonInstaller.install(homeDir, cacheDir, object : HorizonInstaller.Listener {
                    override fun onProgress(stageIndex: Int, label: String?, done: Long, total: Long) {
                        val stageName = label ?: "Stage ${stageIndex + 1}/${HorizonInstaller.STAGE_COUNT}"
                        Log.d(TAG, "Horizon install progress [$stageIndex/${HorizonInstaller.STAGE_COUNT}]: $stageName ($done/$total)")
                        val stageRatio = if (total > 0) (done.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
                        val overall = ((stageIndex.toFloat() + stageRatio) / HorizonInstaller.STAGE_COUNT.toFloat()).coerceIn(0f, 1f)

                        HyperUtils.runOnUiThread {
                            statusTextState = "$stageName (${(overall * 100).toInt()}%)"
                            isIndeterminateState = (total <= 0)
                            progressState = overall
                        }
                    }
                })

                HyperUtils.runOnUiThread {
                    try {
                        installerDialog?.dismiss()
                    } catch (_: Throwable) {}

                    if (installError != null) {
                        Log.e(TAG, "Horizon engine installation failed: $installError")
                        Toast.makeText(context, "Horizon installation failed: $installError", Toast.LENGTH_LONG).show()
                    } else {
                        Log.i(TAG, "Horizon engine installation completed successfully. Launching Horizon instantly...")
                        startHorizonActivity(context, configureIntent)
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error installing/launching Horizon instance", e)
                HyperUtils.runOnUiThread {
                    try {
                        installerDialog?.dismiss()
                    } catch (_: Throwable) {}
                    Toast.makeText(context, "Failed to initialize Horizon engine: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }


    @JvmStatic
    @JvmOverloads
    fun launchHorizonQuickPlayWorld(context: Context, worldName: String, gameDir: String? = null) {
        launchHorizon(context) { intent ->
            intent.putExtra(HorizonActivity.EXTRA_QUICK_PLAY_WORLD, worldName)
            if (gameDir != null) {
                intent.putExtra(HorizonActivity.EXTRA_GAME_DIR, gameDir)
            }
        }
    }

    @Suppress("unused")
    @JvmStatic
    @JvmOverloads
    fun launchHorizonQuickPlayServer(context: Context, serverAddress: String, gameDir: String? = null) {
        launchHorizon(context) { intent ->
            intent.putExtra(HorizonActivity.EXTRA_QUICK_PLAY_SERVER, serverAddress)
            if (gameDir != null) {
                intent.putExtra(HorizonActivity.EXTRA_GAME_DIR, gameDir)
            }
        }
    }

    private fun startHorizonActivity(context: Context, configureIntent: ((Intent) -> Unit)? = null) {
        try {
            val intent = Intent(context, HorizonActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                configureIntent?.invoke(this)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start HorizonActivity", e)
        }
    }
}
