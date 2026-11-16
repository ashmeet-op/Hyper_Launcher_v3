package dev.horizon.android

import android.app.Dialog
import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import com.ashmeet.hyperlauncher.components.dialog.HorizonInstallerDialog
import com.ashmeet.hyperlauncher.theme.PojavTheme
import com.ashmeet.hyperlauncher.utils.HyperUtils
import com.ashmeet.hyperlauncher.utils.helper.LauncherComposeHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@Keep
interface HorizonInstallCallback {
    fun onProgress(stage: Int, currentBytes: Long, totalBytes: Long)
    fun onComplete(success: Boolean, errorMessage: String?)
}

@Suppress("KotlinJniMissingFunction")
@Keep
object HorizonInstaller {

    private const val TAG = "HorizonInstaller"
    private var isLibraryLoaded = false

    init {
        try {
            System.loadLibrary("horizon_android")
            isLibraryLoaded = true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to load libhorizon_android.so", e)
        }
    }

    @JvmStatic
    private external fun nativeIsReady(homePath: String): Boolean

    @JvmStatic
    private external fun nativeInstall(homePath: String, callback: HorizonInstallCallback): Boolean

    fun getHorizonHomeDir(context: Context): File {
        return File(context.filesDir, ".local/horizon-client/26.3")
    }

    @Throws(IOException::class)
    private fun copyEngineData(context: Context) {
        val target = File(context.filesDir, "engine-data.zip")
        if (target.isFile && target.length() > 0) return
        val partial = File(context.filesDir, "engine-data.partial")
        context.assets.open("engine-data.zip").use { input ->
            FileOutputStream(partial).use { output ->
                val buffer = ByteArray(65536)
                var count: Int
                while (input.read(buffer).also { count = it } != -1) {
                    output.write(buffer, 0, count)
                }
                output.fd.sync()
            }
        }
        if (!partial.renameTo(target)) {
            throw IOException("Could not install Horizon engine data")
        }
    }

    fun isReady(context: Context): Boolean {
        try {
            copyEngineData(context)
        } catch (e: Throwable) {
            Log.e(TAG, "copyEngineData failed", e)
            return false
        }
        if (isLibraryLoaded) {
            try {
                return nativeIsReady(context.filesDir.absolutePath)
            } catch (e: Throwable) {
                Log.w(TAG, "nativeIsReady error", e)
            }
        }
        val homeDir = getHorizonHomeDir(context)
        if (!homeDir.exists()) return false
        val files = homeDir.listFiles()
        return files != null && files.any { it.name != "installed.flag" }
    }

    fun showInstallDialogAndRun(context: Context, onComplete: () -> Unit) {
        val homeDir = getHorizonHomeDir(context)
        if (!homeDir.exists()) {
            homeDir.mkdirs()
        }

        var statusText by mutableStateOf("Preparing Horizon Engine...")
        var isIndeterminate by mutableStateOf(true)
        var progressValue by mutableFloatStateOf(0f)

        val composeView = ComposeView(context).apply {
            LauncherComposeHelper.ensureViewTreeOwners(this)
            setContent {
                PojavTheme {
                    HorizonInstallerDialog(
                        statusText = statusText,
                        isIndeterminate = isIndeterminate,
                        progress = progressValue
                    )
                }
            }
        }

        val dialog = Dialog(context, android.R.style.Theme_Translucent_NoTitleBar).apply {
            setContentView(composeView)
            setCancelable(false)
        }

        dialog.show()

        Thread {
            try {
                try {
                    copyEngineData(context)
                } catch (e: Throwable) {
                    Log.e(TAG, "copyEngineData failed during install", e)
                    HyperUtils.runOnUiThread {
                        dialog.dismiss()
                        MaterialAlertDialogBuilder(context)
                            .setTitle("Installation Failed")
                            .setMessage("Failed to prepare engine assets: ${e.message}")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                    return@Thread
                }

                if (isLibraryLoaded) {
                    try {
                        nativeInstall(context.filesDir.absolutePath, object : HorizonInstallCallback {
                            override fun onProgress(
                                stage: Int,
                                currentBytes: Long,
                                totalBytes: Long
                            ) {
                                HyperUtils.runOnUiThread {
                                    statusText = when (stage) {
                                        0 -> "Checking Minecraft 26.3 Version..."
                                        1 -> "Downloading Game Resources & Sounds..."
                                        2 -> "Unpacking Verified Files..."
                                        3 -> "Preparing Worlds & Registries..."
                                        4 -> "Downloading Touch Controls..."
                                        5 -> "Finishing Installation..."
                                        else -> "Installing..."
                                    }

                                    if (totalBytes > 0) {
                                        isIndeterminate = false
                                        progressValue = currentBytes.toFloat() / totalBytes.toFloat()
                                    } else {
                                        isIndeterminate = true
                                    }
                                }
                            }

                            override fun onComplete(success: Boolean, errorMessage: String?) {
                                HyperUtils.runOnUiThread {
                                    dialog.dismiss()
                                    if (success) {
                                        onComplete()
                                    } else {
                                        MaterialAlertDialogBuilder(context)
                                            .setTitle("Installation Failed")
                                            .setMessage(errorMessage ?: "Horizon Engine resources could not be prepared.")
                                            .setPositiveButton("OK", null)
                                            .show()
                                    }
                                }
                            }
                        })
                    } catch (e: Throwable) {
                        Log.e(TAG, "nativeInstall failed", e)
                        HyperUtils.runOnUiThread {
                            dialog.dismiss()
                            MaterialAlertDialogBuilder(context)
                                .setTitle("Installation Error")
                                .setMessage(e.message ?: "Unknown error")
                                .setPositiveButton("OK", null)
                                .show()
                        }
                    }
                } else {
                    HyperUtils.runOnUiThread {
                        dialog.dismiss()
                        MaterialAlertDialogBuilder(context)
                            .setTitle("Installation Error")
                            .setMessage("libhorizon_android.so is not loaded.")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error during installation thread", e)
                HyperUtils.runOnUiThread {
                    dialog.dismiss()
                    MaterialAlertDialogBuilder(context)
                        .setTitle("Installation Error")
                        .setMessage(e.message ?: "Unknown error")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }.start()
    }
}
