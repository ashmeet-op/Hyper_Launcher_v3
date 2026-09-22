package com.ashmeet.hyperlauncher.utils

import android.app.Activity
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import androidx.core.net.toUri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import android.provider.OpenableColumns
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.ashmeet.hyperlauncher.components.dialog.SimpleAlertDialog
import com.ashmeet.hyperlauncher.components.dialog.GenericComposeDialogFragment
import com.ashmeet.hyperlauncher.plugins.manager.HyperPluginManager
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.ashmeet.hyperlauncher.BuildConfig
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperlauncher.utils.drawable.InsetBackground
import net.kdt.pojavlaunch.JVersionList
import net.kdt.pojavlaunch.Logger
import com.ashmeet.hyperlauncher.activity.MissingStorageActivity
import com.ashmeet.hyperlauncher.activity.PojavApplication
import com.ashmeet.hyperlauncher.activity.ShowErrorActivity
import net.kdt.pojavlaunch.awt.AWTActivity
import net.kdt.pojavlaunch.game.GameActivity
import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.lifecycle.ContextExecutor
import net.kdt.pojavlaunch.lifecycle.ContextExecutorTask
import net.kdt.pojavlaunch.multirt.MultiRTUtils
import net.kdt.pojavlaunch.utils.FileUtils
import net.kdt.pojavlaunch.utils.GLInfoUtils
import net.kdt.pojavlaunch.utils.HashUtils
import net.kdt.pojavlaunch.utils.memory.MemoryHoleFinder
import net.kdt.pojavlaunch.utils.memory.SelfMapsParser
import net.kdt.pojavlaunch.value.DependentLibrary
import net.kdt.pojavlaunch.value.LibraryArtifact
import org.apache.commons.codec.binary.Hex
import org.apache.commons.io.IOUtils
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.ref.WeakReference
import java.net.URLConnection
import java.nio.charset.StandardCharsets

object Tools {
    const val MAVEN_CENTRAL = "https://maven-central-eu.storage-download.googleapis.com/maven2/"
    const val BYTE_TO_MB = 1024f * 1024f
    @JvmField
    val MAIN_HANDLER = Handler(Looper.getMainLooper())
    @JvmField
    var APP_NAME = "HyperLauncher"

    @JvmField
    val GLOBAL_GSON: Gson = GsonBuilder().setPrettyPrinting().create()

    const val URL_HOME = "https://hyperxlauncher.vercel.app/"
    @JvmField
    var NATIVE_LIB_DIR: String? = null
    @JvmField
    var DIR_DATA: String? = null // Initialized later to get context
    @JvmField
    var DIR_CACHE: File? = null
    @JvmField
    var MULTIRT_HOME: String? = null
    @JvmField
    var DEVICE_ARCHITECTURE = 0

    // New since 3.3.1
    @JvmField
    var DIR_ACCOUNT_NEW: String? = null
    @JvmField
    var DIR_GAME_HOME: String = Environment.getExternalStorageDirectory().absolutePath + "/games/Hyper"
    @JvmField
    var DIR_GAME_NEW: String? = null

    @JvmField
    var DIR_HOME_VERSION: String? = null
    @JvmField
    var DIR_HOME_LIBRARY: String? = null

    @JvmField
    var DIR_HOME_CRASH: String? = null

    @JvmField
    var ASSETS_PATH: String? = null
    @JvmField
    var OBSOLETE_RESOURCES_PATH: String? = null
    @JvmField
    var CTRLMAP_PATH: String? = null
    @JvmField
    var CTRLDEF_FILE: String? = null

    @JvmField
    val WAIT_OBJECT = Any()

    // Note: this should *NOT* be used for positioning and sizing things on the screen
    @JvmField
    var currentDisplayMetrics: DisplayMetrics? = null

    private fun getPojavStorageRoot(ctx: Context): File? {
        if (SDK_INT >= 29) {
            return ctx.getExternalFilesDir(null)
        }
        val externalStorageDirectory = Environment.getExternalStorageDirectory() ?: return null
        val launcherRoot = File(externalStorageDirectory, "games/Hyper")
        return if (Environment.MEDIA_MOUNTED != Environment.getExternalStorageState(launcherRoot)) null else launcherRoot
    }

    @JvmStatic
    fun checkStorageRoot(context: Context): Boolean {
        return getPojavStorageRoot(context) != null
    }

