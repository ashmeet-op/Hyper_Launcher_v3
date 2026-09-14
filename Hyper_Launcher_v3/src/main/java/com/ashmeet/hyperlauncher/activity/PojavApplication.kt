package com.ashmeet.hyperlauncher.activity

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.ashmeet.hyperlauncher.plugins.manager.NativePluginManager
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.utils.Architecture
import com.ashmeet.hyperlauncher.utils.LoggerProxy
import com.ashmeet.hyperlauncher.utils.Tools
import com.ashmeet.hyperlauncher.utils.translation.Translator
import net.ashmeet.hyperlauncher.BuildConfig
import net.kdt.pojavlaunch.lifecycle.ContextExecutor
import net.kdt.pojavlaunch.tasks.AsyncAssetManager
import net.kdt.pojavlaunch.tasks.MoJsonDownloader
import net.kdt.pojavlaunch.utils.FileUtils
import net.kdt.pojavlaunch.utils.LocaleUtils
import java.io.File
import java.io.PrintStream
import java.text.DateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class PojavApplication : Application() {

    private fun installFatalErrorHandler() {
        Thread.setDefaultUncaughtExceptionHandler { _, th ->
            val storagePermAllowed = (Build.VERSION.SDK_INT >= 29 || ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED) && Tools.checkStorageRoot(this)
            val crashFile = File(if (storagePermAllowed) Tools.DIR_GAME_HOME else Tools.DIR_DATA, "latestcrash.txt")
            try {
                // Write to file, since some devices may not able to show error
                FileUtils.ensureParentDirectory(crashFile)
                PrintStream(crashFile).use { crashStream ->
                    crashStream.append("Hyper crash report\n")
                    crashStream.append(" - Time: ").append(DateFormat.getDateTimeInstance().format(Date()))
                        .append("\n")
                    crashStream.append(" - Device: ").append(Build.PRODUCT).append(" ").append(Build.MODEL)
                        .append("\n")
                    crashStream.append(" - Android version: ").append(Build.VERSION.RELEASE).append("\n")
                    crashStream.append(" - Crash stack trace:\n")
                    crashStream.append(" - Launcher version: ").append(BuildConfig.VERSION_NAME).append("\n")
                    crashStream.append(Log.getStackTraceString(th))
                }
            } catch (throwable: Throwable) {
                Log.e(CRASH_REPORT_TAG, " - Exception attempt saving crash stack trace:", throwable)
                Log.e(CRASH_REPORT_TAG, " - The crash stack trace was:", th)
            }

            FatalErrorActivity.showError(this, crashFile.absolutePath, storagePermAllowed, th)
            Tools.fullyExit()
        }
    }

    @SuppressLint("SuspiciousIndentation")
    override fun onCreate() {
        ContextExecutor.setApplication(this)
        // Disable fatal errors on gplay. This is necessary so that Google can collect crash report data and send it to me
        // (where I can find the cause and fix it)
        if (BuildConfig.BUILD_TYPE != "gplay") installFatalErrorHandler()

        try {
            super.onCreate()
            if (Tools.checkStorageRoot(this)) {
                // Implicitly initializes early constants and storage constants.
                // Required to run the main activity properly.
                LauncherPreferences.loadPreferences(this)
            } else {
                // In other cases, only initialize enough for the basic most basics to work
                // and not explode.
                Tools.initEarlyConstants(this)
            }

            System.loadLibrary("pojavexec")
            LoggerProxy.init()

            Tools.DEVICE_ARCHITECTURE = Architecture.getDeviceArchitecture()
            NativePluginManager.discoverAarPlugins(this)
            //Force x86 lib directory for Asus x86 based zenfones
            if (Architecture.isx86Device() && Architecture.is32BitsDevice()) {
                val info = applicationInfo
                val originalJNIDirectory = info.nativeLibraryDir
                info.nativeLibraryDir = originalJNIDirectory.substring(
                    0,
                    originalJNIDirectory.lastIndexOf("/")
                ).plus("/x86")
            }
            MoJsonDownloader.prepareSubstitutionMap(assets)
            AsyncAssetManager.unpackRuntime(assets)
            Translator.init(this)
        } catch (throwable: Throwable) {
            val ferrorIntent = Intent(this, FatalErrorActivity::class.java)
            ferrorIntent.putExtra("throwable", throwable)
            ferrorIntent.flags = FLAG_ACTIVITY_NEW_TASK
            startActivity(ferrorIntent)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        ContextExecutor.clearApplication()
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleUtils.setLocale(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LocaleUtils.setLocale(this)
    }

    companion object {
        const val CRASH_REPORT_TAG = "HyperCrashReport"

        @JvmField
        val sExecutorService: ExecutorService = ThreadPoolExecutor(
            4, 4, 500, TimeUnit.MILLISECONDS, LinkedBlockingQueue()
        )
    }
}
