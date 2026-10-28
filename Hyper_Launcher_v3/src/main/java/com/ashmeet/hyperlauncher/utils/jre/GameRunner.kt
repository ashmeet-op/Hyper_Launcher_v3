package com.ashmeet.hyperlauncher.utils.jre

import android.util.ArrayMap
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit
import androidx.appcompat.app.AppCompatActivity
import com.ashmeet.hyperbackend.JVersionList
import com.ashmeet.hyperbackend.authenticator.AuthType
import com.ashmeet.hyperbackend.authenticator.accounts.Account
import com.ashmeet.hyperbackend.authenticator.impl.MicrosoftBackgroundLogin
import com.ashmeet.hyperbackend.game.renderer.GameRenderer
import com.ashmeet.hyperbackend.game.renderer.impl.GLESRenderSpec
import com.ashmeet.hyperbackend.instances.Instance
import com.ashmeet.hyperbackend.lifecycle.LifecycleAwareAlertDialog
import com.ashmeet.hyperbackend.modloaders.Lwjgl3ifyUtils
import com.ashmeet.hyperbackend.utils.FileUtils
import com.ashmeet.hyperbackend.utils.GameOptionsUtils
import com.ashmeet.hyperbackend.utils.GpuUtils
import com.ashmeet.hyperbackend.utils.JREUtils
import com.ashmeet.hyperbackend.utils.JSONUtils
import com.ashmeet.hyperbackend.utils.MCOptionUtils
import com.ashmeet.hyperbackend.utils.OldVersionsUtils
import com.ashmeet.hyperlauncher.extra.ExtraConstants
import com.ashmeet.hyperlauncher.extra.ExtraCore
import com.ashmeet.hyperlauncher.runtime.RuntimeUtils
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences.PREF_ZINK_PREFER_SYSTEM_DRIVER
import com.ashmeet.hyperlauncher.skin.SkinManager
import com.ashmeet.hyperlauncher.utils.Architecture
import com.ashmeet.hyperlauncher.utils.DateUtils
import com.ashmeet.hyperlauncher.utils.DiscordRpcManager
import com.ashmeet.hyperlauncher.utils.HyperUtils
import net.ashmeet.hyperlauncher.R
import java.io.File
import java.io.IOException
import java.util.Collections
import java.util.Objects
import kotlin.system.exitProcess

object GameRunner {

    private fun hasSodium(gameDir: File): Boolean {
        val modsDir = File(gameDir, "mods")
        val mods = modsDir.listFiles { file -> file.isFile && file.name.endsWith(".jar") } ?: return false
        for (file in mods) {
            val name = file.name
            if (name.contains("sodium") ||
                name.contains("embeddium") ||
                name.contains("rubidium")
            ) return true
        }
        return false
    }

    private fun hasAngelica(gameDir: File): Boolean {
        val modsDir = File(gameDir, "mods")
        val mods = modsDir.listFiles { file -> file.isFile && file.name.endsWith(".jar") } ?: return false
        for (file in mods) {
            val name = file.name
            if (name.contains("angelica")) return true
        }
        return false
    }

    private fun affectedByRenderDistanceIssue(version: JVersionList.Version): Boolean {
        if (LauncherPreferences.PREF_USE_ANGLE) return false
        val info = GpuUtils.getGlInfo()
        return info.isAdreno &&
                info.glesMajorVersion >= 3 &&
                DateUtils.dateBefore(DateUtils.getOriginalReleaseDate(version), 2025, 2, 25)
    }

    private fun checkRenderDistance(version: JVersionList.Version, gamedir: File): Boolean {
        if (!affectedByRenderDistanceIssue(version)) return false
        if (hasSodium(gamedir)) return false
        try {
            MCOptionUtils.load()
        } catch (_: Exception) {
        }
        val renderDistance = GameOptionsUtils.parseIntDefault(MCOptionUtils.get("renderDistance"), 12)
        return renderDistance > 7
    }