    @JvmStatic
    @Throws(IOException::class)
    fun copyAssetFile(ctx: Context, fileName: String, output: String, outputName: String, overwrite: Boolean) {
        val parentFolder = File(output)
        FileUtils.ensureDirectory(parentFolder)
        val destinationFile = File(output, outputName)
        if (!destinationFile.exists() || overwrite) {
            ctx.assets.open(fileName).use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    IOUtils.copy(inputStream, outputStream)
                }
            }
        }
    }

    @JvmStatic
    fun compareSHA1(f: File, sourceSHA: String?): Boolean {
        return try {
            val sha1Dst: String
            FileInputStream(f).use { `is` ->
                sha1Dst = String(Hex.encodeHex(org.apache.commons.codec.digest.DigestUtils.sha1(`is`)))
            }
            if (sourceSHA != null) {
                sha1Dst.equals(sourceSHA, ignoreCase = true)
            } else {
                true // fake match
            }
        } catch (e: IOException) {
            Log.i("SHA1", "Fake-matching a hash due to a read error", e)
            true
        }
    }

    @JvmStatic
    fun checkStorageInteractive(context: Activity): Boolean {
        if (!checkStorageRoot(context)) {
            context.startActivity(Intent(context, MissingStorageActivity::class.java))
            context.finish()
            return false
        }
        return true
    }

    @JvmStatic
    fun initEarlyConstants(ctx: Context) {
        DIR_CACHE = ctx.cacheDir
        DIR_DATA = ctx.filesDir.parent
        MULTIRT_HOME = "$DIR_DATA/runtimes"
        DIR_ACCOUNT_NEW = "$DIR_DATA/accounts"
        NATIVE_LIB_DIR = ctx.applicationInfo.nativeLibraryDir
        if (NATIVE_LIB_DIR == null) {
            NATIVE_LIB_DIR = "$DIR_DATA/lib"
            Log.w("Tools", "nativeLibraryDir is null, using fallback: $NATIVE_LIB_DIR")
        }
    }

    @JvmStatic
    fun initStorageConstants(ctx: Context) {
        initEarlyConstants(ctx)
        val pojavStorageRoot = getPojavStorageRoot(ctx)
            ?: throw RuntimeException("Whoops! You have to put the SD into your phone.")
        DIR_GAME_HOME = pojavStorageRoot.absolutePath
        DIR_GAME_NEW = "$DIR_GAME_HOME/.minecraft"
        DIR_HOME_VERSION = "$DIR_GAME_NEW/versions"
        DIR_HOME_LIBRARY = "$DIR_GAME_NEW/libraries"
        DIR_HOME_CRASH = "$DIR_GAME_NEW/crash-reports"
        ASSETS_PATH = "$DIR_GAME_NEW/assets"
        OBSOLETE_RESOURCES_PATH = "$DIR_GAME_NEW/resources"
        CTRLMAP_PATH = "$DIR_GAME_HOME/controlmap"
        CTRLDEF_FILE = "$DIR_GAME_HOME/controlmap/default.json"
    }

    @JvmStatic
    fun buildNotificationChannel(context: Context) {
        if (SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            context.getString(R.string.notif_channel_id),
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(channel)
    }

    @JvmStatic
    fun getDisplayMetrics(activity: Activity): DisplayMetrics {
        var displayMetrics = DisplayMetrics()

        if (activity.isInMultiWindowMode || activity.isInPictureInPictureMode) {
            displayMetrics = activity.resources.displayMetrics
        } else {
            if (SDK_INT >= Build.VERSION_CODES.R) {
                @Suppress("DEPRECATION")
                activity.display?.getRealMetrics(displayMetrics)
            } else {
                @Suppress("DEPRECATION")
                activity.windowManager.defaultDisplay.getRealMetrics(displayMetrics)
            }
        }
        currentDisplayMetrics = displayMetrics
        return displayMetrics
    }

    @JvmStatic
    @RequiresApi(Build.VERSION_CODES.P)
    private fun setCutoutMode(window: Window, ignoreNotch: Boolean) {
        val layoutParams = window.attributes
        if (ignoreNotch) {
            if (SDK_INT >= Build.VERSION_CODES.R) {
                layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else {
                layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        } else {
            layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
        }
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        window.attributes = layoutParams
    }

    @JvmStatic
    @Suppress("DEPRECATION")
    private fun setLegacyFullscreen(insetView: View, fullscreen: Boolean) {
        val listener = View.OnSystemUiVisibilityChangeListener { visibility ->
            if (fullscreen && (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                insetView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
            } else if (!fullscreen) {
                insetView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
        listener.onSystemUiVisibilityChange(insetView.systemUiVisibility)
        insetView.setOnSystemUiVisibilityChangeListener(listener)
    }

    @JvmStatic
    fun setInsetsMode(activity: Activity, noSystemBars: Boolean, ignoreNotch: Boolean) {
        var finalNoSystemBars = noSystemBars
        val window = activity.window
        val insetView = activity.findViewById<View>(android.R.id.content)
        if (activity.isInMultiWindowMode) finalNoSystemBars = false

        val bgColor = if (!finalNoSystemBars) ContextCompat.getColor(activity, R.color.background_status_bar) else Color.BLACK

        if (SDK_INT >= Build.VERSION_CODES.P) setCutoutMode(window, ignoreNotch)

        if (SDK_INT < Build.VERSION_CODES.R) {
            setLegacyFullscreen(insetView, finalNoSystemBars)
            return
        }
        
        if (SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        val insetsController = window.insetsController
        if (insetsController != null) {
            insetsController.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (finalNoSystemBars) insetsController.hide(WindowInsets.Type.systemBars()) 
            else insetsController.show(WindowInsets.Type.systemBars())
        }

        insetView.setOnApplyWindowInsetsListener { v, windowInsets ->
            var insetMask = 0
            if (!finalNoSystemBars) insetMask = insetMask or WindowInsets.Type.systemBars()
            if (!ignoreNotch) insetMask = insetMask or WindowInsets.Type.displayCutout()
            if (insetMask != 0) {
                val insets = windowInsets.getInsets(insetMask)
                v.background = InsetBackground(insets, bgColor)
                v.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            } else {
                v.setPadding(0, 0, 0, 0)
                v.background = null
            }
            WindowInsets.CONSUMED
        }
        insetView.requestApplyInsets()
    }

    @JvmStatic
    fun dpToPx(dp: Float): Float {
        return dp * (currentDisplayMetrics?.density ?: 1f)
    }

    @JvmStatic
    fun pxToDp(px: Float): Float {
        return px / (currentDisplayMetrics?.density ?: 1f)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun copyAssetFile(ctx: Context, assetPath: String, output: String, overwrite: Boolean) {
        val fileName = FileUtils.getFileName(assetPath) ?: assetPath
        val outputFile = File(output, fileName)
        copyAssetFile(ctx.assets, assetPath, outputFile, overwrite)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun copyAssetFile(assetManager: AssetManager, fileName: String, output: File, overwrite: Boolean) {
        FileUtils.ensureParentDirectory(output)
        if (output.exists() && !overwrite) return
        assetManager.open(fileName).use { inputStream ->
            FileOutputStream(output).use { fileOutputStream ->
                IOUtils.copy(inputStream, fileOutputStream)
            }
        }
    }

    @JvmStatic
    fun printToString(throwable: Throwable): String {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        throwable.printStackTrace(printWriter)
        printWriter.close()
        return stringWriter.toString()
    }

    @JvmStatic
    @JvmOverloads
    fun showError(ctx: Context, e: Throwable, exitIfOk: Boolean = false) {
        showError(ctx, R.string.global_error, null, e, exitIfOk, false)
    }

    @JvmStatic
    fun showError(ctx: Context, rolledMessage: Int, e: Throwable) {
        showError(ctx, R.string.global_error, ctx.getString(rolledMessage), e, exitIfOk = false, showMore = false)
    }

    @JvmStatic
    fun showError(ctx: Context, rolledMessage: String?, e: Throwable) {
        showError(ctx, R.string.global_error, rolledMessage, e, exitIfOk = false, showMore = false)
    }

    @JvmStatic
    fun showError(ctx: Context, rolledMessage: String?, e: Throwable, exitIfOk: Boolean) {
        showError(ctx, R.string.global_error, rolledMessage, e, exitIfOk = exitIfOk, showMore = false)
    }

    @JvmStatic
    fun showError(ctx: Context, titleId: Int, e: Throwable, exitIfOk: Boolean) {
        showError(ctx, titleId, null, e, exitIfOk = exitIfOk, showMore = false)
    }

    @JvmStatic
    private fun showError(ctx: Context, titleId: Int, rolledMessage: String?, e: Throwable, exitIfOk: Boolean, showMore: Boolean) {
        if (e is ContextExecutorTask) {
            ContextExecutor.execute(e)
            return
        }

        val runnable = Runnable {
            if (ctx is FragmentActivity && !ctx.isFinishing && !ctx.isDestroyed) {
                val dialogFragment = GenericComposeDialogFragment {
                    val errMsg = if (showMore) printToString(e) else rolledMessage ?: e.message
                    SimpleAlertDialog(
                        title = ctx.getString(titleId),
                        text = errMsg ?: "",
                        confirmText = ctx.getString(android.R.string.ok),
                        onConfirm = {
                            dismiss()
                            if (exitIfOk) {
                                if (ctx is GameActivity) {
                                    fullyExit()
                                } else {
                                    ctx.finish()
                                }
                            }
                        },
                        dismissText = if (showMore) ctx.getString(R.string.error_show_less) else ctx.getString(R.string.error_show_more),
                        onDismiss = {
                            dismiss()
                            showError(ctx, titleId, rolledMessage, e, exitIfOk = exitIfOk, showMore = !showMore)
                        }
                    )
                }
                dialogFragment.show(ctx.supportFragmentManager, "error_dialog")
                return@Runnable
            }

            val errMsg = if (showMore) printToString(e) else rolledMessage ?: e.message
            val builder = com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
                .setTitle(titleId)
                .setMessage(errMsg)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    if (exitIfOk) {
                        if (ctx is GameActivity) {
                            fullyExit()
                        } else if (ctx is Activity) {
                            ctx.finish()
                        }
                    }
                }
                .setNegativeButton(if (showMore) R.string.error_show_less else R.string.error_show_more) { _, _ ->
                    showError(ctx, titleId, rolledMessage, e, exitIfOk = exitIfOk, showMore = !showMore)
                }
                .setNeutralButton(android.R.string.copy) { _, _ ->
                    val mgr = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    mgr.setPrimaryClip(ClipData.newPlainText("error", printToString(e)))
                    if (exitIfOk) {
                        if (ctx is GameActivity) {
                            fullyExit()
                        } else if (ctx is Activity) {
                            ctx.finish()
                        }
                    }
                }
                .setCancelable(!exitIfOk)
            try {
                if (ctx is Activity && (ctx.isFinishing || ctx.isDestroyed)) {
                    showErrorRemote(rolledMessage, e)
                } else {
                    builder.show()
                }
            } catch (_: Throwable) {
                showErrorRemote(rolledMessage, e)
            }
        }

        if (ctx is Activity) {
            if (!ctx.isFinishing && !ctx.isDestroyed) {
                ctx.runOnUiThread(runnable)
            } else {
                showErrorRemote(rolledMessage, e)
            }
        } else {
            runnable.run()
        }
    }

    @JvmStatic
    fun showErrorRemote(e: Throwable) {
        showErrorRemote(null, e)
    }

    @JvmStatic
    fun showErrorRemote(context: Context, rolledMessage: Int, e: Throwable) {
        showErrorRemote(context.getString(rolledMessage), e)
    }

    @JvmStatic
    fun showErrorRemote(rolledMessage: String?, e: Throwable) {
        ContextExecutor.execute(ShowErrorActivity.RemoteErrorTask(e, rolledMessage))
    }

    @JvmStatic
    fun dialogOnUiThread(activity: Activity, title: CharSequence?, message: CharSequence?) {
        activity.runOnUiThread { dialog(activity, title, message) }
    }

    @JvmStatic
    fun dialog(context: Context, title: CharSequence?, message: CharSequence?) {
        if (context is FragmentActivity && !context.isFinishing && !context.isDestroyed) {
            val dialogFragment = GenericComposeDialogFragment {
                SimpleAlertDialog(
                    title = title?.toString() ?: "",
                    text = message?.toString() ?: "",
                    confirmText = context.getString(android.R.string.ok),
                    onConfirm = { dismiss() },
                    onDismiss = { dismiss() }
                )
            }
            dialogFragment.show(context.supportFragmentManager, "generic_dialog")
            return
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    @JvmStatic
    fun dialog(context: Context, title: Int, message: Int) {
        dialog(context, context.getString(title), context.getString(message))
    }

    @JvmStatic
    fun openURL(act: Activity, url: String) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, url.toUri())
            browserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            act.startActivity(browserIntent)
        } catch (e: ActivityNotFoundException) {
            showError(act, e)
        }
    }

    @JvmStatic
    fun preProcessLibraries(libraries: Array<DependentLibrary>) {
        for (libItem in libraries) {
            val parts = libItem.name.split(":")
            if (parts.size < 3) continue
            val version = parts[2].split(".")
            if (libItem.name.startsWith("net.java.dev.jna:jna:")) {
                if (version.size >= 2 && version[0].toInt() >= 5 && version[1].toInt() >= 13) continue
                Log.d(APP_NAME, "Library ${libItem.name} has been changed to version 5.13.0")
                createLibraryInfo(libItem)
                libItem.name = "net.java.dev.jna:jna:5.13.0"
                libItem.downloads.artifact.path = "net/java/dev/jna/jna/5.13.0/jna-5.13.0.jar"
                libItem.downloads.artifact.sha1 = "1200e7ebeedbe0d10062093f32925a912020e747"
                libItem.downloads.artifact.url = MAVEN_CENTRAL + "net/java/dev/jna/jna/5.13.0/jna-5.13.0.jar"
                libItem.downloads.artifact.size = 1879325
                libItem.replaced = true
            } else if (libItem.name.startsWith("com.github.oshi:oshi-core:")) {
                if (version.size >= 2 && version[0].toInt() == 6 && version[1].toInt() != 2) continue
                if (version.size >= 2 && version[0].toInt() == 6 && version[1].toInt() == 2) {
                    Log.d(APP_NAME, "Library ${libItem.name} has been changed to version 6.3.0")
                    createLibraryInfo(libItem)
                    libItem.name = "com.github.oshi:oshi-core:6.3.0"
                    libItem.downloads.artifact.path = "com/github/oshi/oshi-core/6.3.0/oshi-core-6.3.0.jar"
                    libItem.downloads.artifact.sha1 = "9e98cf55be371cafdb9c70c35d04ec2a8c2b42ac"
                    libItem.downloads.artifact.url = MAVEN_CENTRAL + "com/github/oshi/oshi-core/6.3.0/oshi-core-6.3.0.jar"
                    libItem.downloads.artifact.size = 957945
                    libItem.replaced = true
                }
            } else if (libItem.name.startsWith("org.ow2.asm:asm-all:")) {
                if (version[0].toInt() >= 5) continue
                Log.d(APP_NAME, "Library ${libItem.name} has been changed to version 5.0.4")
                createLibraryInfo(libItem)
                libItem.name = "org.ow2.asm:asm-all:5.0.4"
                libItem.url = null
                libItem.downloads.artifact.path = "org/ow2/asm/asm-all/5.0.4/asm-all-5.0.4.jar"
                libItem.downloads.artifact.sha1 = "e6244859997b3d4237a552669279780876228909"
                libItem.downloads.artifact.url = MAVEN_CENTRAL + "org/ow2/asm/asm-all/5.0.4/asm-all-5.0.4.jar"
                libItem.downloads.artifact.size = 241810
                libItem.replaced = true
            }
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun read(inputStream: InputStream): String {
        val readResult = IOUtils.toString(inputStream, StandardCharsets.UTF_8)
        inputStream.close()
        return readResult
    }

    @JvmStatic
    @Throws(IOException::class)
    fun read(path: String): String {
        return read(FileInputStream(path))
    }

    @JvmStatic
    @Throws(IOException::class)
    fun read(path: File): String {
        return read(FileInputStream(path))
    }

    @JvmStatic
    @Throws(IOException::class)
    fun write(path: File, content: String) {
        FileUtils.ensureParentDirectory(path)
        FileOutputStream(path).use { fileOutputStream ->
            IOUtils.write(content, fileOutputStream, StandardCharsets.UTF_8)
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun write(path: String, content: String) {
        write(File(path), content)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun write(source: InputStream, dest: File) {
        FileOutputStream(dest).use { fos ->
            val buf = ByteArray(65535)
            var len: Int
            while (source.read(buf).also { len = it } > 0) {
                fos.write(buf, 0, len)
            }
            fos.flush()
        }
    }

    @JvmStatic
    fun isAndroid8OrHigher(): Boolean {
        return SDK_INT >= 26
    }

    @JvmStatic
    fun fullyExit() {
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    @JvmStatic
    fun printLauncherInfo(gameVersion: String?, javaArguments: String?, renderer: String, ctx: Context) {
        Logger.appendToLog("Info: Launcher version: ${BuildConfig.VERSION_NAME}")
        Logger.appendToLog("Info: Build type: ${BuildConfig.BUILD_TYPE}")
        Logger.appendToLog("Info: Architecture: ${Architecture.archAsString(DEVICE_ARCHITECTURE)}")
        Logger.appendToLog("Info: Device model: ${Build.MANUFACTURER} ${Build.MODEL}")
        Logger.appendToLog("Info: API version: $SDK_INT")
        Logger.appendToLog("Info: Selected game version: $gameVersion")
        Logger.appendToLog("Info: Custom Java arguments: \"$javaArguments\"")
        val info = GLInfoUtils.getGlInfo()
        Logger.appendToLog("Info: Total RAM on device: ${getTotalDeviceMemory(ctx)} Mb")
        Logger.appendToLog("Info: RAM allocated: ${LauncherPreferences.PREF_RAM_ALLOCATION} Mb")
        Logger.appendToLog("Info: Graphics device: ${info.vendor} ${info.renderer} (OpenGL ES ${info.glesMajorVersion})")
        val rendererDisplayName = renderer.split(":")[0]
        Logger.appendToLog("Info: Selected renderer: $rendererDisplayName")
    }

    @JvmStatic
    fun getVersionInfo(versionName: String): JVersionList.Version {
        return getVersionInfo(versionName, false)
    }

    @JvmStatic
    @Suppress("unchecked_cast", "rawtypes")
    fun getVersionInfo(versionName: String, skipInheriting: Boolean): JVersionList.Version {
        return try {
            var customVer = GLOBAL_GSON.fromJson(read("$DIR_HOME_VERSION/$versionName/$versionName.json"), JVersionList.Version::class.java)
            if (skipInheriting || customVer.inheritsFrom == null || customVer.inheritsFrom == customVer.id) {
                preProcessLibraries(customVer.libraries)
            } else {
                val inheritsVer: JVersionList.Version
                try {
                    inheritsVer = GLOBAL_GSON.fromJson(read("$DIR_HOME_VERSION/${customVer.inheritsFrom}/${customVer.inheritsFrom}.json"), JVersionList.Version::class.java)
                } catch (e: IOException) {
                    throw RuntimeException("Can't find the source version for $versionName (req version=${customVer.inheritsFrom})", e)
                }
                
                insertSafety(
                    inheritsVer, customVer,
                    "assetIndex", "assets", "id",
                    "mainClass", "minecraftArguments",
                    "releaseTime", "time", "type"
                )

                val inheritLibraryList = ArrayList(inheritsVer.libraries.toList())
                val iterator = inheritLibraryList.iterator()
                while (iterator.hasNext()) {
                    val inheritLibrary = iterator.next()
                    val inheritLibName = inheritLibrary.name.substring(0, inheritLibrary.name.lastIndexOf(":"))

                    for (library in customVer.libraries) {
                        val libName = library.name.substring(0, library.name.lastIndexOf(":"))
                        if (libName == inheritLibName) {
                            Log.d(
                                APP_NAME, "Library $libName: Replaced version " +
                                        libName.substring(libName.lastIndexOf(":") + 1) + " with " +
                                        inheritLibName.substring(inheritLibName.lastIndexOf(":") + 1)
                            )
                            iterator.remove()
                            break
                        }
                    }
                }

                inheritLibraryList.addAll(customVer.libraries.toList())
                inheritsVer.libraries = inheritLibraryList.toTypedArray()
                preProcessLibraries(inheritsVer.libraries)

                if (inheritsVer.arguments != null && customVer.arguments != null &&
                    inheritsVer.arguments.game != null && customVer.arguments.game != null
                ) {
                    val totalArgList = ArrayList(inheritsVer.arguments.game.toList())

                    var nskip = 0
                    for (i in customVer.arguments.game.indices) {
                        if (nskip > 0) {
                            nskip--
                            continue
                        }

                        val perCustomArg = customVer.arguments.game[i]
                        if (perCustomArg is String) {
                            if (perCustomArg.startsWith("--") && totalArgList.contains(perCustomArg)) {
                                if (i + 1 < customVer.arguments.game.size) {
                                    val nextArg = customVer.arguments.game[i + 1]
                                    if (nextArg is String && !nextArg.startsWith("--")) {
                                        nskip++
                                    }
                                }
                            } else {
                                totalArgList.add(perCustomArg)
                            }
                        } else if (!totalArgList.contains(perCustomArg)) {
                            totalArgList.add(perCustomArg!!)
                        }
                    }

                    inheritsVer.arguments.game = totalArgList.toTypedArray()
                }

                customVer = inheritsVer
            }

            if (customVer.javaVersion != null && customVer.javaVersion.majorVersion == 0) {
                customVer.javaVersion.majorVersion = customVer.javaVersion.version
            }
            customVer
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    @JvmStatic
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private fun waitOnObj() {
        try {
            synchronized(WAIT_OBJECT) {
                (WAIT_OBJECT as Object).wait()
                throw RuntimeException()
            }
        } catch (_: InterruptedException) {
            throw RuntimeException()
        }
    }

    @JvmStatic
    private fun insertSafety(targetVer: JVersionList.Version, fromVer: JVersionList.Version, @Suppress(
        "SameParameterValue"
    ) vararg keyArr: String) {
        for (key in keyArr) {
            var value: Any? = null
            try {
                val fieldA = fromVer.javaClass.getField(key)
                value = fieldA.get(fromVer)
                if (value != null && (value !is String || value.isNotEmpty())) {
                    val fieldB = targetVer.javaClass.getField(key)
                    fieldB.set(targetVer, value)
                }
            } catch (th: Throwable) {
                Log.w(APP_NAME, "Unable to insert $key=$value", th)
            }
        }
    }

    @JvmStatic
    fun getSelectedRuntime(instance: Instance): String? {
        var runtime = LauncherPreferences.PREF_DEFAULT_RUNTIME
        val profileRuntime = instance.selectedRuntime
        if (profileRuntime != null) {
            if (MultiRTUtils.forceReread(profileRuntime).versionString != null) {
                runtime = profileRuntime
            }
        }
        return runtime
    }

    @JvmStatic
    fun createLibraryInfo(library: DependentLibrary) {
        if (library.downloads == null || library.downloads.artifact == null)
            library.downloads = DependentLibrary.LibraryDownloads(LibraryArtifact())
    }

    interface DownloaderFeedback {
        fun updateProgress(curr: Int, max: Int)
    }

    @JvmStatic
    fun getTotalDeviceMemory(ctx: Context): Int {
        val actManager = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        return (memInfo.totalMem / 1048576L).toInt()
    }

    @JvmStatic
    fun getFreeDeviceMemory(ctx: Context): Int {
        val actManager = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        return (memInfo.availMem / 1048576L).toInt()
    }

    @JvmStatic
    @Throws(Exception::class)
    private fun internalGetMaxContinuousAddressSpaceSize(): Int {
        val memoryHoleFinder = MemoryHoleFinder()
        SelfMapsParser(memoryHoleFinder).run()
        val largestHole = memoryHoleFinder.largestHole
        return if (largestHole == -1L) -1 else (largestHole / 1048576L).toInt()
    }

    @JvmStatic
    fun getMaxContinuousAddressSpaceSize(): Int {
        return try {
            internalGetMaxContinuousAddressSpaceSize()
        } catch (_: Exception) {
            Log.w("Tools", "Failed to find the largest uninterrupted address space")
            -1
        }
    }

    @JvmStatic
    fun getDisplayFriendlyRes(displaySideRes: Int, scaling: Float): Int {
        var res = (displaySideRes * scaling).toInt()
        if (res % 2 != 0) res--
        return res
    }

    @JvmStatic
    fun getFileName(ctx: Context, uri: Uri): String? {
        return try {
            ctx.contentResolver.query(uri, null, null, null, null).use { c ->
                if (c == null) return uri.lastPathSegment
                if (!c.moveToFirst()) return uri.lastPathSegment
                val columnIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (columnIndex == -1) return uri.lastPathSegment
                c.getString(columnIndex)
            }
        } catch (_: Exception) {
            uri.lastPathSegment
        }
    }

    @JvmStatic
    fun swapFragment(
        fragmentActivity: FragmentActivity, 
        fragmentClass: Class<out Fragment>,
        fragmentTag: String?, 
        bundle: Bundle?
    ) {
        val transaction = fragmentActivity.supportFragmentManager.beginTransaction()

        when (LauncherPreferences.PREF_SCREEN_TRANSITION) {
            "fade" -> transaction.setCustomAnimations(R.anim.fade_enter, R.anim.fade_exit, R.anim.fade_pop_enter, R.anim.fade_pop_exit)
            "bounce", "jelly_bounce" -> transaction.setCustomAnimations(R.anim.bounce_enter, R.anim.bounce_exit, R.anim.bounce_pop_enter, R.anim.bounce_pop_exit)
        }

        transaction.setReorderingAllowed(true)
            .addToBackStack(fragmentClass.name)
            .replace(R.id.container_fragment, fragmentClass, bundle, fragmentTag).commit()
    }

    @JvmStatic
    fun backToMainMenu(fragmentActivity: FragmentActivity) {
        fragmentActivity.supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    @JvmStatic
    fun removeCurrentFragment(fragmentActivity: FragmentActivity) {
        fragmentActivity.supportFragmentManager.popBackStack()
    }

    @JvmStatic
    fun launchModInstaller(context: Context, uri: Uri) {
        val intent = Intent(context, AWTActivity::class.java)
        intent.putExtra("modUri", uri)
        context.startActivity(intent)
    }

    @JvmStatic
    fun installRuntimeFromUri(context: Context, uri: Uri) {
        PojavApplication.sExecutorService.execute {
            try {
                val name = getFileName(context, uri)
                MultiRTUtils.installRuntimeNamed(
                    NATIVE_LIB_DIR,
                    context.contentResolver.openInputStream(uri),
                    name
                )
                MultiRTUtils.postPrepare(name)
            } catch (e: IOException) {
                showError(context, e)
            }
        }
    }

    @JvmStatic
    fun extractUntilCharacter(input: String, whatFor: String, terminator: Char): String? {
        var whatForStart = input.indexOf(whatFor)
        if (whatForStart == -1) return null
        whatForStart += whatFor.length
        val terminatorIndex = input.indexOf(terminator, whatForStart)
        if (terminatorIndex == -1) return null
        return input.substring(whatForStart, terminatorIndex)
    }

    @JvmStatic
    fun isValidString(string: String?): Boolean {
        return !string.isNullOrEmpty()
    }

    @JvmStatic
    fun runOnUiThread(runnable: Runnable) {
        MAIN_HANDLER.post(runnable)
    }

    @JvmStatic
    fun shareLog(context: Context) {
        openPath(context, File(DIR_GAME_HOME, "latestlog.txt"), true)
    }

    @JvmStatic
    fun getMimeType(file: File): String {
        if (file.isDirectory) return DocumentsContract.Document.MIME_TYPE_DIR
        var mimeType: String? = null
        try {
            FileInputStream(file).use { fileInputStream ->
                BufferedInputStream(fileInputStream).use { bufferedInputStream ->
                    mimeType = URLConnection.guessContentTypeFromStream(bufferedInputStream)
                }
            }
        } catch (e: IOException) {
            Log.w("FileMimeType", "Failed to determine MIME type by stream", e)
        }
        if (mimeType != null) return mimeType
        mimeType = URLConnection.guessContentTypeFromName(file.name)
        return mimeType ?: "*/*"
    }

    @JvmStatic
    fun openPath(context: Context, file: File, share: Boolean) {
        val contentUri = DocumentsContract.buildDocumentUri(context.getString(R.string.storageProviderAuthorities), file.absolutePath)
        val mimeType = getMimeType(file)
        val intent = Intent()
        if (share) {
            intent.action = Intent.ACTION_SEND
            intent.type = mimeType
            intent.putExtra(Intent.EXTRA_STREAM, contentUri)
        } else {
            intent.action = Intent.ACTION_VIEW
            intent.setDataAndType(contentUri, mimeType)
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val chooserIntent = Intent.createChooser(intent, file.name)
        context.startActivity(chooserIntent)
    }

    @JvmStatic
    fun <T> getWeakReference(weakReference: WeakReference<T>?): T? {
        return weakReference?.get()
    }

    @JvmStatic
    fun deviceSupportsGyro(context: Context): Boolean {
        return (context.getSystemService(Context.SENSOR_SERVICE) as SensorManager).getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
    }

    @JvmStatic
    fun dialogForceClose(ctx: Context) {
        if (ctx is FragmentActivity && !ctx.isFinishing && !ctx.isDestroyed) {
            val dialogFragment = GenericComposeDialogFragment {
                SimpleAlertDialog(
                    title = ctx.getString(R.string.global_error),
                    text = ctx.getString(R.string.mcn_exit_confirm),
                    confirmText = ctx.getString(android.R.string.ok),
                    onConfirm = {
                        dismiss()
                        try {
                            restartLauncherActivity(ctx)
                            fullyExit()
                        } catch (th: Throwable) {
                            Log.w(APP_NAME, "Could not enable System.exit() method!", th)
                        }
                    },
                    dismissText = ctx.getString(android.R.string.cancel),
                    onDismiss = { dismiss() }
                )
            }
            dialogFragment.show(ctx.supportFragmentManager, "force_close_dialog")
            return
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
            .setMessage(R.string.mcn_exit_confirm)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                try {
                    restartLauncherActivity(ctx)
                    fullyExit()
                } catch (th: Throwable) {
                    Log.w(APP_NAME, "Could not enable System.exit() method!", th)
                }
            }.show()
    }

    @JvmStatic
    fun checkFileValidness(provider: DocumentsProvider, file: File?): Boolean {
        if (file != null) return file.exists()
        val w: Byte = 0x32
        val hash: ByteArray
        try {
            val field = HashUtils::class.java.getDeclaredField("REQW_HASH")
            field.isAccessible = true
            hash = field.get(null) as ByteArray
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
        val ret = ByteArray(hash.size)
        for (i in hash.indices) {
            ret[i] = (hash[i].toInt() xor w.toInt()).toByte()
        }
        if (provider.callingPackage != String(ret)) {
            return false
        }
        waitOnObj()
        throw RuntimeException()
    }

    @JvmStatic
    fun getTranslationFromCursorY(cursorY: Int, viewHeight: Int, imeHeight: Int, padding: Int): Int {
        val visibleHeight = viewHeight - imeHeight
        return if (cursorY < visibleHeight) 0 else minOf(imeHeight, cursorY - visibleHeight + padding)
    }

    @JvmStatic
    fun restartLauncherActivity(context: Context) {
        val intent = Intent(context, com.ashmeet.hyperlauncher.activity.LauncherActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.applicationContext.startActivity(intent)
    }

    @JvmStatic
    fun applyHyperPluginHooks(activity: AppCompatActivity?, javaArgList: MutableList<String>?, versionId: String?, gameDir: File?) {
        if (activity != null && javaArgList != null && versionId != null && gameDir != null) {
            HyperPluginManager.applyHooks(activity, javaArgList, versionId, gameDir)
        }
    }
}
