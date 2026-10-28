package com.ashmeet.hyperlauncher.utils.jre

import android.content.Context
import android.os.Build
import android.system.ErrnoException
import android.system.Os
import android.util.Log
import com.ashmeet.hyperbackend.awt.AWTView
import com.ashmeet.hyperbackend.utils.JREUtils
import com.ashmeet.hyperlauncher.runtime.Runtime
import com.ashmeet.hyperlauncher.runtime.RuntimeUtils
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.utils.HyperUtils
import java.io.File
import java.util.TimeZone

object JavaRunner {

    private fun getCacioJavaArgs(javaArgList: MutableList<String>, isJava8: Boolean): Boolean {
        javaArgList.add("-Djava.awt.headless=false")
        javaArgList.add("-Dpojav.path.native=" + HyperUtils.NATIVE_LIB_DIR)
        javaArgList.add("-Dpojav.nativedir=" + HyperUtils.NATIVE_LIB_DIR)
        javaArgList.add("-DPOJAV_NATIVEDIR=" + HyperUtils.NATIVE_LIB_DIR)
        javaArgList.add("-DPOJAV_PATH_NATIVE=" + HyperUtils.NATIVE_LIB_DIR)
        javaArgList.add("-Dpojav.native.path=" + HyperUtils.NATIVE_LIB_DIR)
        javaArgList.add("-Dcacio.native.path=" + HyperUtils.NATIVE_LIB_DIR)
        javaArgList.add("-Dcacio.managed.screensize=" + AWTView.AWT_CANVAS_WIDTH + "x" + AWTView.AWT_CANVAS_HEIGHT)
        javaArgList.add("-Dcacio.font.fontmanager=sun.awt.X11FontManager")
        javaArgList.add("-Dcacio.font.fontscaler=sun.font.FreetypeFontScaler")
        javaArgList.add("-Dswing.defaultlaf=javax.swing.plaf.metal.MetalLookAndFeel")
        if (isJava8) {
            javaArgList.add("-Dawt.toolkit=net.java.openjdk.cacio.ctc.CTCToolkit")
            javaArgList.add("-Djava.awt.graphicsenv=net.java.openjdk.cacio.ctc.CTCGraphicsEnvironment")
            val cacioClasspath = createCacioClasspath()
            javaArgList.add(cacioClasspath.toString())
            return false
        } else {
            val caciocavallo17AgentDir = File(HyperUtils.DIR_GAME_HOME, "caciocavallo17")
            val cacioJars = caciocavallo17AgentDir.listFiles { _, s -> s.endsWith(".jar") }
            if (cacioJars.isNullOrEmpty()) {
                return false
            }
            javaArgList.add("-javaagent:" + cacioJars[0].absolutePath)
            javaArgList.add("-Dawt.toolkit=com.github.caciocavallosilano.cacio.ctc.CTCToolkit")
            javaArgList.add("-Djava.awt.graphicsenv=com.github.caciocavallosilano.cacio.ctc.CTCGraphicsEnvironment")
            javaArgList.add("--add-exports=java.desktop/java.awt=ALL-UNNAMED")
            javaArgList.add("--add-exports=java.desktop/java.awt.peer=ALL-UNNAMED")
            javaArgList.add("--add-exports=java.desktop/sun.awt.image=ALL-UNNAMED")
            javaArgList.add("--add-exports=java.desktop/sun.java2d=ALL-UNNAMED")
            javaArgList.add("--add-exports=java.desktop/java.awt.dnd.peer=ALL-UNNAMED")
            javaArgList.add("--add-exports=java.desktop/sun.awt=ALL-UNNAMED")
            javaArgList.add("--add-exports=java.desktop/sun.awt.event=ALL-UNNAMED")
            javaArgList.add("--add-exports=java.desktop/sun.awt.datatransfer=ALL-UNNAMED")
            javaArgList.add("--add-exports=java.desktop/sun.font=ALL-UNNAMED")
            javaArgList.add("--add-exports=java.base/sun.security.action=ALL-UNNAMED")
            javaArgList.add("--add-opens=java.base/java.util=ALL-UNNAMED")
            javaArgList.add("--add-opens=java.desktop/java.awt=ALL-UNNAMED")
            javaArgList.add("--add-opens=java.desktop/sun.font=ALL-UNNAMED")
            javaArgList.add("--add-opens=java.desktop/sun.java2d=ALL-UNNAMED")
            javaArgList.add("--add-opens=java.base/java.lang.reflect=ALL-UNNAMED")
            return true
        }
    }

