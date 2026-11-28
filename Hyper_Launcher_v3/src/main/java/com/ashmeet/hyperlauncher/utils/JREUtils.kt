package com.ashmeet.hyperlauncher.utils

import android.content.Context
import android.system.Os
import android.util.ArrayMap
import android.util.Log
import com.ashmeet.hyperbackend.Logger
import com.ashmeet.hyperbackend.game.renderer.GameRenderer
import com.ashmeet.hyperlauncher.plugin.ffmpeg.FFmpegPluginManager
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.utils.plugins.LibraryPlugin
import git.artdeell.mojoexec.MojoExec
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException

object JREUtils {

    init {
        NativeLibraryLoader.loadRuntime()
        NativeLibraryLoader.loadAwt()
    }

    @JvmStatic
    external fun chdir(path: String?): Int

    @JvmStatic
    external fun setLdLibraryPath(ldLibraryPath: String?)

    @JvmStatic
    fun redirectAndPrintJRELog() {
        Log.v("jrelog", "Log starts here")
        Thread(object : Runnable {
            var failTime = 0
            var logcatPb: ProcessBuilder? = null
            override fun run() {
                try {
                    if (logcatPb == null) {
                        logcatPb = ProcessBuilder().command(
                            "logcat",
                            "-v",
                            "brief",
                            "-s",
                            "jrelog",
                            "LIBGL",
                            "NativeInput"
                        ).redirectErrorStream(true)
                    }

                    Log.i("jrelog-logcat", "Clearing logcat")
                    ProcessBuilder().command("logcat", "-c").redirectErrorStream(true).start()
                    Log.i("jrelog-logcat", "Starting logcat")
                    val p = logcatPb!!.start()

                    val buf = ByteArray(1024)
                    var len: Int
                    val inputStream = p.inputStream
                    while (inputStream.read(buf).also { len = it } != -1) {
                        val currStr = String(buf, 0, len)
                        Logger.appendToLog(currStr)
                    }

                    if (p.waitFor() != 0) {
                        Log.e("jrelog-logcat", "Logcat exited with code " + p.exitValue())
                        failTime++
                        Log.i(
                            "jrelog-logcat",
                            (if (failTime <= 10) "Restarting logcat" else "Too many restart fails") + " (attempt " + failTime + "/10"
                        )
                        if (failTime <= 10) {
                            run()
                        } else {
                            Logger.appendToLog("ERROR: Unable to get more log.")
                        }
                    }
                } catch (e: Throwable) {
                    Log.e("jrelog-logcat", "Exception on logging thread", e)
                    Logger.appendToLog("Exception on logging thread:\n" + Log.getStackTraceString(e))
                }
            }
        }).start()
        Log.i("jrelog-logcat", "Logcat thread started")
    }

