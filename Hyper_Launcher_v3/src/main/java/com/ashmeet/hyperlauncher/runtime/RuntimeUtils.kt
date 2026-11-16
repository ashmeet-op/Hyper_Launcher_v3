package com.ashmeet.hyperlauncher.runtime

import android.system.Os
import android.util.Log
import com.ashmeet.hyperbackend.utils.MathUtils
import com.ashmeet.hyperlauncher.utils.jre.JavaRunner
import com.ashmeet.hyperlauncher.tracker.TrackerLayout
import com.ashmeet.hyperlauncher.utils.HyperUtils
import com.ashmeet.hyperlauncher.utils.HyperUtils.NATIVE_LIB_DIR
import net.ashmeet.hyperlauncher.R
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FileUtils.listFiles
import org.apache.commons.io.FileUtils.write
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.HashMap
import java.util.Objects

object RuntimeUtils {
    private val sCache = HashMap<String, Runtime>()

    private val RUNTIME_FOLDER = File(HyperUtils.MULTIRT_HOME ?: "")
    private const val JAVA_VERSION_STR = "JAVA_VERSION=\""
    private const val OS_ARCH_STR = "OS_ARCH=\""

    @JvmStatic
    fun getRuntimes(): List<Runtime> {
        if (!RUNTIME_FOLDER.exists() && !RUNTIME_FOLDER.mkdirs()) {
            throw RuntimeException("Failed to create runtime directory")
        }

        val runtimes = ArrayList<Runtime>()
        val files = RUNTIME_FOLDER.listFiles()
        if (files != null) {
            for (f in files) {
                runtimes.add(read(f.name))
            }
        } else {
            throw RuntimeException("The runtime directory does not exist")
        }

        return runtimes
    }

    @JvmStatic
    fun getExactJreName(majorVersion: Int): String? {
        val runtimes = getRuntimes()
        for (r in runtimes) {
            if (r.javaVersion == majorVersion) return r.name
        }
        return null
    }

    @JvmStatic
    fun getNearestJreName(majorVersion: Int): String? {
        val runtimes = getRuntimes()
        val nearestRankedRuntime = MathUtils.findNearestPositive(majorVersion, runtimes) { runtime -> runtime.javaVersion }
            ?: return null
        val nearestRuntime = nearestRankedRuntime.value ?: return null
        return nearestRuntime.name
    }

