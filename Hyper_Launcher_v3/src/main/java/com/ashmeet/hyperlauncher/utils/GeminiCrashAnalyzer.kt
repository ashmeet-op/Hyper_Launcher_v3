package com.ashmeet.hyperlauncher.utils

import android.content.Context
import android.os.Build
import android.util.Log
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.instances.Instances
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object GeminiCrashAnalyzer {
    private const val TAG = "GeminiCrashAnalyzer"

    fun getApiKey(context: Context): String {
        return runCatching { context.getString(R.string.hyper_launcher_api_key) }.getOrDefault("")
    }

    /**
     * Gather diagnostic context from crash logs, settings, and instance files
     */
    fun gatherDiagnosticContext(context: Context, rawLogs: String? = null): String {
        val sb = StringBuilder()

        sb.append("=== SYSTEM & DEVICE INFO ===\n")
        sb.append("Device: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n")
        sb.append("Android API: ").append(Build.VERSION.SDK_INT).append(" (Android ").append(Build.VERSION.RELEASE).append(")\n")
        sb.append("Architecture: ").append(Architecture.archAsString(Architecture.getDeviceArchitecture())).append("\n\n")

        sb.append("=== LAUNCHER & RENDERER SETTINGS ===\n")
        val allocatedRam = LauncherPreferences.PREF_RAM_ALLOCATION
        val freeRam = Tools.getFreeDeviceMemory(context)
        sb.append("Allocated RAM: ").append(allocatedRam).append(" MB\n")
        sb.append("Free Device RAM: ").append(freeRam).append(" MB\n")
        sb.append("Selected Renderer: ").append(LauncherPreferences.PREF_RENDERER).append("\n")
        sb.append("Vulkan Driver: ").append(LauncherPreferences.PREF_DRIVER).append("\n")
        sb.append("Graphics Backend: ").append(LauncherPreferences.PREF_GRAPHICS_BACKEND).append("\n")
        sb.append("Use ANGLE: ").append(LauncherPreferences.PREF_USE_ANGLE).append("\n")
        sb.append("Alternate Surface: ").append(LauncherPreferences.PREF_USE_ALTERNATE_SURFACE).append("\n")
        sb.append("Force VSync: ").append(LauncherPreferences.PREF_FORCE_VSYNC).append("\n")
        sb.append("Sustained Performance: ").append(LauncherPreferences.PREF_SUSTAINED_PERFORMANCE).append("\n\n")

        val instance = runCatching { Instances.loadSelectedInstance() }.getOrNull()
        if (instance != null) {
            sb.append("=== SELECTED INSTANCE ===\n")
            sb.append("Instance Name: ").append(instance.mInstanceRoot.name).append("\n")
            sb.append("Minecraft Version: ").append(instance.versionId ?: "Unknown").append("\n")
            sb.append("Selected Runtime: ").append(instance.selectedRuntime ?: "Auto").append("\n")
            sb.append("Custom JVM Args: ").append(instance.jvmArgs ?: "None").append("\n")

            val modsDir = File(instance.gameDirectory, "mods")
            if (modsDir.exists() && modsDir.isDirectory) {
                val modFiles = modsDir.listFiles { _, name -> name.endsWith(".jar") || name.endsWith(".disabled") }
                sb.append("Installed Mods (").append(modFiles?.size ?: 0).append("):\n")
                modFiles?.take(50)?.forEach { modFile ->
                    sb.append(" - ").append(modFile.name).append("\n")
                }
                if ((modFiles?.size ?: 0) > 50) {
                    sb.append(" ... and ").append(modFiles!!.size - 50).append(" more mods\n")
                }
            } else {
                sb.append("Installed Mods: None (mods folder missing or empty)\n")
            }
            sb.append("\n")
        }

        val logs = rawLogs ?: runCatching { Tools.read(File(Tools.DIR_GAME_HOME, "latestlog.txt")) }.getOrDefault("")
        if (logs.isNotBlank()) {
            val logLines = logs.lines()
            val recentLogs = if (logLines.size > 120) logLines.takeLast(120).joinToString("\n") else logs
            sb.append("=== LATEST GAME LOGS (Tail) ===\n")
            sb.append(recentLogs).append("\n\n")
        }

        val crashReportsDir = File(Tools.DIR_GAME_HOME, "crash-reports")
        if (crashReportsDir.exists() && crashReportsDir.isDirectory) {
            val crashFiles = crashReportsDir.listFiles()?.sortedByDescending { it.lastModified() }
            val latestCrashFile = crashFiles?.firstOrNull()
            if (latestCrashFile != null && latestCrashFile.exists()) {
                val crashContent = runCatching { Tools.read(latestCrashFile) }.getOrNull()
                if (crashContent != null && crashContent.isNotBlank()) {
                    val crashLines = crashContent.lines()
                    val truncatedCrash = if (crashLines.size > 150) crashLines.take(150).joinToString("\n") else crashContent
                    sb.append("=== LATEST CRASH REPORT (").append(latestCrashFile.name).append(") ===\n")
                    sb.append(truncatedCrash).append("\n")
                }
            }
        }

        return sb.toString()
    }

    /**
     * Send crash & diagnostic context to Gemini API
     */
    suspend fun analyze(context: Context, logs: String? = null): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(context)
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("API_KEY_MISSING"))
        }

        val diagnosticInfo = gatherDiagnosticContext(context, logs)

        val prompt = """
            You are an expert diagnostic AI for HyperLauncher (an Android Minecraft launcher).
            Analyze the following game crash log, renderer settings, device specs, and mod list.
            
            Diagnostic Data:
            $diagnosticInfo
            
            Instructions:
            1. Identify the exact cause of the crash (e.g. incompatible mod, out of memory, wrong Java version, renderer/driver conflict like MobileGlues/Zink/GL4ES, corrupted file/config, or missing dependency).
            2. Provide clear, concise, bullet-point steps explaining how the user can fix the issue.
            3. If the crash is an internal launcher or native C/C++ crash (e.g. native segfault in .so library, MojoExec, JNI hook error, or system GPU driver crash), explicitly tell the user: "This appears to be an internal launcher or native driver issue. Please contact HyperLauncher support with your crash log."
            4. Keep your response friendly, well-formatted, and concise.
        """.trimIndent()

        val models = listOf("gemini-3.7-flash", "gemini-3.5-flash-lite", "gemini-3-flash-preview")
        var lastException: Exception? = null

        for (model in models) {
            try {
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 20000
                conn.readTimeout = 30000
                conn.doOutput = true
                conn.doInput = true

                val payload = JSONObject().apply {
                    val contents = JSONArray().apply {
                        val contentObj = JSONObject().apply {
                            val parts = JSONArray().apply {
                                val partObj = JSONObject().apply {
                                    put("text", prompt)
                                }
                                put(partObj)
                            }
                            put("parts", parts)
                        }
                        put(contentObj)
                    }
                    put("contents", contents)
                }

                conn.outputStream.use { os ->
                    os.write(payload.toString().toByteArray(Charsets.UTF_8))
                }

                val responseCode = conn.responseCode
                if (responseCode == 200) {
                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonResponse = JSONObject(responseText)
                    val candidates = jsonResponse.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val candidate = candidates.getJSONObject(0)
                        val content = candidate.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val answer = parts.getJSONObject(0).optString("text")
                            if (answer.isNotBlank()) {
                                return@withContext Result.success(answer)
                            }
                        }
                    }
                } else {
                    val errorText = try {
                        conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    } catch (_: Exception) {
                        ""
                    }
                    Log.w(TAG, "Model $model returned $responseCode: $errorText")
                    if (responseCode == 400 || responseCode == 401 || responseCode == 403) {
                        if (errorText.contains("API_KEY_INVALID") || errorText.contains("API key not valid") || responseCode == 401) {
                            return@withContext Result.failure(IllegalStateException("API_KEY_INVALID"))
                        }
                    }
                }
            } catch (ex: Exception) {
                Log.w(TAG, "Failed calling Gemini model $model", ex)
                lastException = ex
            }
        }

        Result.failure(lastException ?: IOException("Failed to communicate with Gemini API. Please check your API key and network connection."))
    }
}