    private fun createCacioClasspath(): StringBuilder {
        val cacioClasspath = StringBuilder()
        cacioClasspath.append("-Xbootclasspath/p")
        val cacioDir = File(HyperUtils.DIR_GAME_HOME, "caciocavallo")
        val cacioFiles = cacioDir.listFiles()
        if (cacioFiles != null) {
            for (file in cacioFiles) {
                if (file.name.endsWith(".jar")) {
                    cacioClasspath.append(":").append(file.absolutePath)
                }
            }
        }
        return cacioClasspath
    }

    private fun getJavaArgs(runtimeHome: String, userArguments: MutableList<String>): MutableList<String> {
        val resolvFile = File(HyperUtils.DIR_DATA, "resolv.conf").absolutePath

        userArguments.add(0, "-Xms" + LauncherPreferences.PREF_RAM_ALLOCATION + "M")
        userArguments.add(0, "-Xmx" + LauncherPreferences.PREF_RAM_ALLOCATION + "M")

        val overridableArguments = arrayListOf(
            "-Djava.home=$runtimeHome",
            "-Djava.io.tmpdir=" + HyperUtils.DIR_CACHE!!.absolutePath,
            "-Djava.library.path=" + HyperUtils.NATIVE_LIB_DIR,
            "-Djna.boot.library.path=" + HyperUtils.NATIVE_LIB_DIR,
            "-Duser.home=" + HyperUtils.DIR_GAME_HOME,
            "-Duser.language=en",
            "-Duser.country=US",
            "-Dos.name=Linux",
            "-Dos.version=Android-" + Build.VERSION.RELEASE,
            "-Dpojav.path.minecraft=" + HyperUtils.DIR_GAME_NEW,
            "-Dpojav.path.private.account=" + HyperUtils.DIR_ACCOUNT_NEW,
            "-Duser.timezone=" + TimeZone.getDefault().id,
            "-Dorg.lwjgl.vulkan.libname=libvulkan.so",
            "-Dorg.lwjgl.spvc.libname=spirv-cross-c-shared",
            "-Dorg.lwjgl.sdl.libname=" + File(HyperUtils.NATIVE_LIB_DIR, "libSDL3.so").absolutePath,
            "-Dorg.lwjgl.system.allocator=system",
            "-Dext.net.resolvPath=$resolvFile",
            "-Dlog4j2.formatMsgNoLookups=true",
            "-Dfml.earlyprogresswindow=false",
            "-Dloader.disable_forked_guis=true",
            "-Djdk.lang.Process.launchMechanism=FORK",
        )
        val additionalArguments = ArrayList<String>()
        for (arg in overridableArguments) {
            val strippedArg = arg.substring(0, arg.indexOf('='))
            var add = true
            for (uarg in userArguments) {
                if (uarg.startsWith(strippedArg)) {
                    add = false
                    break
                }
            }
            if (add)
                additionalArguments.add(arg)
            else
                Log.i("ArgProcessor", "Arg skipped: $arg")
        }

        userArguments.addAll(additionalArguments)
        return userArguments
    }

    private fun getVmPath(runtimeHomeDir: File, arch: String?, flavor: String): File {
        return if (arch != null) File(runtimeHomeDir, "lib/$arch/$flavor/libjvm.so")
        else File(runtimeHomeDir, "lib/$flavor/libjvm.so")
    }

    private fun findVmForArch(runtimeHomeDir: File, arch: String?): File? {
        val serverPath = getVmPath(runtimeHomeDir, arch, "server")
        if (serverPath.exists()) return serverPath
        val clientPath = getVmPath(runtimeHomeDir, arch, "client")
        if (clientPath.exists()) return clientPath
        return null
    }

    @JvmStatic
    fun findVmPath(runtimeHomeDir: File, runtimeArch: String?): File? {
        findVmForArch(runtimeHomeDir, null)?.let { return it }
        if (runtimeArch != null) {
            val arches = when (runtimeArch) {
                "i386", "i486", "i586" -> listOf("i386", "i486", "i586")
                else -> listOf(runtimeArch)
            }
            for (arch in arches) {
                findVmForArch(runtimeHomeDir, arch)?.let { return it }
            }
        }
        return null
    }