    @Throws(IOException::class)
    private fun overrideEnvVars(envMap: MutableMap<String, String>) {
        val customEnvFile = File(HyperUtils.DIR_GAME_HOME, "custom_env.txt")
        if (!customEnvFile.exists() || !customEnvFile.isFile) return
        BufferedReader(FileReader(customEnvFile)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line!!
                val index = l.indexOf("=")
                if (index != -1) {
                    envMap[l.substring(0, index)] = l.substring(index + 1)
                }
            }
        }
    }

    @JvmStatic
    fun setupAngleEnv(envMap: MutableMap<String, String>) {
        if (!LauncherPreferences.PREF_USE_ANGLE) return
        var angleFile: File? = null
        if (HyperUtils.NATIVE_LIB_DIR != null) {
            angleFile = File(HyperUtils.NATIVE_LIB_DIR, "libEGL_angle.so")
        }
        if (angleFile == null || !angleFile.exists()) {
            angleFile = File("/system/lib64/libEGL_angle.so")
        }
        if (!angleFile.exists()) {
            angleFile = File("/system/lib/libEGL_angle.so")
        }
        if (angleFile.exists()) {
            envMap["LIBGL_EGL"] = angleFile.absolutePath
            envMap["LIBGL_GLES"] = angleFile.absolutePath
        }
    }

    @JvmStatic
    fun setupFfmpegEnv(ctx: Context, envMap: MutableMap<String, String>) {
        val ffmpeg = LibraryPlugin.discoverPlugin(ctx, LibraryPlugin.ID_FFMPEG_PLUGIN)
        if (ffmpeg != null) {
            envMap["POJAV_FFMPEG_PATH"] = ffmpeg.resolveAbsolutePath("libffmpeg.so")
            return
        }
        if (FFmpegPluginManager.isAvailable) {
            val path = FFmpegPluginManager.executablePath
            if (path != null) {
                envMap["POJAV_FFMPEG_PATH"] = path
            }
        }
    }

    @JvmStatic
    fun setLibraryOverrides(path: String, overrides: String) {
        Log.i("JREUtils", "setLibraryOverrides: path=$path, overrides=$overrides")
    }

    @JvmStatic
    @Throws(Throwable::class)
    fun setGameEnvironment(context: Context, renderer: GameRenderer?, versionEnvMap: Map<String, String>?) {
        val envMap: MutableMap<String, String> = ArrayMap()
        val modRuntimeDir = File(HyperUtils.DIR_CACHE, "app_runtime_mod")
        if (!modRuntimeDir.exists()) {
            modRuntimeDir.mkdirs()
        }
        envMap["MOD_ANDROID_RUNTIME"] = modRuntimeDir.absolutePath

        setupAngleEnv(envMap)
        setupFfmpegEnv(context, envMap)

        renderer?.setupEnvironment(context, envMap)
        if (versionEnvMap != null) {
            envMap.putAll(versionEnvMap)
        }
        MojoExec.setUseBigCoreAffinity(LauncherPreferences.PREF_BIG_CORE_AFFINITY)
        if (LauncherPreferences.PREF_BIG_CORE_AFFINITY) envMap["POJAV_BIG_CORE_AFFINITY"] = "1"
        if (LauncherPreferences.PREF_ALSOFT_FORCE_OPENSL) envMap["ALSOFT_DRIVERS"] = "opensl"

        overrideEnvVars(envMap)

        for ((key, value) in envMap) {
            Logger.appendToLog("Added custom env: $key=$value")
            try {
                Os.setenv(key, value, true)
            } catch (exception: NullPointerException) {
                Log.e("JREUtils", exception.toString())
            }
        }
    }

    @JvmStatic
    fun launchJavaVM() {
        HyperUtils.fullyExit()
    }

    @JvmStatic
    fun parseJavaArguments(argsInput: String?): ArrayList<String> {
        val parsedArguments = ArrayList<String>(0)
        if (argsInput == null || argsInput.trim().isEmpty()) return parsedArguments
        var args = argsInput.trim().replace(" ", "")
        val separators = arrayOf("-XX:-", "-XX:+", "-XX:", "--", "-D", "-X", "-javaagent:", "-verbose")
        for (prefix in separators) {
            while (true) {
                val start = args.indexOf(prefix)
                if (start == -1) break
                var end = -1
                for (separator in separators) {
                    val tempEnd = args.indexOf(separator, start + prefix.length)
                    if (tempEnd == -1) continue
                    if (end == -1) {
                        end = tempEnd
                        continue
                    }
                    end = end.coerceAtMost(tempEnd)
                }
                if (end == -1) end = args.length

                val parsedSubString = args.substring(start, end)
                args = args.replace(parsedSubString, "")

                if (!parsedSubString.startsWith("-")) {
                    Log.w("JAVA ARGS PARSER", "Rejected invalid argument not starting with '-': $parsedSubString")
                    continue
                }

                if (parsedSubString.indexOf('=') == parsedSubString.lastIndexOf('=')) {
                    val arraySize = parsedArguments.size
                    if (arraySize > 0) {
                        val lastString = parsedArguments[arraySize - 1]
                        if (lastString.isNotEmpty() && (lastString[lastString.length - 1] == ',' || parsedSubString.contains(","))) {
                            parsedArguments[arraySize - 1] = lastString + parsedSubString
                            continue
                        }
                    }
                    parsedArguments.add(parsedSubString)
                } else if (parsedSubString.startsWith("--") && parsedSubString.indexOf('=') != -1) {
                    parsedArguments.add(parsedSubString)
                } else {
                    Log.w("JAVA ARGS PARSER", "Removed improper arguments: $parsedSubString")
                }
            }
        }
        return parsedArguments
    }

    @JvmStatic
    val detectedVersion: Int
        get() = GpuUtils.getGlInfo().glesMajorVersion
}
