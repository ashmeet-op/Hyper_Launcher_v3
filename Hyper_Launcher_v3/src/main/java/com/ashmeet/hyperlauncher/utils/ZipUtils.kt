package com.ashmeet.hyperlauncher.utils

import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

object ZipUtils {

    @JvmStatic
    @Throws(IOException::class)
    fun getEntryStream(zipFile: ZipFile, entryPath: String): InputStream {
        val entry = zipFile.getEntry(entryPath) ?: throw IOException("No entry in ZIP file: $entryPath")
        return zipFile.getInputStream(entry)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun zipExtract(zipFile: ZipFile, dirName: String, destination: File) {
        val zipEntries = zipFile.entries()
        val dirNameLen = dirName.length
        while (zipEntries.hasMoreElements()) {
            val zipEntry = zipEntries.nextElement()
            val entryName = zipEntry.name
            if (!entryName.startsWith(dirName) || zipEntry.isDirectory) continue
            val zipDestination = File(destination, entryName.substring(dirNameLen))
            FileUtils.ensureParentDirectory(zipDestination)
            zipFile.getInputStream(zipEntry).use { inputStream ->
                FileOutputStream(zipDestination).use { outputStream ->
                    IOUtils.copy(inputStream, outputStream)
                }
            }
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun zipStreamExtract(zipInputStream: ZipInputStream, destination: File) {
        var zipEntry = zipInputStream.nextEntry
        while (zipEntry != null) {
            val entryName = zipEntry.name
            if (!zipEntry.isDirectory) {
                val zipDestination = File(destination, entryName)
                FileUtils.ensureParentDirectory(zipDestination)
                FileOutputStream(zipDestination).use { outputStream ->
                    IOUtils.copy(zipInputStream, outputStream)
                }
            }
            zipEntry = zipInputStream.nextEntry
        }
    }
}