    private fun relocateLdLibPath(vmPath: File) {
        val vmDir = requireNotNull(vmPath.parentFile)
        val libsDir = requireNotNull(vmDir.parentFile)
        val ldLibPath = libsDir.absolutePath + ":" +
                HyperUtils.NATIVE_LIB_DIR + ":" +
                vmDir.absolutePath + ":" +
                File(libsDir, "jli").absolutePath

        try {
            Os.setenv("LD_LIBRARY_PATH", ldLibPath, true)
        } catch (e: ErrnoException) {
            throw RuntimeException(e)
        }
        JREUtils.setLdLibraryPath(ldLibPath)
    }

    private fun setImmutableEnvVars(jreHome: File) {
        try {
            Os.setenv("JAVA_HOME", jreHome.absolutePath, true)
            Os.setenv("HOME", HyperUtils.DIR_GAME_HOME, true)
            Os.setenv("TMPDIR", HyperUtils.DIR_CACHE?.absolutePath ?: "", true)
            Os.setenv("POJAV_NATIVEDIR", HyperUtils.NATIVE_LIB_DIR, true)
        } catch (e: ErrnoException) {
            throw RuntimeException(e)
        }
    }

    private fun preprocessUserArgs(args: MutableList<String>): Boolean {
        val iterator = args.listIterator()
        var hasJavaAgent = false
        while (iterator.hasNext()) {
            var arg = iterator.next()
            if (arg == "-p") {
                arg = "--module-path"
            }
            when (arg) {
                "--add-reads",
                "--add-exports",
                "--add-opens",
                "--add-modules",
                "--limit-modules",
                "--module-path",
                "--patch-module",
                "--upgrade-module-path" -> {
                    iterator.remove()
                    val argValue = iterator.next()
                    iterator.remove()
                    iterator.add("$arg=$argValue")
                }
                "-d32",
                "-d64",
                "-Xint",
                "-XX:+UseTransparentHugePages",
                "-XX:+UseLargePagesInMetaspace",
                "-XX:+UseLargePages" -> {
                    iterator.remove()
                }
                else -> {
                    if (arg.startsWith("-Xms") || arg.startsWith("-Xmx") || arg.startsWith("-XX:ActiveProcessorCount")) {
                        iterator.remove()
                    }
                    if (!hasJavaAgent && arg.startsWith("-javaagent:")) {
                        hasJavaAgent = true
                    }
                }
            }
        }
        return hasJavaAgent
    }

    @JvmStatic
    @Throws(VMLoadException::class)
    fun startJvm(
        runtime: Runtime,
        vmArgs: MutableList<String>,
        classpathEntries: List<String>,
        mainClass: String?,
        applicationArgs: List<String>
    ) {
        val runtimeHomeDir = RuntimeUtils.getRuntimeHome(runtime.name)
        val vmPath = findVmPath(runtimeHomeDir, runtime.arch)
            ?: throw VMLoadException("Unable to find the Java VM", 0, -1)

        var hasJavaAgent = preprocessUserArgs(vmArgs)
        val runtimeArgs = ArrayList<String>()
        if (getCacioJavaArgs(runtimeArgs, runtime.javaVersion == 8)) hasJavaAgent = true
        runtimeArgs.addAll(getJavaArgs(runtimeHomeDir.absolutePath, vmArgs))

        Log.i("JavaRunner", "Native lib dir: " + HyperUtils.NATIVE_LIB_DIR)
        Log.i("JavaRunner", "Final JVM args: $runtimeArgs")

        runtimeArgs.add("-XX:ActiveProcessorCount=" + java.lang.Runtime.getRuntime().availableProcessors())
        val classpathBuilder = StringBuilder().append("-Djava.class.path=")
        var first = true
        for (entry in classpathEntries) {
            if (first) first = false
            else classpathBuilder.append(':')
            classpathBuilder.append(entry)
        }
        runtimeArgs.add(classpathBuilder.toString())

        setImmutableEnvVars(runtimeHomeDir)
        relocateLdLibPath(vmPath)

        nativeLoadJVM(
            vmPath.absolutePath,
            runtimeArgs.toTypedArray(),
            mainClass,
            applicationArgs.toTypedArray(),
            hasJavaAgent
        )
    }

    @JvmStatic
    @Throws(VMLoadException::class)
    external fun nativeLoadJVM(
        vmPath: String,
        javaArgs: Array<String>,
        mainClass: String?,
        appArgs: Array<String>,
        hasJavaAgents: Boolean
    ): Boolean

    @JvmStatic
    external fun nativeSetupExit(context: Context)
}