    @JvmStatic
    @Throws(IOException::class)
    fun installRuntimeNamed(nativeLibDir: String, runtimeInputStream: InputStream, name: String) {
        val dest = File(RUNTIME_FOLDER, "/$name")
        if (dest.exists()) FileUtils.deleteDirectory(dest)
        try {
            uncompressTarXZ(runtimeInputStream, dest)
            runtimeInputStream.close()
            unpack200(nativeLibDir, "$RUNTIME_FOLDER/$name")
            read(name)
        } finally {
            TrackerLayout.clearTracker(TrackerLayout.UNPACK_RUNTIME)
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun postPrepare(name: String) {
        val dest = File(RUNTIME_FOLDER, "/$name")
        if (!dest.exists()) return

        val runtime = read(name)
        val vmPath = JavaRunner.findVmPath(dest, runtime.arch)
            ?: throw IOException("Could not find libjvm.so after extraction")
        val libDir = Objects.requireNonNull(vmPath.parentFile)?.parentFile

        val ftIn = File(libDir, "libfreetype.so.6")
        val ftOut = File(libDir, "libfreetype.so")
        if (ftIn.exists() && (!ftOut.exists() || (ftIn.length() != ftOut.length()))) {
            if (!ftIn.renameTo(ftOut)) throw IOException("Failed to rename freetype")
        }

        copyDummyNativeLib("libawt_xawt.so", libDir!!)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun installRuntimeNamedBinpack(
        universalFileInputStream: InputStream,
        platformBinsInputStream: InputStream,
        name: String,
        binpackVersion: String,
    ) {
        val dest = File(RUNTIME_FOLDER, "/$name")
        if (dest.exists()) FileUtils.deleteDirectory(dest)
        try {
            installRuntimeNamedNoRemove(universalFileInputStream, dest)
            installRuntimeNamedNoRemove(platformBinsInputStream, dest)

            unpack200(NATIVE_LIB_DIR ?: "", "$RUNTIME_FOLDER/$name")

            val binpackVerfile = File(RUNTIME_FOLDER, "/$name/pojav_version")
            write(binpackVerfile, binpackVersion, StandardCharsets.UTF_8)
            forceReread(name)
        } finally {
            TrackerLayout.clearTracker(TrackerLayout.UNPACK_RUNTIME)
        }
    }

    @JvmStatic
    fun readInternalRuntimeVersion(name: String): String? {
        val versionFile = File(RUNTIME_FOLDER, "/$name/pojav_version")
        return try {
            if (versionFile.exists()) {
                HyperUtils.read(versionFile.absolutePath)
            } else {
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    @JvmStatic
    fun readLastUpdateTime(name: String): Long {
        val lastUpdateTimeFile = File(RUNTIME_FOLDER, "$name/last_check_time")
        if (!lastUpdateTimeFile.exists()) return -1
        return try {
            HyperUtils.read(lastUpdateTimeFile).trim().toLong()
        } catch (_: IOException) {
            -1
        } catch (_: NumberFormatException) {
            -1
        }
    }

    @JvmStatic
    fun writeLastUpdateTime(name: String, time: Long) {
        val lastUpdateTimeFile = File(RUNTIME_FOLDER, "$name/last_check_time")
        try {
            HyperUtils.write(lastUpdateTimeFile, time.toString())
        } catch (_: IOException) {
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun removeRuntimeNamed(name: String) {
        val dest = File(RUNTIME_FOLDER, "/$name")
        if (dest.exists()) {
            FileUtils.deleteDirectory(dest)
            sCache.remove(name)
        }
    }

    @JvmStatic
    fun getRuntimeHome(name: String): File {
        val dest = File(RUNTIME_FOLDER, name)
        Log.i("MiltiRTUitls", "Dest exists? " + dest.exists())
        if (!dest.exists() || (forceReread(name).versionString == null)) {
            throw RuntimeException("Selected runtime is broken!")
        }
        return dest
    }

    @JvmStatic
    fun forceReread(name: String): Runtime {
        sCache.remove(name)
        return read(name)
    }

    @JvmStatic
    fun read(name: String): Runtime {
        sCache[name]?.let { return it }
        val release = File(RUNTIME_FOLDER, "$name/release")
        if (!release.exists()) {
            return Runtime(name)
        }
        val returnRuntime = try {
            val content = HyperUtils.read(release.absolutePath)
            val javaVersion = HyperUtils.extractUntilCharacter(content, JAVA_VERSION_STR, '"')
            val osArch = HyperUtils.extractUntilCharacter(content, OS_ARCH_STR, '"')
            if (javaVersion != null && osArch != null) {
                val javaVersionSplit = javaVersion.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val javaVersionInt = if (javaVersionSplit[0] == "1") {
                    javaVersionSplit[1].toInt()
                } else {
                    javaVersionSplit[0].toInt()
                }
                Runtime(name, javaVersion, osArch, javaVersionInt)
            } else {
                Runtime(name)
            }
        } catch (_: IOException) {
            Runtime(name)
        }
        sCache[name] = returnRuntime
        return returnRuntime
    }

    private fun unpack200(nativeLibraryDir: String, runtimePath: String) {
        val basePath = File(runtimePath)
        val files = listFiles(basePath, arrayOf("pack"), true)

        val workdir = File(nativeLibraryDir)
        val processBuilder = ProcessBuilder().directory(workdir)
        for (jarFile in files) {
            try {
                val process = processBuilder.command(
                    "./libunpack200.so",
                    "-r",
                    jarFile.absolutePath,
                    jarFile.absolutePath.replace(".pack", ""),
                ).start()
                process.waitFor()
            } catch (_: InterruptedException) {
                Log.e("MULTIRT", "Failed to unpack the runtime !")
            } catch (_: IOException) {
                Log.e("MULTIRT", "Failed to unpack the runtime !")
            }
        }
    }

    @Throws(IOException::class)
    @Suppress("SameParameterValue")
    private fun copyDummyNativeLib(name: String, dest: File) {
        val fileLib = File(dest, name)
        val isStream = FileInputStream(File(NATIVE_LIB_DIR, name))
        val os = FileOutputStream(fileLib)
        IOUtils.copy(isStream, os)
        isStream.close()
        os.close()
    }

    @Throws(IOException::class)
    private fun installRuntimeNamedNoRemove(runtimeInputStream: InputStream, dest: File) {
        uncompressTarXZ(runtimeInputStream, dest)
        runtimeInputStream.close()
    }

    @Throws(IOException::class)
    private fun uncompressTarXZ(tarFileInputStream: InputStream, dest: File) {
        com.ashmeet.hyperbackend.utils.FileUtils.ensureDirectory(dest)

        val buffer = ByteArray(8192)
        TarArchiveInputStream(XZCompressorInputStream(tarFileInputStream)).use { tarIn ->
            var tarEntry: TarArchiveEntry?
            while (tarIn.nextTarEntry.also { tarEntry = it } != null) {
                val entry = tarEntry!!
                val tarEntryName = entry.name
                TrackerLayout.setTracker(TrackerLayout.UNPACK_RUNTIME, 100, R.string.global_unpacking, tarEntryName)

                val destPath = File(dest, entry.name)
                com.ashmeet.hyperbackend.utils.FileUtils.ensureParentDirectory(destPath)
                if (entry.isSymbolicLink) {
                    try {
                        Os.symlink(entry.name, entry.linkName)
                    } catch (e: Throwable) {
                        Log.e("MultiRT", e.toString())
                    }
                } else if (entry.isDirectory) {
                    com.ashmeet.hyperbackend.utils.FileUtils.ensureDirectory(destPath)
                } else if (!destPath.exists() || destPath.length() != entry.size) {
                    FileOutputStream(destPath).use { os ->
                        IOUtils.copyLarge(tarIn, os, buffer)
                    }
                }
            }
        }
    }
}