    private fun isGl4esCompatible(version: JVersionList.Version): Boolean {
        return DateUtils.dateBefore(DateUtils.getOriginalReleaseDate(version), 2025, 1, 7)
    }

    private fun isCompatContext(version: JVersionList.Version): Boolean {
        return DateUtils.dateBefore(DateUtils.getOriginalReleaseDate(version), 2021, 3, 9)
    }

    @Throws(InterruptedException::class)
    private fun showDialog(activity: AppCompatActivity, message: Int): Boolean {
        val dialogCreator = LifecycleAwareAlertDialog.DialogCreator { _, dialogBuilder ->
            dialogBuilder.setMessage(activity.getString(message))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) { _, _ -> }
        }
        return LifecycleAwareAlertDialog.haltOnDialog(activity.lifecycle, activity, dialogCreator)
    }

    @Throws(InterruptedException::class, IOException::class)
    private fun switchRendererifSupported(
        support: Boolean,
        renderer: String,
        gameRenderer: GameRenderer,
        instance: Instance,
        activity: AppCompatActivity,
        resId: Int
    ) {
        if (support) {
            instance.renderer = renderer
            instance.write()
            gameRenderer.setCurrentRenderer(renderer)
        } else {
            showDialog(activity, resId)
            exitProcess(0)
        }
    }

    @JvmStatic
    @Throws(Throwable::class)
    fun launchGame(
        activity: AppCompatActivity,
        accountParam: Account?,
        instance: Instance,
        versionId: String,
        classpath: Array<File?>,
        gameRenderer: GameRenderer
    ) {
        var account = accountParam
        if (account == null) {
            account = Account().apply {
                username = "Steve"
                accessToken = "0"
                profileId = "00000000-0000-0000-0000-000000000000"
            }
        }
        if (account.authType == AuthType.MICROSOFT) {
            try {
                if (System.currentTimeMillis() > account.expiresAt) {
                    account = MicrosoftBackgroundLogin.performRefresh(account)
                }
            } catch (_: Exception) {
            }
        }
        var freeDeviceMemory = HyperUtils.getFreeDeviceMemory(activity)
        val localeString: Int
        val freeAddressSpace = if (Architecture.is32BitsDevice()) HyperUtils.getMaxContinuousAddressSpaceSize() else -1
        Log.i("MemStat", "Free RAM: $freeDeviceMemory Addressable: $freeAddressSpace")
        val showAddressMemoryWarning = freeDeviceMemory > freeAddressSpace && freeAddressSpace != -1
        if (showAddressMemoryWarning) {
            freeDeviceMemory = freeAddressSpace
            localeString = R.string.address_memory_warning_msg
        } else {
            localeString = R.string.memory_warning_msg
        }

        if (LauncherPreferences.PREF_RAM_ALLOCATION > freeDeviceMemory && (showAddressMemoryWarning || LauncherPreferences.PREF_SHOW_MEMORY_WARNING_DIALOG)) {
            val finalDeviceMemory = freeDeviceMemory
            val dialogCreator = LifecycleAwareAlertDialog.DialogCreator { _, builder ->
                builder.setMessage(activity.getString(localeString, finalDeviceMemory, LauncherPreferences.PREF_RAM_ALLOCATION))
                    .setPositiveButton(android.R.string.ok) { _, _ -> }

                if (!showAddressMemoryWarning) {
                    builder.setNegativeButton(R.string.option_do_not_show_again) { _, _ ->
                        LauncherPreferences.DEFAULT_PREF?.edit {
                            putBoolean("showMemoryWarning", false)
                        }
                        Toast.makeText(activity, R.string.notification_permission_toast, Toast.LENGTH_SHORT).show()
                    }
                }
            }

            if (LifecycleAwareAlertDialog.haltOnDialog(activity.lifecycle, activity, dialogCreator)) {
                return
            }
        }
        val gamedir = instance.getGameDirectory()
        val versionInfo = HyperUtils.getVersionInfo(versionId)
        requireNotNull(versionInfo)
        versionInfo.libraries = null
        versionInfo.downloads = null

        val renderer = gameRenderer.currentRenderer

        if (isCompatContext(versionInfo) && !hasAngelica(gamedir) && renderer is GLESRenderSpec.LTWRenderSpec) {
            switchRendererifSupported(true, GameRenderer.GL4ES_RENDERER, gameRenderer, instance, activity, 0)
        }

        val isGl4es = renderer is GLESRenderSpec.GL4ESRenderSpec
        val ltwSupported = Objects.requireNonNull(GameRenderer.getKnownRenderer(GameRenderer.LTW_RENDERER)).compatibleDevice(activity)
        if (!isCompatContext(versionInfo) && isGl4es && hasSodium(gamedir)) {
            switchRendererifSupported(ltwSupported, GameRenderer.LTW_RENDERER, gameRenderer, instance, activity, R.string.compat_sodium_not_supported)
        }

        if (!isGl4esCompatible(versionInfo) && isGl4es) {
            switchRendererifSupported(ltwSupported, GameRenderer.LTW_RENDERER, gameRenderer, instance, activity, R.string.compat_sodium_not_supported)
        }

        DiscordRpcManager.startRpc(
            "Playing Minecraft ($versionId)",
            if (instance.mInstanceRoot != null) instance.mInstanceRoot.name else "HyperLauncher"
        )

        GameRenderer.releaseCache()

        val isLtw = renderer is GLESRenderSpec.LTWRenderSpec

        if (isLtw && checkRenderDistance(versionInfo, gamedir)) {
            if (showDialog(activity, R.string.ltw_render_distance_warning_msg)) return
            try {
                MCOptionUtils.set("renderDistance", "7")
                MCOptionUtils.save()
            } catch (_: Exception) {
            }
        }

        GameOptionsUtils.fixOptions(isLtw)

        if (isLtw && GpuUtils.getGlInfo().forcedMsaa) {
            if (showDialog(activity, R.string.ltw_4x_msaa_warning_msg)) return
        }

        var requiredJavaVersion = 8
        if (versionInfo.javaVersion != null) requiredJavaVersion = versionInfo.javaVersion.majorVersion

        val runtime = RuntimeUtils.forceReread(pickRuntime(instance, requiredJavaVersion))

        disableSplash(gamedir)
        val launchArgs = getMoJsonClientArgs(account, versionInfo, gamedir)

        OldVersionsUtils.selectOpenGlVersion(versionInfo)

        val launcherDir = File(HyperUtils.DIR_GAME_HOME, "launcher")
        val mioLaunchWrapper = File(launcherDir, "MioLaunchWrapper.jar")
        val mioLibPatcher = File(launcherDir, "MioLibPatcher.jar")

        val javaArgList = ArrayList<String>()

        if (mioLibPatcher.exists()) {
            javaArgList.add("-javaagent:" + mioLibPatcher.absolutePath)
        }

        val launchClassPath = ArrayList<String>(classpath.size + 1)
        if (mioLaunchWrapper.exists()) {
            launchClassPath.add(mioLaunchWrapper.absolutePath)
        }

        for (i in classpath.indices) {
            val classpathEntry = classpath[i] ?: continue
            val entryPath = classpathEntry.absolutePath
            if (!classpathEntry.exists()) {
                Log.w("GameRunner", "Skipped classpath entry $entryPath because it is missing")
            }
            launchClassPath.add(entryPath)
            classpath[i] = null
        }

        launchClassPath.trimToSize()

        if (versionInfo.logging != null && versionInfo.logging.client != null && versionInfo.logging.client.file != null) {
            var configFile = HyperUtils.DIR_DATA + "/security/" + versionInfo.logging.client.file.id.replace("client", "log4j-rce-patch")
            if (!File(configFile).exists()) {
                configFile = HyperUtils.DIR_GAME_NEW + "/" + versionInfo.logging.client.file.id
            }
            javaArgList.add("-Dlog4j.configurationFile=$configFile")
        }

        versionInfo.logging = null

        val versionSpecificNativesDir = File(HyperUtils.DIR_CACHE, "natives/$versionId")
        if (versionSpecificNativesDir.exists()) {
            val dirPath = versionSpecificNativesDir.absolutePath
            javaArgList.add("-Djava.library.path=$dirPath:" + HyperUtils.NATIVE_LIB_DIR)
            javaArgList.add("-Djna.boot.library.path=$dirPath")
            javaArgList.add("-Dorg.lwjgl.librarypath=$dirPath")
        }

        val lwjglExtractDir = File(HyperUtils.DIR_CACHE, "lwjgl_native/$versionId")
        FileUtils.ensureDirectory(lwjglExtractDir)
        javaArgList.add("-Dorg.lwjgl.system.SharedLibraryExtractPath=" + lwjglExtractDir.absolutePath)

        addAuthlibInjectorArgs(javaArgList, account)
        addNoBlocklistArgs(javaArgList)

        mergeMoJsonArgs(javaArgList, getMoJsonJvmArgs(versionId))

        versionInfo.arguments = null
        versionInfo.minecraftArguments = null
        versionInfo.assets = null
        versionInfo.assetIndex = null

        javaArgList.addAll(JREUtils.parseJavaArguments(instance.getLaunchArgs()))

        JREUtils.setGameEnvironment(activity, gameRenderer)
        JREUtils.chdir(instance.getGameDirectory().absolutePath)

        if (GpuUtils.getGlInfo().isAdreno && !PREF_ZINK_PREFER_SYSTEM_DRIVER) {
            gameRenderer.overrideVulkanDriver()
        }
        if (!gameRenderer.maybeSetupRenderer()) {
            if (showDialog(activity, R.string.gr_err_renderer_load_Failed)) return
            exitProcess(0)
        }
        javaArgList.add("-Dorg.lwjgl.opengl.libname=libGLMojo.so")
        javaArgList.add("-Dorg.lwjgl.freetype.libname=" + HyperUtils.NATIVE_LIB_DIR + "/libfreetype.so")
        javaArgList.add("-DZstdNativePath=" + HyperUtils.NATIVE_LIB_DIR + "/libzstd-jni-1.5.7-6-dhcompat.so")
        HyperUtils.applyHyperPluginHooks(javaArgList, instance.getGameDirectory())

        activity.runOnUiThread {
            Toast.makeText(
                activity,
                activity.getString(R.string.autoram_info_msg, LauncherPreferences.PREF_RAM_ALLOCATION),
                Toast.LENGTH_SHORT
            ).show()
        }

        Log.i("GameRunner", "Running with $launchArgs")

        var hasLwjgl3ify = false
        try {
            hasLwjgl3ify = Lwjgl3ifyUtils.detectLwjgl3ifyJar(gamedir) != null
        } catch (_: Exception) {
        }
        if (!hasLwjgl3ify && versionInfo.mainClass != null && versionInfo.mainClass.contains("lwjgl3ify")) {
            hasLwjgl3ify = true
        }

        var mainClass = versionInfo.mainClass
        if (hasLwjgl3ify && mioLaunchWrapper.exists()) {
            mainClass = "mio.Wrapper"
            launchArgs.add(0, versionInfo.mainClass)
        }

        try {
            JavaRunner.nativeSetupExit(activity)
            JavaRunner.startJvm(runtime, javaArgList, launchClassPath, mainClass, launchArgs)
        } catch (e: VMLoadException) {
            val dialogCreator = LifecycleAwareAlertDialog.DialogCreator { _, builder ->
                builder.setMessage(e.toString(activity)).setPositiveButton(android.R.string.ok) { _, _ -> }
            }

            if (LifecycleAwareAlertDialog.haltOnDialog(activity.lifecycle, activity, dialogCreator)) {
                return
            }
        }

        HyperUtils.restartLauncherActivity(activity)
        HyperUtils.fullyExit()
    }

    private fun disableSplash(dir: File) {
        val configDir = File(dir, "config")
        if (FileUtils.ensureDirectorySilently(configDir)) {
            val forgeSplashFile = File(dir, "config/splash.properties")
            var forgeSplashContent = "enabled=true"
            try {
                if (forgeSplashFile.exists()) {
                    forgeSplashContent = HyperUtils.read(forgeSplashFile.absolutePath)
                }
                if (forgeSplashContent.contains("enabled=true")) {
                    HyperUtils.write(
                        forgeSplashFile,
                        forgeSplashContent.replace("enabled=true", "enabled=false")
                    )
                }
            } catch (e: IOException) {
                Log.w(HyperUtils.APP_NAME, "Could not disable Forge 1.12.2 and below splash screen!", e)
            }
        } else {
            Log.w(HyperUtils.APP_NAME, "Failed to create the configuration directory")
        }
    }

    private fun addAuthlibInjectorArgs(javaArgList: MutableList<String>, account: Account) {
        if (account.authType == AuthType.MICROSOFT || account.authType == AuthType.ELY_BY) {
            return
        }
        var injectorUrl = account.authType?.injectorUrl
        if (injectorUrl == null || (account.capePath != null || account.skinPath != null)) {
            val localUrl = SkinManager.instance.registerAndStartServer(account)
            if (localUrl != null) {
                injectorUrl = localUrl
            }
        }
        if (injectorUrl != null) {
            val authlibJar = File(HyperUtils.DIR_DATA, "authlib-injector/authlib-injector.jar")
            if (authlibJar.exists() && authlibJar.isFile && authlibJar.length() > 0) {
                javaArgList.add("-javaagent:" + authlibJar.absolutePath + "=" + injectorUrl)
            } else {
                Log.w("GameRunner", "authlib-injector jar not found at " + authlibJar.absolutePath)
            }
        }
    }

    private fun addNoBlocklistArgs(javaArgList: MutableList<String>) {
        if (!LauncherPreferences.PREF_DISABLE_BLOCKLIST) return
        val noBlocklistDir = File(HyperUtils.DIR_DATA, "noblocklist")
        var jarToUse: File? = null
        val jarFiles = noBlocklistDir.listFiles { _, name -> name.endsWith(".jar") }
        if (jarFiles != null) {
            for (file in jarFiles) {
                if (file.exists() && file.isFile && file.length() > 0) {
                    jarToUse = file
                    break
                }
            }
        }
        if (jarToUse == null) {
            val defaultFile = File(noBlocklistDir, "NoBlocklist-1.0-SNAPSHOT-all.jar")
            if (defaultFile.exists() && defaultFile.isFile && defaultFile.length() > 0) {
                jarToUse = defaultFile
            }
        }
        if (jarToUse != null) {
            Log.i("GameRunner", "Using NoBlocklist jar: " + jarToUse.absolutePath)
            javaArgList.add("-javaagent:" + jarToUse.absolutePath)
        } else {
            Log.w("GameRunner", "NoBlocklist is enabled, but no valid jar found in " + noBlocklistDir.absolutePath)
        }
    }

    private fun shouldSkipArg(arg: String): Boolean {
        val args = arrayOf(
            "-Djava.library.path=",
            "-Djna.tmpdir=",
            "-Dorg.lwjgl.system.SharedLibraryExtractPath=",
            "-Dio.netty.native.workdir="
        )
        for (s in args) {
            if (arg.startsWith(s)) return true
        }
        return false
    }

    private fun mergeMoJsonArgs(userArgs: MutableList<String>, moJsonArgs: List<String>) {
        for (arg in moJsonArgs) {
            if (shouldSkipArg(arg)) continue
            userArgs.add(arg)
        }
    }

    private fun getMoJsonJvmArgs(versionName: String): List<String> {
        val versionInfo = HyperUtils.getVersionInfo(versionName, true)
        if (versionInfo.arguments == null || versionInfo.arguments.jvm == null) {
            return Collections.emptyList()
        }

        val varArgMap = ArrayMap<String, String>()
        varArgMap["classpath_separator"] = ":"
        varArgMap["library_directory"] = HyperUtils.DIR_HOME_LIBRARY
        varArgMap["version_name"] = versionInfo.id
        varArgMap["natives_directory"] = HyperUtils.NATIVE_LIB_DIR

        val clientVmArgs = ArrayList<String>()
        if (versionInfo.arguments != null) {
            for (arg in versionInfo.arguments.jvm) {
                if (arg is String) {
                    clientVmArgs.add(arg)
                }
            }
        }
        return JSONUtils.insertJSONValueList(clientVmArgs, varArgMap)
    }

    private fun getMoJsonClientArgs(profile: Account, versionInfo: JVersionList.Version, gameDir: File): ArrayList<String> {
        val username = profile.username
        var versionName = versionInfo.id
        if (versionInfo.inheritsFrom != null) {
            versionName = versionInfo.inheritsFrom
        }

        var userType = "mojang"
        try {
            val creationDate = DateUtils.getOriginalReleaseDate(versionInfo)
            if (!DateUtils.dateBefore(creationDate, 2022, 9, 26)) {
                userType = "msa"
            }
        } catch (_: Exception) {
        }

        val varArgMap = ArrayMap<String, String>()
        varArgMap["auth_session"] = profile.accessToken
        varArgMap["auth_access_token"] = profile.accessToken
        varArgMap["auth_player_name"] = username
        varArgMap["auth_uuid"] = profile.profileId.replace("-", "")
        varArgMap["auth_xuid"] = profile.xuid
        varArgMap["assets_root"] = HyperUtils.ASSETS_PATH
        varArgMap["assets_index_name"] = versionInfo.assets
        varArgMap["game_assets"] = HyperUtils.ASSETS_PATH
        varArgMap["game_directory"] = gameDir.absolutePath
        varArgMap["user_properties"] = "{}"
        varArgMap["user_type"] = userType
        varArgMap["version_name"] = versionName
        varArgMap["version_type"] = versionInfo.type

        val clientArgs = ArrayList<String>()
        if (versionInfo.arguments != null && versionInfo.arguments.game != null) {
            for (arg in versionInfo.arguments.game) {
                if (arg is String) {
                    clientArgs.add(arg)
                }
            }
        }
        if (versionInfo.minecraftArguments != null) {
            clientArgs.addAll(splitAndFilterEmpty(versionInfo.minecraftArguments))
        }
        val quickPlayWorld = ExtraCore.consumeValue(ExtraConstants.QUICK_PLAY_WORLD) as String?
        if (quickPlayWorld != null && quickPlayWorld.trim().isNotEmpty()) {
            val worldName = quickPlayWorld.trim()
            varArgMap["quickPlaySingleplayer"] = worldName
            if (isQuickPlaySingleplayerSupported(versionInfo)) {
                if (!clientArgs.contains("--quickPlaySingleplayer")) {
                    clientArgs.add("--quickPlaySingleplayer")
                    clientArgs.add(worldName)
                }
            } else {
                Log.w("GameRunner", "Quick Play for singleplayer is not supported on this Minecraft version and has been skipped.")
            }
        }

        val quickPlayServer = ExtraCore.consumeValue(ExtraConstants.QUICK_PLAY_SERVER) as String?
        if (quickPlayServer != null && quickPlayServer.trim().isNotEmpty()) {
            val serverAddress = quickPlayServer.trim()
            varArgMap["quickPlayMultiplayer"] = serverAddress
            varArgMap["quickPlayServer"] = serverAddress
            if (isQuickPlayServerSupported(versionInfo)) {
                if (!clientArgs.contains("--quickPlayMultiplayer") && !clientArgs.contains("--quickPlayServer")) {
                    clientArgs.add("--quickPlayMultiplayer")
                    clientArgs.add(serverAddress)
                }
            } else {
                if (!clientArgs.contains("--server")) {
                    if (serverAddress.contains(":")) {
                        val parts = serverAddress.split(":".toRegex(), limit = 2).toTypedArray()
                        clientArgs.add("--server")
                        clientArgs.add(parts[0])
                        clientArgs.add("--port")
                        clientArgs.add(parts[1])
                    } else {
                        clientArgs.add("--server")
                        clientArgs.add(serverAddress)
                    }
                }
            }
        }
        return JSONUtils.insertJSONValueList(clientArgs, varArgMap) as ArrayList<String>
    }

    private fun isQuickPlaySingleplayerSupported(versionInfo: JVersionList.Version?): Boolean {
        if (versionInfo == null) return false
        if (versionInfo.arguments != null && versionInfo.arguments.game != null) {
            for (arg in versionInfo.arguments.game) {
                if (arg is String && (arg.contains("quickPlaySingleplayer") || arg.contains("quickPlay"))) {
                    return true
                }
                if (arg is JVersionList.Arguments.ArgValue) {
                    if (arg.value != null && (arg.value.contains("quickPlaySingleplayer") || arg.value.contains("quickPlay"))) {
                        return true
                    }
                    if (arg.values != null) {
                        for (v in arg.values) {
                            if (v != null && (v.contains("quickPlaySingleplayer") || v.contains("quickPlay"))) return true
                        }
                    }
                }
            }
        }
        return try {
            val creationDate = DateUtils.getOriginalReleaseDate(versionInfo)
            !DateUtils.dateBefore(creationDate, 2023, 3, 1)
        } catch (_: Exception) {
            false
        }
    }

    private fun isQuickPlayServerSupported(versionInfo: JVersionList.Version?): Boolean {
        if (versionInfo == null) return false
        if (versionInfo.arguments != null && versionInfo.arguments.game != null) {
            for (arg in versionInfo.arguments.game) {
                if (arg is String && (arg.contains("quickPlayMultiplayer") || arg.contains("quickPlayServer") || arg.contains("quickPlay"))) {
                    return true
                }
                if (arg is JVersionList.Arguments.ArgValue) {
                    if (arg.value != null && (arg.value.contains("quickPlayMultiplayer") || arg.value.contains("quickPlayServer") || arg.value.contains("quickPlay"))) {
                        return true
                    }
                    if (arg.values != null) {
                        for (v in arg.values) {
                            if (v != null && (v.contains("quickPlayMultiplayer") || v.contains("quickPlayServer") || v.contains("quickPlay"))) return true
                        }
                    }
                }
            }
        }
        return try {
            val creationDate = DateUtils.getOriginalReleaseDate(versionInfo)
            !DateUtils.dateBefore(creationDate, 2023, 3, 1)
        } catch (_: Exception) {
            false
        }
    }

    private fun splitAndFilterEmpty(argStr: String): List<String> {
        val strList = ArrayList<String>()
        for (arg in argStr.split(" ")) {
            if (arg.isNotEmpty()) {
                strList.add(arg)
            }
        }
        return strList
    }

    @JvmStatic
    fun pickRuntime(instance: Instance, targetJavaVersion: Int): String {
        var runtime = HyperUtils.getSelectedRuntime(instance)
        val profileRuntime = instance.selectedRuntime
        val pickedRuntime = if (runtime != null) RuntimeUtils.read(runtime) else null
        if (runtime == null || pickedRuntime == null || pickedRuntime.javaVersion == 0 || pickedRuntime.javaVersion < targetJavaVersion) {
            val preferredRuntime = RuntimeUtils.getNearestJreName(targetJavaVersion)
                ?: throw RuntimeException("Failed to autopick runtime!")
            if (profileRuntime != null) {
                instance.selectedRuntime = preferredRuntime
                instance.maybeWrite()
            }
            runtime = preferredRuntime
        }
        return runtime
    }
}
